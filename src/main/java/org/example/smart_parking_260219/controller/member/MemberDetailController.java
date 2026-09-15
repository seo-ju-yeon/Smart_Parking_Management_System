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
import java.time.LocalDate;
import java.util.List;

@Log4j2
@WebServlet(name = "memberDetailController", value = "/member/member_detail")
public class MemberDetailController extends HttpServlet {
    private final MemberService memberService = MemberService.INSTANCE;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("member_detail doGet");

        try {
            String carNum = req.getParameter("carNum");
            log.info("회원 조회 - 입력된 번호: {}", carNum);

            // 파라미터 검증
            if (carNum == null || carNum.trim().isEmpty()) {
                log.error("차량번호 파라미터 누락");
                resp.sendRedirect("/member/member_search.jsp?error=missing");
                return;
            }
            MemberDTO member = memberService.getOneMember(carNum.trim());

            if (member == null) {
                resp.sendRedirect("/member/member_list?error=notFound");
                return;
            }

            if (member != null) {
                // DB의 subscribed 상태와 별개로, 현재 날짜 기준 만료 여부 판단
                boolean isExpired = member.getEndDate() != null && member.getEndDate().isBefore(LocalDate.now());
                req.setAttribute("isExpired", isExpired);
            }

            List<MemberDTO> history = memberService.getMemberHistory(carNum.trim());
            req.setAttribute("history", history);

            String page = req.getParameter("page");
            if (page == null || page.isEmpty()) page = "1";

            req.setAttribute("member", member);
            req.setAttribute("page", page);
            req.getRequestDispatcher("/WEB-INF/views/member/member_detail.jsp").forward(req, resp);

        } catch (Exception e) {
            log.error("회원 조회 오류: {}", e.getMessage(), e);
            String msg = java.net.URLEncoder.encode(
                    e.getClass().getSimpleName() + ": " + e.getMessage(), "UTF-8");
            resp.sendRedirect("/member/member_list?error=fail&debug=" + msg);
        }
    }
}