package org.example.smart_parking_260219.controller.member;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.service.MemberService;

import java.io.IOException;

@Log4j2
@WebServlet(name = "memberDeleteController", value = "/member/member_delete")
public class MemberDeleteController extends HttpServlet {

    private final MemberService memberService = MemberService.INSTANCE;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("=== /member/member_delete GET 요청 ===");

        try {
            String carNum = req.getParameter("carNum");

            // 파라미터 검증
            if (carNum == null || carNum.trim().isEmpty()) {
                log.error("차량번호 파라미터 누락");
                resp.sendRedirect("/member/member_list?error=missing");
                return;
            }

            // 회원 삭제
            memberService.removeMember(carNum);

            // 성공 시 목록으로 리다이렉트
            resp.sendRedirect("/member/member_list?success=delete");

        } catch (Exception e) {
            log.error("회원 삭제 중 오류 발생", e);
            resp.sendRedirect("/member/member_list?error=deleteFail");
        }
    }
}
