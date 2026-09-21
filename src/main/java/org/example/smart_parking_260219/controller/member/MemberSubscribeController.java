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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

        // 기존 회원 등록 화면에서 차량번호 조회를 시작한다.
        if (carNum == null || carNum.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/member/member_add");
            return;
        }

        carNum = carNum.trim();
        String encodedCarNum = URLEncoder.encode(carNum, StandardCharsets.UTF_8);

        try {
            MemberDTO member = memberService.getOneMember(carNum);

            if (member == null) {
                // 차량번호 없음 → 회원등록 페이지로 이동
                log.info("기존 회원 조회 결과 없음 - 회원 등록 화면으로 이동");
                resp.sendRedirect(req.getContextPath() + "/member/member_add?carNum=" + encodedCarNum);
                return;
            }

            // 차량번호 있음 → 기존 회원 등록 화면의 월정액 갱신 단계로 이동
            log.info("기존 회원 조회 완료 - 월정액 갱신 화면으로 이동");
            resp.sendRedirect(req.getContextPath()
                    + "/member/member_add?step=renew&carNum=" + encodedCarNum);

        } catch (Exception e) {
            log.error("월정액 조회 오류", e);
            resp.sendRedirect(req.getContextPath() + "/member/member_list?error=fail");
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
            String encodedCarNum = URLEncoder.encode(carNum, StandardCharsets.UTF_8);
            resp.sendRedirect(req.getContextPath()
                    + "/member/member_detail?carNum=" + encodedCarNum + "&success=subscribe");

        } catch (Exception e) {
            log.error("월정액 등록 오류", e);
            String encodedCarNum = URLEncoder.encode(carNum, StandardCharsets.UTF_8);
            resp.sendRedirect(req.getContextPath()
                    + "/member/member_subscribe?carNum=" + encodedCarNum + "&error=fail");
        }
    }
}
