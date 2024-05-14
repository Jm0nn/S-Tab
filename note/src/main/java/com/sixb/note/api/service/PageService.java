package com.sixb.note.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sixb.note.dto.page.*;
import com.sixb.note.dto.pageData.*;
import com.sixb.note.entity.Note;
import com.sixb.note.entity.Page;
import com.sixb.note.exception.NoteNotFoundException;
import com.sixb.note.exception.PageNotFoundException;
import com.sixb.note.repository.NoteRepository;
import com.sixb.note.repository.PageRepository;
import com.sixb.note.util.IdCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PageService {

    private final PageRepository pageRepository;
    private final NoteRepository noteRepository;

    private final String initData = "{\"paths\": [], \"figures\": [], \"textBoxes\": [], \"images\": []}";

    public PageCreateResponseDto createPage(PageCreateRequestDto request) throws PageNotFoundException {
        String beforePageId = request.getBeforePageId();

        Page newPage = createNewPage(beforePageId);

        if (newPage == null) {
            throw new PageNotFoundException("이전 페이지 정보가 없습니다.");
        }

        Page beforePage = pageRepository.findPageById(beforePageId); // 이렇게 할지, 앞에서 받을지 고민

        // 앞페이지에 이어진 페이지 찾기
        Page connectPage = pageRepository.getNextPageByPageId(beforePageId);
        // 페이지가 있다면
        if (connectPage != null) {
            // 그 페이지와 새로운 페이지 연결
            newPage.setNextPage(connectPage);
            // 앞페이지와 연결 삭제
            pageRepository.deleteNextPageRelation(beforePageId);
        }

        // 페이지 링크하기
        beforePage.setNextPage(newPage);

        // responsedto에 넣기
        PageCreateResponseDto responseDto = PageCreateResponseDto.builder()
                .pageId(newPage.getPageId())
                .color(newPage.getColor())
                .template(newPage.getTemplate())
                .direction(newPage.getDirection())
                .updatedAt(newPage.getUpdatedAt())
                .build();

        // db에 저장하고 반환
        pageRepository.save(newPage);
        pageRepository.save(beforePage);

        return responseDto;
    }

    public void deletePage(String pageId) throws PageNotFoundException {
        Page page = pageRepository.findPageById(pageId);
        if (page != null) {
            Boolean deleteStatus = page.getIsDeleted();
            if (deleteStatus == false) {
                page.setIsDeleted(true);
                pageRepository.save(page);
            } else {
                throw new PageNotFoundException("이미 삭제된 페이지입니다.");
            }
        } else {
            throw new PageNotFoundException("페이지를 찾을 수 없습니다.");
        }
    }

    // 데이터 저장
    public void saveData(SaveDataRequestDto request) throws PageNotFoundException, JsonProcessingException {
        System.out.println("request:" + request);
        String pageId = request.getPageId();
        System.out.println(pageId);
        Page page = pageRepository.findPageById(pageId);

        LocalDateTime now = LocalDateTime.now();

        if (page != null) {
            Note note = noteRepository.findNoteById(page.getNoteId());
            System.out.println(note.getNoteId());
            if (note == null) {
                throw new PageNotFoundException("노트를 찾을 수 없습니다.");
            }
            if (page.getIsDeleted() == false) {
                // 형식 검사?
                PageDataDto pageData = request.getPageData();

                System.out.println(pageData.toString());

                ObjectMapper mapper = new ObjectMapper();

                String pageDataString = mapper.writeValueAsString(pageData);

                page.setUpdatedAt(now);
                page.setPageData(pageDataString);
                note.setUpdatedAt(now);
                pageRepository.save(page);
                noteRepository.save(note);
            } else {
                throw new PageNotFoundException("이미 삭제된 페이지입니다.");
            }

        } else {
            throw new PageNotFoundException("페이지를 찾을 수 없습니다.");
        }
    }

    public PageUpdateDto updatePage(PageUpdateDto request) throws PageNotFoundException {
        String pageId = request.getPageId();
        Page page = pageRepository.findPageById(pageId);
        if (page != null) {
            Boolean deleteStatus = page.getIsDeleted();
            if (deleteStatus == false) {
                // 양식 정보 수정
                page.setTemplate(request.getTemplate());
                page.setColor(request.getColor());
                page.setDirection(request.getDirection());

                pageRepository.save(page);

                return request;
            } else {
                throw new PageNotFoundException("이미 삭제된 페이지입니다.");
            }
        } else {
            throw new PageNotFoundException("페이지를 찾을 수 없습니다.");
        }
    }

    // 페이지 조회
    public PageListResponseDto getPageList(long userId, String noteId) throws NoteNotFoundException, PageNotFoundException, JsonProcessingException {
        Note note = noteRepository.findNoteById(noteId);
        if (note != null) {
            List<PageInfoDto> pageInfoList = new ArrayList<>();

            // noteId에 연결되어있는 페이지 모두 불러오기
            List<Page> pageList = pageRepository.findAllPagesByNoteId(noteId);

            for (Page page : pageList) {
                PageInfoDto pageInfoDto = setPageInfoDto(page);
                // pageInfoList에 넣기
                pageInfoList.add(pageInfoDto);
            }

            return PageListResponseDto.builder()
                    .data(pageInfoList)
                    .title(note.getTitle())
                    .build();
        } else {
            throw new NoteNotFoundException("노트를 찾을 수 없습니다.");
        }

    }

    // 페이지 링크 - 보류
//    public void linkPage(PageLinkRequestDto request) throws PageNotFoundException {
//        Page linkPage = pageRepository.findPageById(request.getLinkPageId());
//        Page targetPage = pageRepository.findPageById(request.getTargetPageId());
//    }

    public PageInfoDto copyPage(PageCopyRequestDto request) throws PageNotFoundException, JsonProcessingException {
        String beforePageId = request.getBeforePageId();
        Page beforePage = pageRepository.findPageById(beforePageId);
        Page targetPage = pageRepository.findPageById(request.getTargetPageId());

        if (beforePage != null && targetPage != null) {
            if (noteRepository.findNoteById(beforePage.getNoteId()) == null) {
                throw new PageNotFoundException("no note found.");
            }

            // before 페이지에 이어서 페이지 만들기
            Page newPage = createNewPage(beforePageId);

            newPage.setPdfUrl(beforePage.getPdfUrl()); // nullPointException 안나나?
            newPage.setPdfPage(beforePage.getPdfPage());
            newPage.setPageData(beforePage.getPageData());

            // 이전페이지에 이어진 페이지 찾기
            Page connectPage = pageRepository.getNextPageByPageId(beforePageId);
            // 페이지가 있다면
            if (connectPage != null) {
                // 그 페이지와 새로운 페이지 연결
                newPage.setNextPage(connectPage);
                // 앞페이지와 연결 삭제
                pageRepository.deleteNextPageRelation(beforePageId);
            }

            // 페이지 링크하기
            beforePage.setNextPage(newPage);

            // responsedto에 넣기
            PageInfoDto response = setPageInfoDto(newPage);

            // db에 저장하고 반환
            pageRepository.save(newPage);
            pageRepository.save(beforePage);

            return response;

        } else {
            throw new PageNotFoundException("페이지를 찾을 수 없습니다.");
        }
    }

    // pdf 가져오기
    public void pdfPage(PagePdfRequestDto request) throws PageNotFoundException {

        String beforePageId = request.getBeforePageId();
        Page beforePage = pageRepository.findPageById(beforePageId);
        String pdfUrl = request.getPdfUrl();
        int pdfPageCount = request.getPdfPageCount();

        // 앞페이지에 이어진 페이지 찾기
        Page connectPage = pageRepository.getNextPageByPageId(beforePageId);

        // 페이지가 있다면
        if (connectPage != null) {
            // 앞페이지와 연결 삭제
            pageRepository.deleteNextPageRelation(beforePageId);
        }

        if (beforePage != null) {
            // pdfcount 만큼 for문 돌면서 페이지 생성하기
            for (int i = pdfPageCount; i > 0; i--) {

                Page page = createNewPage(beforePageId);
                // 추가 정보 저장
                page.setTemplate("blank");
                page.setColor("white");
                page.setPdfPage(i);
                page.setPdfUrl(pdfUrl);

                // 페이지 링크하기
                if (connectPage != null) {
                    page.setNextPage(connectPage);
                }
                pageRepository.save(page);
                connectPage = page; // 이렇게 재할당 해도 되나요?
            }

            // before페이지 링크하기
            beforePage.setNextPage(connectPage);

            // 페이지 저장
            pageRepository.save(beforePage);

        } else {
            throw new PageNotFoundException("페이지를 찾을 수 없습니다.");
        }

    }

    private Page createNewPage(String beforePageId) {
        // id로 이전 페이지 정보를 찾아
        Optional<Page> pageOptional = Optional.ofNullable(pageRepository.findPageById(beforePageId));
        Page beforePage = pageOptional.orElse(null);
        if (beforePage != null && !beforePage.getIsDeleted()) { // 페이지를 찾았고, 삭제된 페이지가 아닌 경우
            LocalDateTime now = LocalDateTime.now();
            String pageId = IdCreator.create("p");

            Page newPage = Page.builder()// pdf 부분은 나가서 지정
                    .pageId(pageId)
                    .color(beforePage.getColor())
                    .template(beforePage.getTemplate())
                    .direction(beforePage.getDirection())
                    .noteId(beforePage.getNoteId())
                    .pageData(initData)
                    .build();
            newPage.setCreatedAt(now);
            newPage.setUpdatedAt(now);
            
            return newPage;
        } else {
            return null;
        }
    }

    // 이렇게 함수를 만들어서 사용할지 그냥 쓸지 고민중
//    private Page getPageById(String pageId) {
//        Optional<Page> pageOptional = Optional.ofNullable(pageRepository.findPageById(pageId));
//        // 이전 페이지 정보가 있다면
//        return pageOptional.orElse(null);
//    }

    private PageInfoDto setPageInfoDto (Page newPage) throws JsonProcessingException {
        String pageDataString = newPage.getPageData();
        ObjectMapper mapper = new ObjectMapper();
        PageDataDto pageDataDto = mapper.readValue(pageDataString, PageDataDto.class);
        return PageInfoDto.builder()
                .pageId(newPage.getPageId())
                .color(newPage.getColor())
                .template(newPage.getTemplate())
                .direction(newPage.getDirection())
                .pdfPage(newPage.getPdfPage())
                .pdfUrl(newPage.getPdfUrl())
                .updatedAt(newPage.getUpdatedAt())
                .isBookmarked(false)
                .paths(pageDataDto.getPaths())
                .figures(pageDataDto.getFigures())
                .images(pageDataDto.getImages())
                .textBoxes(pageDataDto.getTextBoxes())
                .build();
    }
}
