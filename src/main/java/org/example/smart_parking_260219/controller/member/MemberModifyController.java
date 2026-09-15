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
import java.time.LocalDate;

@Log4j2
@WebServlet(name = "memberModifyController", value = "/member/member_modify")
public class MemberModifyController extends HttpServlet {

    private final MemberService memberService = MemberService.INSTANCE;

    @SneakyThrows
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("member_modify doGet");
        try {
            String carNum = req.getParameter("carNum");
            if (carNum == null || carNum.trim().isEmpty()) {
                resp.sendRedirect("/member/member_list?error=missing");
                return;
            }
            MemberDTO member = memberService.getOneMember(carNum.trim());
            if (member == null) {
                resp.sendRedirect("/member/member_list?error=notFound");
                return;
            }

            // 갱신 예상 날짜 계산 (JSP에서 사용)
            LocalDate today = LocalDate.now();
            LocalDate baseDate = member.getEndDate();
            LocalDate newStart = (baseDate == null || baseDate.isBefore(today))
                    ? today.plusDays(1) : baseDate.plusDays(1);
            LocalDate newEnd = newStart.plusMonths(1);

            // page 파라미터 유지
            String page = req.getParameter("page");
            if (page == null || page.isEmpty()) page = "1";

            req.setAttribute("member", member);
            req.setAttribute("newStart", newStart);
            req.setAttribute("newEnd", newEnd);
            req.setAttribute("page", page);
            req.getRequestDispatcher("/WEB-INF/views/member/member_modify.jsp").forward(req, resp);

        } catch (Exception e) {
            log.error("수정 페이지 오류", e);
            resp.sendRedirect("/member/member_list?error=fail");
        }
    }

    @SneakyThrows
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("member_modify doPost");

        String carNum = req.getParameter("carNum");
        String action = req.getParameter("action");
        String page   = req.getParameter("page");
        if (page == null || page.isEmpty()) page = "1";

        try {
            // 1개월 갱신
            if ("renew".equals(action)) {
                memberService.renewSubscription(carNum);
                log.info("수정 페이지에서 갱신 완료: {}", carNum);
                resp.sendRedirect("/member/member_list?success=renew&page=" + page);
                return;
            }

            // 일반 수정 (이름, 전화번호만)
            String memberIdStr = req.getParameter("memberId");
            int memberId = (memberIdStr != null && !memberIdStr.isEmpty())
                    ? Integer.parseInt(memberIdStr) : 0;

            String name = req.getParameter("name");
            String phone = req.getParameter("phone");

            // 날짜·구독 정보는 hidden으로 기존 값 유지
            String startDateStr = req.getParameter("startDate");
            String endDateStr = req.getParameter("endDate");
            LocalDate startDate = (startDateStr != null && !startDateStr.isEmpty())
                    ? LocalDate.parse(startDateStr) : null;
            LocalDate endDate = (endDateStr != null && !endDateStr.isEmpty())
                    ? LocalDate.parse(endDateStr) : null;

            // endDate 기준 구독 상태 자동 계산
            boolean subscribed = (endDate != null && !endDate.isBefore(LocalDate.now()));

            MemberDTO memberDTO = MemberDTO.builder()
                    .carNum(carNum)
                    .memberId(memberId)
                    .carType(2)
                    .name(name)
                    .phone(phone)
                    .subscribed(subscribed)
                    .startDate(startDate)
                    .endDate(endDate)
                    .subscribedFee(100000)
                    .build();

            memberService.modifyMember(memberDTO);
            log.info("회원 수정 완료: {}", carNum);
            resp.sendRedirect("/member/member_list?success=modify&page=" + page);

        } catch (Exception e) {
            log.error("회원 수정 오류", e);
            resp.sendRedirect("/member/member_list?error=modifyFail&page=" + page);
        }
    }
}