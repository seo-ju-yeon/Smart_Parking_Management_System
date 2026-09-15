package org.example.smart_parking_260219.controller.policy;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.smart_parking_260219.dto.FeePolicyDTO;
import org.example.smart_parking_260219.dto.PageResponseDto;
import org.example.smart_parking_260219.service.FeePolicyService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "FeePolicyListController", value = "/view/policy/list")
public class FeePolicyListController extends HttpServlet {
    private final FeePolicyService feePolicyService = FeePolicyService.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //  페이지 번호 파라미터 받기 (전달값이 없으면 기본 1페이지)
        String pageParam = req.getParameter("pageNum");
        int pageNum = (pageParam != null) ? Integer.parseInt(pageParam) : 1;
        int size = 10; // 한 페이지에 보여줄 개수 설정

        //  서비스에서 정책 목록 조회
        List<FeePolicyDTO> allList = feePolicyService.getPolicyList();
        int totalCount = allList.size(); // 총 개수

        //  페이징 로직 추가 (리스트 자르기)
        int fromIndex = (pageNum - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalCount);

        List<FeePolicyDTO> boardList;
        if (fromIndex < totalCount) {
            boardList = allList.subList(fromIndex, toIndex);
        } else {
            boardList = new ArrayList<>();
        }

        //  전체 페이지 수 계산
        int totalPage = (int) Math.ceil((double) totalCount / size);

        //  PageResponseDto 생성 및 데이터 설정
        PageResponseDto pageResponseDto = new PageResponseDto();
        pageResponseDto.setBoardList(boardList); // 리스트 담기
        pageResponseDto.setTotalCount(totalCount); // 전체 건수
        pageResponseDto.setPageNum(pageNum); // 현재 페이지 (기본 1)
        pageResponseDto.setTotalPage(totalPage); // 전체 페이지 수 (기본 1)

        //  request에 담기
        req.setAttribute("pageResponseDto", pageResponseDto);

        //  목록 JSP로 포워딩
        req.getRequestDispatcher("/WEB-INF/views/policy/list.jsp").forward(req, resp);
    }
}
