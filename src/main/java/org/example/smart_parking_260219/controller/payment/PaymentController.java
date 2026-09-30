package org.example.smart_parking_260219.controller.payment;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.service.PaymentService;

import java.io.IOException;

@Log4j2
@WebServlet(name = "paymentController", value = "/payment/payment")
public class PaymentController extends HttpServlet {
    private final PaymentService paymentService = PaymentService.INSTANCE;

    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException {
        // 정산 화면은 출차 대상 검증을 거친 요청으로만 진입하도록 함
        resp.sendRedirect(req.getContextPath() + "/output");
    }

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws ServletException, IOException {
        int parkingId;
        int paymentType;

        try {
            parkingId = Integer.parseInt(req.getParameter("parkingId"));
            paymentType = Integer.parseInt(req.getParameter("paymentType"));
        } catch (NumberFormatException e) {
            log.warn("숫자 형식이 아닌 결제 요청값 수신");
            resp.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "유효하지 않은 결제 요청입니다."
            );
            return;
        }

        // Controller에서는 HTTP 요청값의 기본 형식과 범위만 확인함
        if (parkingId <= 0
                || !isValidPaymentType(paymentType)) {
            log.warn("허용 범위를 벗어난 결제 요청값 수신");
            resp.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "유효하지 않은 결제 요청입니다."
            );
            return;
        }

        try {
            // 조회, 계산, 결제, 출차, 공간 반환을 하나의 트랜잭션으로 처리함
            paymentService.completePaymentAndExit(
                    parkingId,
                    paymentType
            );

            log.info("결제 및 출차 처리 완료 - parkingId={}", parkingId);

            resp.sendRedirect(req.getContextPath() + "/dashboard");
        } catch (IllegalArgumentException e) {
            log.warn("유효하지 않은 결제 요청 - parkingId={}", parkingId);
            resp.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "유효하지 않은 결제 요청입니다."
            );
        } catch (IllegalStateException e) {
            // 트랜잭션 내부 오류는 Service에서 rollback된 상태임
            log.error(
                    "결제 및 출차 트랜잭션 실패 - parkingId={}",
                    parkingId,
                    e
            );
            throw new ServletException(
                    "결제 및 출차 처리에 실패했습니다.",
                    e
            );
        }
    }

    // 결제 수단이 카드(1), 현금(2), 월정액(3) 중 하나인지 확인함
    private boolean isValidPaymentType(int paymentType) {
        return paymentType >= 1 && paymentType <= 3;
    }
}
