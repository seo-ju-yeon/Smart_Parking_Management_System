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
    // 정산에 적용한 정책과 서버에서 계산한 금액을 결제 이력으로 저장함
    private static final String INSERT_PAYMENT_SQL =
            "INSERT INTO payment "
                    + "(parking_id, policy_id, payment_type, calculated_fee, "
                    + "discount_amount, final_fee, payment_date) "
                    + "VALUES (?, ?, ?, ?, ?, ?, NOW())";

    private static PaymentDAO instance;

    private PaymentDAO() {}

    public static PaymentDAO getInstance() {
        if(instance == null) {
            instance = new PaymentDAO();
        }
        return instance;
    }

    // 기존 단독 저장 흐름에서는 DAO가 Connection을 생성하고 반환함
    public void insertPayment(PaymentVO paymentVO) {
        try (Connection connection = DBConnection.INSTANCE.getConnection()) {
            int affectedRows = insertPayment(connection, paymentVO);

            // 결제 한 건이 정확히 저장되지 않으면 정상 처리로 판단하지 않음
            if (affectedRows != 1) {
                throw new SQLException("결제 정보가 정상적으로 저장되지 않았습니다.");
            }
        } catch (SQLException e) {
            log.error("결제 정보 저장 중 오류 발생", e);
            throw new RuntimeException("결제 정보 저장에 실패했습니다.", e);
        }
    }

    // 트랜잭션 Service가 전달한 Connection을 사용하며 DAO에서는 닫지 않음
    public int insertPayment(
            Connection connection,
            PaymentVO paymentVO
    ) throws SQLException {
        try (PreparedStatement preparedStatement =
                     connection.prepareStatement(INSERT_PAYMENT_SQL)) {
            preparedStatement.setInt(1, paymentVO.getParkingId());
            preparedStatement.setInt(2, paymentVO.getPolicyId());
            preparedStatement.setInt(3, paymentVO.getPaymentType());
            preparedStatement.setInt(4, paymentVO.getCalculatedFee());
            preparedStatement.setInt(5, paymentVO.getDiscountAmount());
            preparedStatement.setInt(6, paymentVO.getFinalFee());

            // 변경 행 수는 상위 Service가 성공 여부를 판단할 때 사용함
            return preparedStatement.executeUpdate();
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
