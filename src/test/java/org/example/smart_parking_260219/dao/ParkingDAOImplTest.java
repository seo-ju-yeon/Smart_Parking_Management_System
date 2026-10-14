package org.example.smart_parking_260219.dao;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.util.AppConfig;
import org.example.smart_parking_260219.vo.ParkingVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// 설정된 DB에 주차 기록을 추가하고 조회하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class ParkingDAOImplTest {

    private ParkingDAO parkingDAO;
    // 데이터 생성과 검증, 정리에서 같은 임시 주차면과 차량번호를 사용함
    private String testSpaceId;
    private String testCarNum;

    @BeforeEach
    void setUp() throws SQLException {
        // IDE에서 직접 실행해도 개발 DB에 데이터를 넣지 않도록 확인함
        assertEquals("jdbc:mariadb://localhost:3308/smart_parking_test", AppConfig.get("db.url"));
        assertEquals("parking_test", AppConfig.get("db.username"));

        parkingDAO = new ParkingDAOImpl();
        String newSpaceId = "T" + UUID.randomUUID().toString().substring(0, 4);

        // 주차 기록의 외래 키가 참조할 임시 주차면을 먼저 만듦
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO parking_spot (space_id) VALUES (?)")) {
            statement.setString(1, newSpaceId);
            statement.executeUpdate();
        }

        testSpaceId = newSpaceId;
        testCarNum = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        log.info("--- 테스트 데이터 준비: spaceId={}, carNum={} ---", testSpaceId, testCarNum);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testSpaceId == null) {
            return;
        }

        // 주차 기록을 먼저 삭제한 뒤 참조 대상인 주차면을 삭제함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement parkingStatement = connection.prepareStatement(
                     "DELETE FROM parking WHERE space_id = ?");
             PreparedStatement spotStatement = connection.prepareStatement(
                     "DELETE FROM parking_spot WHERE space_id = ?")) {
            parkingStatement.setString(1, testSpaceId);
            int deletedParkingCount = parkingStatement.executeUpdate();

            spotStatement.setString(1, testSpaceId);
            int deletedSpotCount = spotStatement.executeUpdate();

            log.info("--- 테스트 데이터 정리: spaceId={}, 주차 기록 {}건, 주차면 {}건 ---",
                    testSpaceId, deletedParkingCount, deletedSpotCount);
        }
    }

    @Test
    void insertParkingTest() {
        ParkingVO parking = ParkingVO.builder()
                .spaceId(testSpaceId)
                .carNum(testCarNum)
                .carType(1)
                .build();

        parkingDAO.insertParking(parking);
        ParkingVO inserted = parkingDAO.selectParkingByCarNum(testCarNum);

        log.info("--- 주차 기록 추가 결과: {} ---", inserted);
        assertNotNull(inserted);
        assertEquals(testSpaceId, inserted.getSpaceId());
        assertEquals(testCarNum, inserted.getCarNum());
        assertEquals(1, inserted.getCarType());
        assertFalse(inserted.isPaid());
    }

    @Test
    void selectParkingByLast4Test() throws SQLException {
        int parkingId = insertTestParkingRecord();
        String last4 = testCarNum.substring(testCarNum.length() - 4);

        ParkingVO found = parkingDAO.selectParkingByLast4(last4);

        log.info("--- 차량번호 뒤 4자리 조회 결과: last4={}, 주차 기록={} ---", last4, found);
        assertNotNull(found);
        assertEquals(parkingId, found.getParkingId());
        assertEquals(testCarNum, found.getCarNum());
        assertEquals(testSpaceId, found.getSpaceId());
    }

    @Test
    void selectParkingByIdTest() throws SQLException {
        int parkingId = insertTestParkingRecord();

        ParkingVO found = parkingDAO.selectParkingByParkingId(parkingId);

        log.info("--- 주차 기록 ID 조회 결과: parkingId={}, 주차 기록={} ---", parkingId, found);
        assertNotNull(found);
        assertEquals(parkingId, found.getParkingId());
        assertEquals(testCarNum, found.getCarNum());
        assertEquals(testSpaceId, found.getSpaceId());
    }

    // 조회 테스트가 검증할 기록은 DAO 메서드와 별개로 직접 준비함
    private int insertTestParkingRecord() throws SQLException {
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO parking (space_id, car_num, car_type) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, testSpaceId);
            statement.setString(2, testCarNum);
            statement.setInt(3, 1);
            assertEquals(1, statement.executeUpdate());

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                assertTrue(generatedKeys.next());
                return generatedKeys.getInt(1);
            }
        }
    }
}
