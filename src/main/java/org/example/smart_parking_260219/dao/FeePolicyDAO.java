package org.example.smart_parking_260219.dao;

import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.vo.FeePolicyVO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class FeePolicyDAO {
    private static FeePolicyDAO instance;

    public static FeePolicyDAO getInstance() {
        if (instance == null) {
            instance = new FeePolicyDAO();
        }
        return instance;
    }

    // 새로운 정책 등록 (생성된 PK 반환)
    public void insertPolicy(FeePolicyVO feePolicyVo) {
        /* 데이터베이스에 정책을 추가하는 메서드 */
        LocalDateTime registrationTime = LocalDateTime.now();

        String sql = "INSERT INTO fee_policy " +
                " (grace_period, default_time, default_fee, extra_time, extra_fee, light_discount, " +
                "disabled_discount, subscribed_fee, max_daily_fee, is_active, modify_date) " +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, feePolicyVo.getGracePeriod());
            preparedStatement.setInt(2, feePolicyVo.getDefaultTime());
            preparedStatement.setInt(3, feePolicyVo.getDefaultFee());
            preparedStatement.setInt(4, feePolicyVo.getExtraTime());
            preparedStatement.setInt(5, feePolicyVo.getExtraFee());
            preparedStatement.setDouble(6, feePolicyVo.getLightDiscount());
            preparedStatement.setDouble(7, feePolicyVo.getDisabledDiscount());
            preparedStatement.setInt(8, feePolicyVo.getSubscribedFee());
            preparedStatement.setInt(9, feePolicyVo.getMaxDailyFee());
            preparedStatement.setBoolean(10, feePolicyVo.isActive());

            preparedStatement.setTimestamp(11, Timestamp.valueOf(registrationTime));

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // [전체 조회] 요금 정책 목록 출력
    public List<FeePolicyVO> selectAllPolicies() {
        List<FeePolicyVO> list = new ArrayList<>();

        String sql = "SELECT * FROM fee_policy ORDER BY policy_id DESC";

        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                FeePolicyVO vo = FeePolicyVO.builder()
                        .policyId(resultSet.getInt("policy_id"))
                        .gracePeriod(resultSet.getInt("grace_period"))
                        .defaultTime(resultSet.getInt("default_time"))
                        .defaultFee(resultSet.getInt("default_fee"))
                        .extraTime(resultSet.getInt("extra_time"))
                        .extraFee(resultSet.getInt("extra_fee"))
                        .lightDiscount(resultSet.getDouble("light_discount"))
                        .disabledDiscount(resultSet.getDouble("disabled_discount"))
                        .subscribedFee(resultSet.getInt("subscribed_fee"))
                        .maxDailyFee(resultSet.getInt("max_daily_fee"))
                        .isActive(resultSet.getBoolean("is_active"))
                        .modifyDate(resultSet.getTimestamp("modify_date") != null
                                ? resultSet.getTimestamp("modify_date").toLocalDateTime()
                                : null)
                        .build();

                list.add(vo);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // [단건 조회] 요금 정책 상세 출력 (ID 기반)
    public FeePolicyVO selectPolicyById(int id) {
        String sql = "SELECT * FROM fee_policy WHERE policy_id = ?";

        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, id);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) return null;

            Timestamp ts = resultSet.getTimestamp("modify_date");
            return FeePolicyVO.builder()
                    .policyId(resultSet.getInt("policy_id"))
                    .gracePeriod(resultSet.getInt("grace_period"))
                    .defaultTime(resultSet.getInt("default_time"))
                    .defaultFee(resultSet.getInt("default_fee"))
                    .extraTime(resultSet.getInt("extra_time"))
                    .extraFee(resultSet.getInt("extra_fee"))
                    .lightDiscount(resultSet.getDouble("light_discount"))
                    .disabledDiscount(resultSet.getDouble("disabled_discount"))
                    .subscribedFee(resultSet.getInt("subscribed_fee"))
                    .maxDailyFee(resultSet.getInt("max_daily_fee"))
                    .isActive(resultSet.getBoolean("is_active"))
                    .modifyDate(ts != null ? ts.toLocalDateTime() : null)
                    .build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // [단건 조회] 요금 정책 상세 출력
    public FeePolicyVO selectOnePolicy() {
        String sql = "SELECT * FROM fee_policy WHERE is_active = true ORDER BY modify_date DESC LIMIT 1";

        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) return null;

            Timestamp ts = resultSet.getTimestamp("modify_date");
            return FeePolicyVO.builder()
                    .policyId(resultSet.getInt("policy_id"))
                    .gracePeriod(resultSet.getInt("grace_period"))
                    .defaultTime(resultSet.getInt("default_time"))
                    .defaultFee(resultSet.getInt("default_fee"))
                    .extraTime(resultSet.getInt("extra_time"))
                    .extraFee(resultSet.getInt("extra_fee"))
                    .lightDiscount(resultSet.getDouble("light_discount"))
                    .disabledDiscount(resultSet.getDouble("disabled_discount"))
                    .subscribedFee(resultSet.getInt("subscribed_fee"))
                    .maxDailyFee(resultSet.getInt("max_daily_fee"))
                    .isActive(resultSet.getBoolean("is_active"))
                    .modifyDate(ts != null ? ts.toLocalDateTime() : null)
                    .build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 월정액 요금 조회
    public Integer getSubscribedFee(Connection connection) throws SQLException {
        String sql = "SELECT subscribed_fee FROM fee_policy LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet rs = preparedStatement.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("subscribed_fee");
            }

            // 기본값 반환
            log.warn("fee_policy 테이블에 데이터 없음. 기본값 100000 반환");
            return 100000;
        }
    }

    // 현재 활성화된 정책 모두 false로 변경
    public int deactivateAllPolicies() {
        String sql = "UPDATE fee_policy SET is_active = false WHERE is_active = true";

        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement ps = connection.prepareStatement(sql);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 특정 ID의 정책을 활성화(true)로 변경하는 메서드
    public int activatePolicy(int id) {
        String sql = "UPDATE fee_policy SET is_active = true WHERE policy_id = ?";

        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, id);

            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
