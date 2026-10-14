package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.dao.ManagerDAO;
import org.example.smart_parking_260219.dto.ManagerDTO;
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

// 테스트 DB에 관리자를 추가하고 인증과 목록 조회를 확인하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class ManagerServiceTest {

    private static final String TEST_PASSWORD = "test-password-123";
    private final ManagerService managerService = ManagerService.INSTANCE;
    private final ManagerDAO managerDAO = ManagerDAO.getInstance();
    private String testManagerId;

    @BeforeEach
    void setUp() {
        // IDE에서 직접 실행해도 개발 DB에 관리자를 추가하지 않도록 확인함
        assertEquals("jdbc:mariadb://localhost:3308/smart_parking_test", AppConfig.get("db.url"));
        assertEquals("parking_test", AppConfig.get("db.username"));

        testManagerId = "it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        log.info("--- 관리자 서비스 테스트 준비: managerId={} ---", testManagerId);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testManagerId == null) {
            return;
        }

        // 이번 테스트에서 사용한 관리자 ID만 삭제함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM manager WHERE manager_id = ?")) {
            statement.setString(1, testManagerId);
            int deletedCount = statement.executeUpdate();
            log.info("--- 관리자 서비스 테스트 정리: managerId={}, 삭제 {}건 ---",
                    testManagerId, deletedCount);
        }
    }

    @Test
    void addManagerTest() {
        ManagerDTO input = ManagerDTO.builder()
                .managerId(testManagerId)
                .managerName("테스트관리자")
                .password(TEST_PASSWORD)
                .email(testManagerId + "@smartparking.local")
                .build();

        managerService.addManager(input);
        ManagerVO saved = managerDAO.selectOne(testManagerId);

        log.info("--- 관리자 등록 결과: managerId={}, 관리자={} ---", testManagerId, saved);
        assertNotNull(saved);
        assertEquals(testManagerId, saved.getManagerId());
        assertEquals("테스트관리자", saved.getManagerName());
        assertEquals(testManagerId + "@smartparking.local", saved.getEmail());
        assertTrue(PasswordUtil.checkPassword(TEST_PASSWORD, saved.getPassword()));
        assertTrue(saved.isActive());
        assertEquals(ManagerRole.NORMAL, saved.getRole());
        assertTrue(managerService.isAuth(testManagerId, TEST_PASSWORD));
    }

    @Test
    void isAuthSuccessTest() throws SQLException {
        insertTestManager(true);

        boolean authenticated = managerService.isAuth(testManagerId, TEST_PASSWORD);

        log.info("--- 활성 관리자 로그인 결과: managerId={}, 인증={} ---", testManagerId, authenticated);
        assertTrue(authenticated);
    }

    @Test
    void isAuthWrongPasswordTest() throws SQLException {
        insertTestManager(true);

        boolean authenticated = managerService.isAuth(testManagerId, "wrong-password");

        log.info("--- 잘못된 비밀번호 로그인 결과: managerId={}, 인증={} ---",
                testManagerId, authenticated);
        assertFalse(authenticated);
    }

    @Test
    void isAuthUnknownIdTest() {
        // 생성하지 않은 임시 ID로 존재하지 않는 관리자 인증을 확인함
        boolean authenticated = managerService.isAuth(testManagerId, TEST_PASSWORD);

        log.info("--- 존재하지 않는 관리자 로그인 결과: managerId={}, 인증={} ---",
                testManagerId, authenticated);
        assertFalse(authenticated);
    }

    @Test
    void isAuthInactiveAccountTest() throws SQLException {
        insertTestManager(false);

        boolean authenticated = managerService.isAuth(testManagerId, TEST_PASSWORD);

        log.info("--- 비활성 관리자 로그인 결과: managerId={}, 인증={} ---", testManagerId, authenticated);
        assertFalse(authenticated);
    }

    @Test
    void getAllManagersTest() throws SQLException {
        insertTestManager(true);

        List<ManagerDTO> managers = managerService.getAllManagers();
        ManagerDTO found = managers.stream()
                .filter(manager -> testManagerId.equals(manager.getManagerId()))
                .findFirst()
                .orElse(null);

        log.info("--- 관리자 목록 조회 결과: 전체 {}명, 테스트 관리자={} ---", managers.size(), found);
        assertNotNull(found);
        assertEquals(testManagerId, found.getManagerId());
        assertEquals("테스트관리자", found.getManagerName());
        assertTrue(found.isActive());
        assertEquals(ManagerRole.NORMAL, found.getRole());
    }

    // 인증과 목록 조회 테스트가 사용할 관리자는 서비스 등록 메서드와 별개로 직접 준비함
    private void insertTestManager(boolean active) throws SQLException {
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO manager (manager_id, manager_name, password, email, active) "
                             + "VALUES (?, ?, ?, ?, ?)")) {
            statement.setString(1, testManagerId);
            statement.setString(2, "테스트관리자");
            statement.setString(3, PasswordUtil.hashPassword(TEST_PASSWORD));
            statement.setString(4, testManagerId + "@smartparking.local");
            statement.setBoolean(5, active);
            assertEquals(1, statement.executeUpdate());
        }
    }
}
