package com.ssafy.stab.webrtc.openvidu;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.ssafy.stab.BuildConfig;
import com.ssafy.stab.webrtc.observers.CustomPeerConnectionObserver;
import com.ssafy.stab.webrtc.observers.CustomSdpObserver;
import com.ssafy.stab.webrtc.websocket.CustomWebSocket;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SessionDescription;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Session {

    private LocalParticipant localParticipant;
    private Map<String, RemoteParticipant> remoteParticipants = new HashMap<>();
    private String id;
    private String token;

    private final List<IceServer> iceServersDefault =
            Arrays.asList(IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                    IceServer.builder("turn:"+BuildConfig.OPENVIDU_TURN).createIceServer());

    private List<IceServer> iceServers = new ArrayList();
    private PeerConnectionFactory peerConnectionFactory;
    private CustomWebSocket websocket;
    private StreamObserver streamObserver;

    public Session(String id, String token, Context context) {
        this.id = id;
        this.token = token;

        // Creating a new PeerConnectionFactory instance
        PeerConnectionFactory.InitializationOptions.Builder optionsBuilder = PeerConnectionFactory.InitializationOptions.builder(context);
        optionsBuilder.setEnableInternalTracer(true);
        PeerConnectionFactory.InitializationOptions opt = optionsBuilder.createInitializationOptions();
        PeerConnectionFactory.initialize(opt);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

//        // Using software encoder and decoder
//        final VideoEncoderFactory encoderFactory;
//        final VideoDecoderFactory decoderFactory;
//        encoderFactory = new SoftwareVideoEncoderFactory();
//        decoderFactory = new SoftwareVideoDecoderFactory();

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .createPeerConnectionFactory();
    }

    public void setWebSocket(CustomWebSocket websocket) {
        this.websocket = websocket;
    }

    public PeerConnection createLocalPeerConnection() {
        PeerConnection.RTCConfiguration config =
                new PeerConnection.RTCConfiguration(iceServers.isEmpty()
                        ? iceServersDefault
                        : iceServers);
        config.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED;
        config.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        config.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.NEGOTIATE;
        config.continualGatheringPolicy =
                PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        config.keyType = PeerConnection.KeyType.ECDSA;
        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        PeerConnection peerConnection = peerConnectionFactory.createPeerConnection(config, new CustomPeerConnectionObserver("local") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                websocket.onIceCandidate(iceCandidate, localParticipant.getConnectionId());
            }

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                if (PeerConnection.SignalingState.STABLE.equals(signalingState)) {
                    // SDP Offer/Answer finished. Add stored remote candidates.
                    Iterator<IceCandidate> it = localParticipant.getIceCandidateList().iterator();
                    while (it.hasNext()) {
                        IceCandidate candidate = it.next();
                        localParticipant.getPeerConnection().addIceCandidate(candidate);
                        it.remove();
                    }
                }
            }
        });

        if (localParticipant.getAudioTrack() != null) {
            peerConnection.addTransceiver(localParticipant.getAudioTrack(),
                new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY));
        }
//        if (localParticipant.getVideoTrack() != null) {
//            peerConnection.addTransceiver(localParticipant.getVideoTrack(),
//                new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY));
//        }

        return peerConnection;
    }

    public interface StreamObserver {
        void onStreamReceived(MediaStream stream, RemoteParticipant participant);
    }

    public void setStreamObserver(StreamObserver observer) {
        this.streamObserver = observer;
    }

    public void createRemotePeerConnection(final String connectionId) {
        PeerConnection.RTCConfiguration config =
                new PeerConnection.RTCConfiguration(iceServers.isEmpty()
                        ? iceServersDefault
                        : iceServers);
        config.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED;
        config.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        config.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.NEGOTIATE;
        config.continualGatheringPolicy =
                PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        config.keyType = PeerConnection.KeyType.ECDSA;
        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        PeerConnection peerConnection = peerConnectionFactory.createPeerConnection(config, new CustomPeerConnectionObserver("remotePeerCreation") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                websocket.onIceCandidate(iceCandidate, connectionId);
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                super.onAddTrack(rtpReceiver, mediaStreams);
                if (streamObserver != null && mediaStreams.length > 0) {
                    streamObserver.onStreamReceived(mediaStreams[0], remoteParticipants.get(connectionId));
                }
            }

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                if (PeerConnection.SignalingState.STABLE.equals(signalingState)) {
                    // SDP Offer/Answer finished. Add stored remote candidates.
                    final RemoteParticipant remoteParticipant = remoteParticipants.get(connectionId);
                    Iterator<IceCandidate> it = remoteParticipant.getIceCandidateList().iterator();
                    while (it.hasNext()) {
                        IceCandidate candidate = it.next();
                        remoteParticipant.getPeerConnection().addIceCandidate(candidate);
                        it.remove();
                    }
                }
            }
        });

        peerConnection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
                new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY));
