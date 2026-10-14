package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.dto.FeePolicyDTO;
import org.example.smart_parking_260219.util.AppConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 테스트 DB에 요금 정책을 추가하고 활성 상태를 바꾸므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class FeePolicyServiceTest {

    private final FeePolicyService feePolicyService = FeePolicyService.getInstance();
    private final Set<Integer> originalActivePolicyIds = new HashSet<>();
    private Integer testDefaultFee;

    @BeforeEach
    void setUp() throws SQLException {
        // IDE에서 직접 실행해도 개발 DB의 정책을 변경하지 않도록 확인함
        assertEquals("jdbc:mariadb://localhost:3308/smart_parking_test", AppConfig.get("db.url"));
        assertEquals("parking_test", AppConfig.get("db.username"));

        // 정책 등록과 적용은 기존 정책을 비활성화하므로 원래 활성 정책을 기억함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT policy_id FROM fee_policy WHERE is_active = true");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                originalActivePolicyIds.add(resultSet.getInt("policy_id"));
            }
        }

        testDefaultFee = findUnusedDefaultFee();
        log.info("--- 요금 정책 서비스 테스트 준비: defaultFee={}, 기존 활성 정책={} ---",
                testDefaultFee, originalActivePolicyIds);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testDefaultFee == null) {
            return;
        }

        // 이번 테스트의 정책을 삭제하고 기존 활성 정책을 하나의 트랜잭션에서 복원함
        try (Connection connection = DBConnection.INSTANCE.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deletePolicy = connection.prepareStatement(
                    "DELETE FROM fee_policy WHERE default_fee = ?");
                 PreparedStatement deactivate = connection.prepareStatement(
                         "UPDATE fee_policy SET is_active = false WHERE is_active = true");
                 PreparedStatement restore = connection.prepareStatement(
                         "UPDATE fee_policy SET is_active = true WHERE policy_id = ?")) {
                deletePolicy.setInt(1, testDefaultFee);
                int deletedCount = deletePolicy.executeUpdate();
                deactivate.executeUpdate();

                for (int policyId : originalActivePolicyIds) {
                    restore.setInt(1, policyId);
                    restore.executeUpdate();
                }

                connection.commit();
                log.info("--- 요금 정책 서비스 테스트 정리: defaultFee={}, 삭제 {}건, 활성 정책 복원={} ---",
                        testDefaultFee, deletedCount, originalActivePolicyIds);
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }

        assertEquals(originalActivePolicyIds, readActivePolicyIds());
    }

    @Test
    void addPolicyTest() throws SQLException {
        FeePolicyDTO input = FeePolicyDTO.builder()
                .gracePeriod(20)
                .defaultTime(10)
                .defaultFee(testDefaultFee)
                .extraTime(30)
                .extraFee(1000)
                .lightDiscount(30)
                .disabledDiscount(50)
                .subscribedFee(100000)
                .maxDailyFee(15000)
                .build();

        feePolicyService.addPolicy(input);

        FeePolicyDTO inserted = feePolicyService.getPolicyList().stream()
                .filter(policy -> policy.getDefaultFee() == testDefaultFee)
                .findFirst()
                .orElse(null);

        log.info("--- 요금 정책 등록 결과: {} ---", inserted);
        assertNotNull(inserted);
        assertEquals(20, inserted.getGracePeriod());
        assertEquals(10, inserted.getDefaultTime());
        assertEquals(30, inserted.getExtraTime());
        assertEquals(1000, inserted.getExtraFee());
        assertEquals(0.3, inserted.getLightDiscount(), 0.000001);
        assertEquals(0.5, inserted.getDisabledDiscount(), 0.000001);
        assertEquals(100000, inserted.getSubscribedFee());
        assertEquals(15000, inserted.getMaxDailyFee());
        assertTrue(inserted.isActive());
        assertEquals(Set.of(inserted.getPolicyId()), readActivePolicyIds());
    }

    @Test
    void getPolicyListTest() throws SQLException {
        int policyId = insertTestPolicy();

        List<FeePolicyDTO> policies = feePolicyService.getPolicyList();
        FeePolicyDTO found = policies.stream()
                .filter(policy -> policy.getPolicyId() == policyId)
                .findFirst()
                .orElse(null);

        log.info("--- 요금 정책 목록 조회 결과: 전체 {}건, 테스트 정책={} ---", policies.size(), found);
        assertNotNull(found);
        assertEquals(testDefaultFee.intValue(), found.getDefaultFee());
        assertFalse(found.isActive());
    }

    @Test
    void getPolicyTest() throws SQLException {
        int policyId = insertTestPolicy();
        activateOnlyTestPolicy(policyId);

        FeePolicyDTO found = feePolicyService.getPolicy();

        log.info("--- 활성 요금 정책 조회 결과: {} ---", found);
        assertNotNull(found);
        assertEquals(policyId, found.getPolicyId());
        assertEquals(testDefaultFee.intValue(), found.getDefaultFee());
        assertTrue(found.isActive());
        assertEquals(Set.of(policyId), readActivePolicyIds());
    }

    @Test
    void getPolicyByIdTest() throws Exception {
        int policyId = insertTestPolicy();

        FeePolicyDTO found = feePolicyService.getPolicyById(policyId);

        log.info("--- 요금 정책 ID 조회 결과: policyId={}, 정책={} ---", policyId, found);
        assertNotNull(found);
        assertEquals(policyId, found.getPolicyId());
        assertEquals(testDefaultFee.intValue(), found.getDefaultFee());
        assertFalse(found.isActive());
        assertNull(feePolicyService.getPolicyById(-1));
    }

    @Test
    void applyPolicyTest() throws Exception {
        int policyId = insertTestPolicy();

        feePolicyService.applyPolicy(policyId);

        FeePolicyDTO applied = feePolicyService.getPolicyById(policyId);
        Set<Integer> activePolicyIds = readActivePolicyIds();

        log.info("--- 요금 정책 적용 결과: policyId={}, 활성 정책={} ---", policyId, activePolicyIds);
        assertNotNull(applied);
        assertTrue(applied.isActive());
        assertEquals(Set.of(policyId), activePolicyIds);
        assertEquals(policyId, feePolicyService.getPolicy().getPolicyId());
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

    // 조회 테스트가 사용할 정책은 서비스 등록 메서드와 별개로 직접 준비함
    private int insertTestPolicy() throws SQLException {
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO fee_policy (default_fee, is_active) VALUES (?, false)",
                     Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, testDefaultFee);
            assertEquals(1, statement.executeUpdate());

            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getInt(1);
            }
        }
    }

    // 활성 정책 조회를 검증하기 위해 이번 테스트에서 만든 정책만 활성화함
    private void activateOnlyTestPolicy(int policyId) throws SQLException {
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE fee_policy SET is_active = "
                             + "CASE WHEN policy_id = ? THEN true ELSE false END")) {
            statement.setInt(1, policyId);
            statement.executeUpdate();
        }
    }

    // 현재 활성화된 요금 정책 ID를 조회해 정책 적용 결과와 테스트 종료 후 복원 상태를 확인함
    private Set<Integer> readActivePolicyIds() throws SQLException {
        Set<Integer> activePolicyIds = new HashSet<>();
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT policy_id FROM fee_policy WHERE is_active = true");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                activePolicyIds.add(resultSet.getInt("policy_id"));
            }
        }
        return activePolicyIds;
    }
}
