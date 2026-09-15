package org.example.smart_parking_260219.controller.member;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.MemberDTO;
import org.example.smart_parking_260219.service.MemberService;

import java.io.IOException;

@Log4j2
@WebServlet(name = "memberSubscribeController", value = "/member/member_subscribe")
public class MemberSubscribeController extends HttpServlet {
    private final MemberService memberService = MemberService.INSTANCE;

    @SneakyThrows
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /* 회원 등록시 기존 가입자 확인 controller */
        log.info("member_subscribe GET");

        String carNum = req.getParameter("carNum");

        // carNum 없으면 조회 폼 표시
        if (carNum == null || carNum.trim().isEmpty()) {
            req.getRequestDispatcher("/WEB-INF/views/member/member_subscribe.jsp").forward(req, resp);
            return;
        }

        carNum = carNum.trim();

        try {
            MemberDTO member = memberService.getOneMember(carNum);

            if (member == null) {
                // 차량번호 없음 → 회원등록 페이지로 이동
                log.info("차량번호 없음 → 회원등록: {}", carNum);
                resp.sendRedirect("/member/member_add?carNum=" + carNum + "&newMember=true");
                return;
            }

            // 차량번호 있음 → 월정액 등록 폼 표시
            log.info("차량번호 있음 → 월정액 등록: {}", carNum);
            req.setAttribute("member", member);
            req.getRequestDispatcher("/WEB-INF/views/member/member_subscribe.jsp").forward(req, resp);

        } catch (Exception e) {
            log.error("월정액 조회 오류", e);
            resp.sendRedirect("/member/member_list?error=fail");
        }
    }

    @SneakyThrows
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("member_subscribe POST");

        String carNum = req.getParameter("carNum");

        try {
            // 1개월 갱신: endDate 다음날부터 시작
            memberService.renewSubscription(carNum);
            log.info("월정액 등록 완료: {}", carNum);
            resp.sendRedirect("/member/member_detail?carNum=" + carNum + "&success=subscribe");

        } catch (Exception e) {
            log.error("월정액 등록 오류", e);
            resp.sendRedirect("/member/member_subscribe?carNum=" + carNum + "&error=fail");
        }
    }
}
