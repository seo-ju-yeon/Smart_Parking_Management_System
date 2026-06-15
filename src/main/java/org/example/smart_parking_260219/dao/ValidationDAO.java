package org.example.smart_parking_260219.dao;

import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.vo.ValidationVO;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * validation 테이블의 DB 작업을 처리하는 DAO입니다.
 *
 * <p>
 * 이메일 인증번호 저장, 조회, 삭제 기능을 담당합니다.
 * </p>
 */
@Log4j2
public class ValidationDAO {

    // 인증정보 유효 시간
    private static final int EXPIRY_MINUTES = 5;

    /**
     * 이메일 인증 정보를 DB에 저장합니다.
     *
     * <p>
     * 인증번호는 현재 시각 기준 5분 뒤 만료되도록 저장합니다.
     * </p>
     *
     * @param validationVO 저장할 이메일 인증 정보
     */
    public void insert(ValidationVO validationVO) {
        String sql = "INSERT INTO validation (string_otp, email, expiry_time) VALUES (?, ?, ?)";

        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);

            preparedStatement.setString(1, validationVO.getStringOTP());
            preparedStatement.setString(2, validationVO.getEmail());
            // 현재 시간(LocalDateTime.now()) 기준 5분(plusMinutes(EXPIRY_MINUTES)) 뒤를 만료 시간으로 설정
            preparedStatement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES)));

            preparedStatement.executeUpdate();

            log.info("인증 정보 DB 저장 완료: " + validationVO.getEmail());
        } catch (SQLException e) {
            log.error("인증 정보 DB 저장 실패", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 이메일로 인증 정보를 조회합니다.
     *
     * @param email 조회할 이메일
     * @return 조회된 인증 정보, 없으면 null
     */
    public ValidationVO select(String email) {
        String sql = "SELECT * FROM validation WHERE email = ?";

        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);

            preparedStatement.setString(1, email);

            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Timestamp timestamp = resultSet.getTimestamp("expiry_time");
                // DB의 TIMESTAMP 값을 LocalDateTime으로 변환
                LocalDateTime expiryTime = timestamp.toLocalDateTime();

                ValidationVO validationVO = ValidationVO.builder()
                        .no(resultSet.getInt("no"))
                        .stringOTP(resultSet.getString("string_otp"))
                        .email(resultSet.getString("email"))
                        .expiryTime(expiryTime)
                        .build();

                log.info("인증 정보 조회 완료: {}, 만료시간: {}", email, expiryTime);
                return validationVO;
            }

            log.warn("인증 정보 없음: {}", email);
            return null;
        } catch (SQLException e) {
            log.error("인증 정보 조회 실패", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 이메일에 저장된 기존 인증 정보를 삭제합니다.
     *
     * <p>
     * 인증번호를 재발송할 때 이전 인증번호를 제거하기 위해 사용합니다.
     * </p>
     *
     * @param email 인증 정보를 삭제할 이메일
     */
    public void deleteByEmail(String email) {
        String sql = "DELETE FROM validation WHERE email = ?";

        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);

            preparedStatement.setString(1, email);

            // 재발송 전에 기존 인증번호 삭제
            int deleted = preparedStatement.executeUpdate();

            log.info("기존 인증 정보 삭제: {} ({}건)", email, deleted);
        } catch (SQLException e) {
            log.error("인증 정보 삭제 실패", e);
            throw new RuntimeException(e);
        }
    }
}
