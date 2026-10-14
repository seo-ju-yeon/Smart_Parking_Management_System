package org.example.smart_parking_260219.dao;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.util.AppConfig;
import org.example.smart_parking_260219.util.PasswordUtil;
import org.example.smart_parking_260219.vo.ManagerRole;
import org.example.smart_parking_260219.vo.ManagerVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 테스트 DB에 관리자를 추가하고 조회, 변경하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class ManagerDAOTest {

    private static final String TEST_PASSWORD = "test-password-123";
    private final ManagerDAO managerDAO = ManagerDAO.getInstance();
    private String testManagerId;
    private boolean managerCreated;

    @BeforeEach
    void setUp() {
        // IDE에서 직접 실행해도 개발 DB에 데이터를 넣지 않도록 확인함
        assertEquals("jdbc:mariadb://localhost:3308/smart_parking_test", AppConfig.get("db.url"));
        assertEquals("parking_test", AppConfig.get("db.username"));

        testManagerId = "it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (!managerCreated) {
            return;
        }

        // 이번 테스트에서 추가한 관리자만 삭제함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM manager WHERE manager_id = ?")) {
            statement.setString(1, testManagerId);
            int deletedCount = statement.executeUpdate();
            log.info("--- 테스트 관리자 정리: managerId={}, 삭제 건수={} ---", testManagerId, deletedCount);
            assertEquals(1, deletedCount);
        }
    }

    @Test
    void insertManagerTest() {
        ManagerVO manager = ManagerVO.builder()
                .managerId(testManagerId)
                .managerName("테스트관리자")
                .password(TEST_PASSWORD)
                .email(testManagerId + "@smartparking.local")
                .build();

        managerDAO.insertManager(manager);
        managerCreated = true;
        ManagerVO inserted = managerDAO.selectOne(testManagerId);

        log.info("--- 관리자 등록 결과: {} ---", inserted);
        assertNotNull(inserted);
        assertEquals(testManagerId, inserted.getManagerId());
        assertEquals("테스트관리자", inserted.getManagerName());
        assertTrue(PasswordUtil.checkPassword(TEST_PASSWORD, inserted.getPassword()));
        assertTrue(inserted.isActive());
        assertEquals(ManagerRole.NORMAL, inserted.getRole());
    }

    @Test
    void selectOneTest() throws SQLException {
        insertTestManager();
        ManagerVO found = managerDAO.selectOne(testManagerId);

        log.info("--- 관리자 ID 조회 결과: managerId={}, 관리자={} ---", testManagerId, found);
        assertNotNull(found);
        assertEquals(testManagerId, found.getManagerId());
        assertEquals("테스트관리자", found.getManagerName());
        assertTrue(found.isActive());
        assertEquals(ManagerRole.NORMAL, found.getRole());
    }

    @Test
    void selectAllTest() throws SQLException {
        insertTestManager();
        List<ManagerVO> managers = managerDAO.selectAll();
        ManagerVO found = managers.stream()
                .filter(manager -> testManagerId.equals(manager.getManagerId()))
                .findFirst()
                .orElse(null);

        log.info("--- 전체 관리자 조회: 총 {}명, 테스트 관리자={} ---", managers.size(), found);
        assertNotNull(found);
        assertEquals(testManagerId, found.getManagerId());
    }

    @Test
    void updateActiveTest() throws SQLException {
        insertTestManager();
        ManagerVO before = managerDAO.selectOne(testManagerId);
        assertNotNull(before);
        assertTrue(before.isActive());

        managerDAO.updateActive(false, testManagerId);
        ManagerVO updated = managerDAO.selectOne(testManagerId);

        log.info("--- 관리자 활성화 상태 변경 결과: managerId={}, 관리자={} ---", testManagerId, updated);
        assertNotNull(updated);
        assertFalse(updated.isActive());
    }

    // 조회, 변경 테스트가 검증할 관리자는 DAO 메서드와 별개로 직접 준비함
    private void insertTestManager() throws SQLException {
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO manager (manager_id, manager_name, password, email) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, testManagerId);
            statement.setString(2, "테스트관리자");
            statement.setString(3, PasswordUtil.hashPassword(TEST_PASSWORD));
            statement.setString(4, testManagerId + "@smartparking.local");
            assertEquals(1, statement.executeUpdate());
        }

        managerCreated = true;
        log.info("--- 테스트 관리자 생성: managerId={} ---", testManagerId);
    }
}
