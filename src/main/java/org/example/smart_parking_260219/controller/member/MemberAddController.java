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
import java.util.List;

@Log4j2
@WebServlet(name = "memberAddController", value = "/member/member_add")
public class MemberAddController extends HttpServlet {

    private final MemberService memberService = MemberService.INSTANCE;
    private static final int SUBSCRIBED_FEE = 100000; // 구독 비용 고정값 (코드로만 수정 가능)

    /*
     * GET 분기
     *  - carNum 없음 → STEP1: 차량번호 조회 폼
     *  - carNum 있음, 결과 0건 → STEP2: 신규 회원 등록 폼 (바로 이동)
     *  - carNum 있음, 결과 1건 이상 → STEP3: 중복 목록 선택 (member_search와 동일)
     *  - step=renew + carNum → STEP4: 선택된 차량 갱신 폼
     */
    @SneakyThrows
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("member_add doGet");

        String step = req.getParameter("step");
        String carNum = req.getParameter("carNum");

        // STEP4: 선택된 차량 갱신 폼
        if ("renew".equals(step) && carNum != null && !carNum.trim().isEmpty()) {
            try {
                MemberDTO member = memberService.getOneMember(carNum.trim());
                if (member == null) {
                    resp.sendRedirect("/member/member_add");
                    return;
                }
                LocalDate today = LocalDate.now();
                LocalDate baseDate = member.getEndDate();
                LocalDate newStart = (baseDate == null || baseDate.isBefore(today))
                        ? today.plusDays(1) : baseDate.plusDays(1);
                LocalDate newEnd = newStart.plusMonths(1);

                req.setAttribute("member", member);
                req.setAttribute("newStart", newStart);
                req.setAttribute("newEnd", newEnd);
                req.setAttribute("step", "renew");
                req.getRequestDispatcher("/WEB-INF/views/member/member_add.jsp").forward(req, resp);
            } catch (Exception e) {
                log.error("member_add renew GET 오류", e);
                resp.sendRedirect("/member/member_list?error=fail");
            }
            return;
        }

        // STEP1: 차량번호 없음 → 조회 폼
        if (carNum == null || carNum.trim().isEmpty()) {
            req.getRequestDispatcher("/WEB-INF/views/member/member_add.jsp").forward(req, resp);
            return;
        }

        carNum = carNum.trim();
        req.setAttribute("searchCarNum", carNum);

        try {
            // member_search와 동일: 뒤 4자리 LIKE 검색
            List<MemberDTO> matchedMembers = memberService.getCarNum(carNum);

            if (matchedMembers == null || matchedMembers.isEmpty()) {
                // STEP2: 결과 없음 → 바로 신규 등록 폼
                log.info("신규 차량 → 등록 폼: {}", carNum);
                req.setAttribute("step", "register");
            } else {
                // STEP3: 중복 목록 → 선택 화면
                log.info("중복 차량 {}건 → 선택 화면: {}", matchedMembers.size(), carNum);
                req.setAttribute("matchedMembers", matchedMembers);
                req.setAttribute("step", "select");
            }
            req.getRequestDispatcher("/WEB-INF/views/member/member_add.jsp").forward(req, resp);

        } catch (Exception e) {
            log.error("member_add GET 오류", e);
            req.setAttribute("error", "fail");
            req.getRequestDispatcher("/WEB-INF/views/member/member_add.jsp").forward(req, resp);
        }
    }

    /*
     * POST 분기
     *  - action= register → 신규 회원 등록
     *  - action= renew → 기존 회원 월정액 1개월 갱신
     */
    @SneakyThrows
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("member_add doPost");

        String action = req.getParameter("action");
        String carNum = req.getParameter("carNum");

        try {
            if ("register".equals(action)) {
                int carType = Integer.parseInt(req.getParameter("carType"));
                String name = req.getParameter("name");
                String phone = req.getParameter("phone");
                LocalDate startDate = LocalDate.parse(req.getParameter("startDate"));
                LocalDate endDate = LocalDate.parse(req.getParameter("endDate"));

                MemberDTO memberDTO = MemberDTO.builder()
                        .carNum(carNum)
                        .carType(carType)
                        .name(name)
                        .phone(phone)
                        .subscribed(true)
                        .startDate(startDate)
                        .endDate(endDate)
                        .subscribedFee(SUBSCRIBED_FEE)
                        .build();

                memberService.addMember(memberDTO);
                log.info("회원 등록 완료: {} ({} ~ {})", carNum, startDate, endDate);

                forwardAlert(req, resp,
                        "월정액 회원 등록이 완료되었습니다.",
                        "redirect",
                        req.getContextPath() + "/member/member_list");

            } else if ("renew".equals(action)) {
                memberService.renewSubscription(carNum);
                log.info("월정액 갱신 완료: {}", carNum);

                forwardAlert(req, resp,
                        "월정액 1개월 갱신이 완료되었습니다.",
                        "redirect",
                        req.getContextPath() + "/member/member_list");

            } else {
                resp.sendRedirect("/member/member_list");
            }

        } catch (Exception e) {
            log.error("member_add POST 오류 상세", e);
            // 2. 모든 파라미터를 다 찍어봅니다.
            log.info("파라미터 상세 -> carType: {}, name: {}, startDate: {}, endDate: {}",
                    req.getParameter("carType"),
                    req.getParameter("name"),
                    req.getParameter("startDate"),
                    req.getParameter("endDate"));
            forwardAlert(req, resp,
                    "처리에 실패했습니다.",
                    "back",
                    null);
        }
    }

    /**
     * Servlet이 실행 가능한 script 문자열을 직접 만들지 않고 공통 알림 화면에 표시 정보만 전달한다.
     */
    private void forwardAlert(
            HttpServletRequest req,
            HttpServletResponse resp,
            String message,
            String action,
            String url
    ) throws ServletException, IOException {
        req.setAttribute("pageAlertMessage", message);
        req.setAttribute("pageAlertAction", action);
        if (url != null) {
            req.setAttribute("pageAlertUrl", url);
        }
        req.getRequestDispatcher("/WEB-INF/views/common/alert.jsp")
                .forward(req, resp);
    }
}
