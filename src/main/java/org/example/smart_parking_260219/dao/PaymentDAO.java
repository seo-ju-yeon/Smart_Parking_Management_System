package org.example.smart_parking_260219.dao;

import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.vo.PaymentVO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Log4j2

public class PaymentDAO {

    private static PaymentDAO instance;

    private PaymentDAO() {}

    public static PaymentDAO getInstance() {
        if(instance == null) {
            instance = new PaymentDAO();
        }
        return instance;
    }

    // 등록 - 요금 계산 후 결제 정보 저장
    public void insertPayment(PaymentVO vo) {
        // 1. 해당 차량의 '출차 전' 주차 기록 ID 조회 (차 번호 존재 여부 확인 대용)
        // 2. 결제 내역(payment) 등록
        // 3. 주차 기록(parking)의 결제 완료 및 출차 시간 업데이트

        String sql = "INSERT INTO payment (parking_id, policy_id, payment_type, calculated_fee, discount_amount, final_fee, payment_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, NOW())";

        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);

            // 결제 정보 등록
            preparedStatement.setInt(1, vo.getParkingId());
            preparedStatement.setInt(2, vo.getPolicyId());
            preparedStatement.setInt(3, vo.getPaymentType());
            preparedStatement.setInt(4, vo.getCalculatedFee());
            preparedStatement.setInt(5, vo.getDiscountAmount());
            preparedStatement.setInt(6, vo.getFinalFee());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException();
        }
    }

    // 전체 조회 - 결제 목록 출력 (차량번호, 타입, 시간 포함)
    public List<PaymentVO> selectAllPayments() {
        List<PaymentVO> paymentVOList = new ArrayList<>();
        // 쿼리문에 p.car_type과 p.total_time을 명시해줘야 합니다.
        String sql = "SELECT pay.*, p.car_num, p.car_type, p.total_time FROM payment pay "
                + "JOIN parking p ON pay.parking_id = p.parking_id "
                + "ORDER BY pay.payment_id DESC";
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                PaymentVO paymentVO = PaymentVO.builder()
                        .paymentId(resultSet.getInt("payment_id"))
                        .parkingId(resultSet.getInt("parking_id"))
                        .policyId(resultSet.getInt("policy_id"))
                        .carNum(resultSet.getString("car_num"))
                        .carType(resultSet.getInt("car_type"))
                        .totalTime(resultSet.getInt("total_time"))
                        .paymentType(resultSet.getInt("payment_type"))
                        .calculatedFee(resultSet.getInt("calculated_fee"))
                        .discountAmount(resultSet.getInt("discount_amount"))
                        .finalFee(resultSet.getInt("final_fee"))
                        .paymentDate(resultSet.getTimestamp("payment_date").toLocalDateTime()).build();
                paymentVOList.add(paymentVO);
            }
        } catch (SQLException e) {
            log.error("결제 목록 조회 중 오류 발생", e);
            throw new RuntimeException(e);
        }
        return paymentVOList;
    }

    // 해당 날짜 목록 조회
    public List<PaymentVO> selectPaymentByDate(String targetDate) {
        String sql = "SELECT pay.*, park.car_type, park.car_num, park.total_time " +
                "FROM payment pay " +
                "JOIN parking park ON pay.parking_id = park.parking_id " +
                "WHERE DATE(pay.payment_date) = ?";
        List<PaymentVO> paymentVOList = new ArrayList<>();

        LocalDate date = LocalDate.parse(targetDate);

        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setObject(1, date);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                PaymentVO paymentVO = PaymentVO.builder()
                        .paymentId(resultSet.getInt("payment_id"))
                        .parkingId(resultSet.getInt("parking_id"))
                        .policyId(resultSet.getInt("policy_id"))
                        .carNum(resultSet.getString("car_num"))
                        .carType(resultSet.getInt("car_type"))
                        .totalTime(resultSet.getInt("total_time"))
                        .paymentType(resultSet.getInt("payment_type"))
                        .calculatedFee(resultSet.getInt("calculated_fee"))
                        .discountAmount(resultSet.getInt("discount_amount"))
                        .finalFee(resultSet.getInt("final_fee"))
                        .paymentDate(resultSet.getTimestamp("payment_date").toLocalDateTime()).build();
                paymentVOList.add(paymentVO);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return paymentVOList;
    }
}
