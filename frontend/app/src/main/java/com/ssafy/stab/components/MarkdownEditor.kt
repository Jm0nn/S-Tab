package com.ssafy.stab.components


import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ssafy.stab.R
import com.ssafy.stab.apis.space.share.getMarkdown
import com.ssafy.stab.apis.space.share.patchMarkdown
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer


fun parseMarkdownToHtml(markdown: String?, textAlign: String): String {
    val nonNullMarkdown = markdown ?: ""
    val parser = Parser.builder().build()
    val document = parser.parse(nonNullMarkdown)
    val renderer = HtmlRenderer.builder().build()
    val htmlContent = renderer.render(document)
    return "<div style='text-align:$textAlign;'>$htmlContent</div>"
}

@Composable
fun MarkdownViewer(htmlContent: String) {
    var webViewLoaded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true // 필요한 경우 자바스크립트 활성화
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            webViewLoaded = true
                        }
                    }
                    loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            },
            modifier = if (webViewLoaded) Modifier.fillMaxSize() else Modifier.size(0.dp)
        )
    }
}

@Composable
fun MarkdownScreen(spaceId: String) {
    var markdownText by remember { mutableStateOf("") }
    var textAlign by remember { mutableStateOf("left") }
    var isEditing by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val leftImg = painterResource(id = R.drawable.left_align)
    val centerImg = painterResource(id = R.drawable.center_align)
    val rightImg = painterResource(id = R.drawable.right_align)

    LaunchedEffect(key1 = spaceId) {
        getMarkdown(spaceId) { res ->
            val markdownData = res.data ?: ""
            markdownText = markdownData
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isEditing) {
            Column(
                modifier = Modifier
                    .height(200.dp)
                    .verticalScroll(scrollState)
            ) {
                MarkdownEditor(
                    onMarkdownChange = { markdownText = it },
                    markdownText = markdownText
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clickable {
                                patchMarkdown(spaceId, markdownText)
                                isEditing = false
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(color = Color(0xFFDCE3F1))
                            .padding(5.dp)
                    ) {
                        Text("완료")
                    }
                    Spacer(modifier = Modifier.width(40.dp))
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.width(20.dp))
                    Row {
                        Image(
                            painter = leftImg, contentDescription = null,
                            modifier = Modifier
                                .clickable { textAlign = "left" }
                                .height(20.dp)
                                .width(20.dp)
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Image(
                            painter = centerImg, contentDescription = null,
                            modifier = Modifier
                                .clickable { textAlign = "center" }
                                .height(20.dp)
                                .width(20.dp)
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Image(
                            painter = rightImg, contentDescription = null,
                            modifier = Modifier
                                .clickable { textAlign = "right" }
                                .height(20.dp)
                                .width(20.dp)
                        )
                        Spacer(modifier = Modifier.width(50.dp))
                    }
                }
                Column(
                    modifier = Modifier
                        .clickable { isEditing = true }
                        .height(300.dp)
                        .verticalScroll(scrollState)
                        .padding(40.dp, 0.dp)
                ) {
                    MarkdownViewer(htmlContent = parseMarkdownToHtml(markdownText, textAlign))
                }
            }
        }
        Divider(color = Color.Gray, thickness = 1.dp)
    }
}

@Composable
fun MarkdownEditor(onMarkdownChange: (String) -> Unit, markdownText: String) {
    TextField(
        value = markdownText,
        onValueChange = onMarkdownChange,
        label = { Text("표지 작성") },
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    )
}