//        peerConnection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
//                new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY));

        this.remoteParticipants.get(connectionId).setPeerConnection(peerConnection);
    }

    public void createOfferForPublishing(MediaConstraints constraints) {
        localParticipant.getPeerConnection().createOffer(new CustomSdpObserver("createOffer") {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                super.onCreateSuccess(sdp);
                Log.i("createOffer SUCCESS", sdp.toString());
                localParticipant.getPeerConnection().setLocalDescription(new CustomSdpObserver("createOffer_setLocalDescription") {
                    @Override
                    public void onSetSuccess() {
                        super.onSetSuccess();
                        websocket.publishVideo(sdp);
                    }
                }, sdp);
            }
        }, constraints);
    }

    public void createAnswerForSubscribing(RemoteParticipant remoteParticipant, String streamId, MediaConstraints constraints) {
        remoteParticipant.getPeerConnection().createAnswer(new CustomSdpObserver("createAnswerSubscribing") {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                super.onCreateSuccess(sdp);
                Log.i("createAnswer SUCCESS", sdp.toString());
                remoteParticipant.getPeerConnection().setLocalDescription(new CustomSdpObserver("createAnswerSubscribing_setLocalDescription") {
                    @Override
                    public void onSetSuccess() {
                        super.onSetSuccess();
                        websocket.receiveVideoFrom(sdp, remoteParticipant, streamId);
                    }
                }, sdp);
            }
        }, constraints);
    }

    public String getId() {
        return this.id;
    }

    public String getToken() {
        try {
            JSONObject jsonObject = new JSONObject(this.token); // this.token은 JSON 문자열을 포함하고 있다고 가정
            return jsonObject.getString("token"); // 'token' 키에 해당하는 값을 추출
        } catch (JSONException e) {
            Log.e("getToken 에러", "JSON parsing error", e);
            return "";
        }
    }

    public void setIceServers(List<IceServer> iceServers) {
        this.iceServers = iceServers;
    }

    public LocalParticipant getLocalParticipant() {
        return this.localParticipant;
    }

    public void setLocalParticipant(LocalParticipant localParticipant) {
        this.localParticipant = localParticipant;
    }

    public RemoteParticipant getRemoteParticipant(String id) {
        return this.remoteParticipants.get(id);
    }

    public PeerConnectionFactory getPeerConnectionFactory() {
        return this.peerConnectionFactory;
    }

//    public EglBase getRootEglBase() {
//        return this.rootEglBase;
//    }

    public void addRemoteParticipant(RemoteParticipant remoteParticipant) {
        this.remoteParticipants.put(remoteParticipant.getConnectionId(), remoteParticipant);
    }

    public RemoteParticipant removeRemoteParticipant(String id) {
        return this.remoteParticipants.remove(id);
    }

    public void leaveSession() {
        AsyncTask.execute(() -> {
            websocket.setWebsocketCancelled(true);
            if (websocket != null) {
                websocket.leaveRoom();
                websocket.disconnect();
            }
            this.localParticipant.dispose();
        });

        AsyncTask.execute(() -> {
            for (RemoteParticipant remoteParticipant : remoteParticipants.values()) {
                if (remoteParticipant.getPeerConnection() != null) {
                    remoteParticipant.getPeerConnection().close();
                }
            }
            if (peerConnectionFactory != null) {
                peerConnectionFactory.dispose();
                peerConnectionFactory = null;
            }
        });
    }


}

