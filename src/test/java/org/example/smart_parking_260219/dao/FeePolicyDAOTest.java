package org.example.smart_parking_260219.dao;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.util.AppConfig;
import org.example.smart_parking_260219.vo.FeePolicyVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 테스트 DB에 요금 정책을 추가하고 조회하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class FeePolicyDAOTest {

    private final FeePolicyDAO feePolicyDAO = FeePolicyDAO.getInstance();
    // 각 테스트에서 만든 정책을 찾고 정리하는 데 사용함
    private Integer testDefaultFee;

    @BeforeEach
    void setUp() throws SQLException {
        // IDE에서 직접 실행해도 개발 DB에 데이터를 넣지 않도록 확인함
        assertEquals("jdbc:mariadb://localhost:3308/smart_parking_test", AppConfig.get("db.url"));
        assertEquals("parking_test", AppConfig.get("db.username"));

        testDefaultFee = findUnusedDefaultFee();
        log.info("--- 테스트 요금 정책 식별값 준비: defaultFee={} ---", testDefaultFee);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testDefaultFee == null) {
            return;
        }

        // 이번 테스트에서 사용한 기본 요금으로 해당 정책만 삭제함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM fee_policy WHERE default_fee = ?")) {
            statement.setInt(1, testDefaultFee);
            int deletedCount = statement.executeUpdate();
            log.info("--- 테스트 요금 정책 정리: defaultFee={}, 삭제 건수={} ---",
                    testDefaultFee, deletedCount);
        }
    }

    @Test
    void insertPolicyTest() {
        FeePolicyVO policy = FeePolicyVO.builder()
                .gracePeriod(12)
                .defaultTime(45)
                .defaultFee(testDefaultFee)
                .extraTime(15)
                .extraFee(750)
                .lightDiscount(0.25)
                .disabledDiscount(0.4)
                .subscribedFee(98000)
                .maxDailyFee(18000)
                .isActive(false)
                .build();

        feePolicyDAO.insertPolicy(policy);
        FeePolicyVO inserted = feePolicyDAO.selectAllPolicies().stream()
                .filter(item -> item.getDefaultFee() == testDefaultFee)
                .findFirst()
                .orElse(null);

        log.info("--- 요금 정책 추가 결과: {} ---", inserted);
        assertNotNull(inserted);
        assertEquals(12, inserted.getGracePeriod());
        assertEquals(45, inserted.getDefaultTime());
        assertEquals(testDefaultFee.intValue(), inserted.getDefaultFee());
        assertEquals(15, inserted.getExtraTime());
        assertEquals(750, inserted.getExtraFee());
        assertEquals(0.25, inserted.getLightDiscount(), 0.000001);
        assertEquals(0.4, inserted.getDisabledDiscount(), 0.000001);
        assertEquals(98000, inserted.getSubscribedFee());
        assertEquals(18000, inserted.getMaxDailyFee());
        assertFalse(inserted.isActive());
        assertNotNull(inserted.getModifyDate());
    }

    @Test
    void selectAllPoliciesTest() throws SQLException {
        // 조회 대상 정책은 검증할 DAO 메서드와 별개로 직접 준비함
        insertTestPolicy(false, LocalDateTime.now());
        List<FeePolicyVO> policies = feePolicyDAO.selectAllPolicies();
        FeePolicyVO found = policies.stream()
                .filter(policy -> policy.getDefaultFee() == testDefaultFee)
                .findFirst()
                .orElse(null);

        log.info("--- 전체 요금 정책 조회: 총 {}개, 테스트 정책={} ---", policies.size(), found);
        assertNotNull(found);
        assertEquals(testDefaultFee.intValue(), found.getDefaultFee());
        assertFalse(found.isActive());
    }

    @Test
    void selectOnePolicyTest() throws SQLException {
        // 테스트 정책이 최신 활성 정책이 되도록 수정 시각을 미래로 지정함
        insertTestPolicy(true, LocalDateTime.now().plusYears(1));
        FeePolicyVO found = feePolicyDAO.selectOnePolicy();

        log.info("--- 최신 활성 요금 정책 조회: {} ---", found);
        assertNotNull(found);
        assertEquals(testDefaultFee.intValue(), found.getDefaultFee());
        assertTrue(found.isActive());
    }

    // 다른 정책과 겹치지 않는 기본 요금을 테스트 정책의 식별값으로 사용함
    private int findUnusedDefaultFee() throws SQLException {
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM fee_policy WHERE default_fee = ? LIMIT 1")) {
            while (true) {
                int candidate = ThreadLocalRandom.current().nextInt(1_000_000, 2_000_000_000);
                statement.setInt(1, candidate);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return candidate;
                    }
                }
            }
        }
    }

    // 조회 테스트에 사용할 정책을 SQL로 직접 준비함
    private void insertTestPolicy(boolean active, LocalDateTime modifyDate) throws SQLException {
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO fee_policy (default_fee, is_active, modify_date) VALUES (?, ?, ?)")) {
            statement.setInt(1, testDefaultFee);
            statement.setBoolean(2, active);
            statement.setTimestamp(3, Timestamp.valueOf(modifyDate));
            assertEquals(1, statement.executeUpdate());
        }
    }
}
