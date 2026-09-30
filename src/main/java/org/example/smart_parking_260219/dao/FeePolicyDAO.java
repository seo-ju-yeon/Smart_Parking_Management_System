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

    // 새로운 요금 정책을 저장함
    public void insertPolicy(FeePolicyVO feePolicyVo) {
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

    // 요금 정책 목록을 최근 등록 순으로 조회함
    public List<FeePolicyVO> selectAllPolicies() {
        List<FeePolicyVO> list = new ArrayList<>();

        String sql = "SELECT * FROM fee_policy ORDER BY policy_id DESC";

        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                list.add(mapFeePolicy(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // 정책 ID로 단일 요금 정책을 조회함
    public FeePolicyVO selectPolicyById(int id) {
        String sql = "SELECT * FROM fee_policy WHERE policy_id = ?";

        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, id);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                return null;
            }

            return mapFeePolicy(resultSet);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 독립 조회에서는 DAO가 Connection을 생성하고 반환함
    public FeePolicyVO selectOnePolicy() {
        try (Connection connection = DBConnection.INSTANCE.getConnection()) {
            return selectActivePolicy(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 트랜잭션 Service가 전달한 Connection으로 최신 활성 정책을 조회함
    public FeePolicyVO selectActivePolicy(
            Connection connection
    ) throws SQLException {
        String sql = "SELECT * FROM fee_policy "
                + "WHERE is_active = true "
                + "ORDER BY modify_date DESC LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            if (!resultSet.next()) {
                return null;
            }

            return mapFeePolicy(resultSet);
        }
    }

    // 전달받은 Connection으로 월정액 요금을 조회함
    public Integer getSubscribedFee(Connection connection) throws SQLException {
        String sql = "SELECT subscribed_fee FROM fee_policy LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet rs = preparedStatement.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("subscribed_fee");
            }

            // 정책이 없으면 기존 기본 월정액 요금을 사용함
            log.warn("fee_policy 테이블에 데이터 없음. 기본값 100000 반환");
            return 100000;
        }
    }

    // 현재 활성화된 모든 정책을 비활성화함
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

    // 정책 ID로 지정한 정책을 활성화함
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

    // 모든 정책 조회가 동일한 필드를 반환하도록 매핑을 한 곳에서 관리함
    private FeePolicyVO mapFeePolicy(
            ResultSet resultSet
    ) throws SQLException {
        Timestamp modifyTimestamp = resultSet.getTimestamp("modify_date");

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
                .modifyDate(modifyTimestamp == null ? null : modifyTimestamp.toLocalDateTime())
                .build();
    }
}
