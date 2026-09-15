package org.example.smart_parking_260219.controller.member;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.MemberDTO;
import org.example.smart_parking_260219.service.MemberService;

import java.io.IOException;
import java.util.List;

@Log4j2
@WebServlet(name = "memberSearchController", value = "/member/member_search")
public class MemberSearchController extends HttpServlet {
    private final MemberService memberService = MemberService.INSTANCE;

    // GET: 검색 폼 표시
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("member_search GET");

        String carNum = req.getParameter("carNum");

        // 검색어 없으면 검색 폼만 표시
        if (carNum == null || carNum.trim().isEmpty()) {
            req.getRequestDispatcher("/WEB-INF/views/member/member_search.jsp").forward(req, resp);
            return;
        }

        carNum = carNum.trim();
        log.info("검색 차량번호: {}", carNum);

        try {
            // 차량번호 4자리로 검색
            List<MemberDTO> matchedMembers = memberService.getCarNum(carNum);

            if (matchedMembers == null || matchedMembers.isEmpty()) {
                log.warn("검색 결과 없음: {}", carNum);
                req.setAttribute("error", "notFound");
                req.setAttribute("searchCarNum", carNum);
                req.getRequestDispatcher("/WEB-INF/views/member/member_search.jsp").forward(req, resp);
                return;
            }

            // 1건이면 바로 상세 페이지로
            if (matchedMembers.size() == 1) {
                log.info("검색 결과 1건 - 상세 페이지로 이동");
                resp.sendRedirect("/member/member_detail?carNum=" + matchedMembers.get(0).getCarNum());
                return;
            }

            // 여러 건이면 선택 페이지로
            log.info("검색 결과 {}건 - 선택 페이지로 이동", matchedMembers.size());
            req.setAttribute("matchedMembers", matchedMembers);
            req.setAttribute("searchCarNum", carNum);
            req.getRequestDispatcher("/WEB-INF/views/member/member_select.jsp").forward(req, resp);

        } catch (Exception e) {
            log.error("회원 검색 중 오류", e);
            req.setAttribute("error", "fail");
            req.getRequestDispatcher("/WEB-INF/views/member/member_search.jsp").forward(req, resp);
        }
    }
}
