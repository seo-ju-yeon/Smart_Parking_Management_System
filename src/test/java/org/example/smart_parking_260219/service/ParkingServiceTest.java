package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.dto.ParkingDTO;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 테스트 DB에 주차 기록을 추가하고 조회하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class ParkingServiceTest {

    private final ParkingService parkingService = ParkingService.INSTANCE;
    private String testSpaceId;
    private String testCarNum;

    @BeforeEach
    void setUp() throws SQLException {
        // IDE에서 직접 실행해도 개발 DB에 데이터를 넣지 않도록 확인함
        assertEquals("jdbc:mariadb://localhost:3308/smart_parking_test",
                AppConfig.get("db.url"));
        assertEquals("parking_test", AppConfig.get("db.username"));

        String newSpaceId = "T" + UUID.randomUUID().toString().substring(0, 4);

        // 주차 기록의 외래 키가 참조할 임시 주차면을 준비함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO parking_spot (space_id) VALUES (?)")) {
            statement.setString(1, newSpaceId);
            assertEquals(1, statement.executeUpdate());
        }

        testSpaceId = newSpaceId;
        testCarNum = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        log.info("--- 주차 서비스 테스트 데이터 준비: spaceId={}, carNum={} ---", testSpaceId, testCarNum);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testSpaceId == null) {
            return;
        }

        // 주차 기록을 먼저 삭제한 뒤 참조 대상인 주차면을 삭제함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement parking = connection.prepareStatement(
                     "DELETE FROM parking WHERE space_id = ?");
             PreparedStatement spot = connection.prepareStatement(
                     "DELETE FROM parking_spot WHERE space_id = ?")) {
            parking.setString(1, testSpaceId);
            int deletedParkingCount = parking.executeUpdate();
            spot.setString(1, testSpaceId);
            int deletedSpotCount = spot.executeUpdate();

            log.info("--- 주차 서비스 테스트 데이터 정리: spaceId={}, 주차 기록 {}건, 주차면 {}건 ---",
                    testSpaceId, deletedParkingCount, deletedSpotCount);
        }
    }

    @Test
    void addParkingTest() throws SQLException {
        ParkingDTO input = ParkingDTO.builder()
                .spaceId(testSpaceId)
                .carNum(testCarNum)
                .carType(3)
                .build();

        parkingService.addParking(input);

        // 서비스가 저장한 값은 DB에서 직접 조회해 확인함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM parking WHERE space_id = ? AND car_num = ?")) {
            statement.setString(1, testSpaceId);
            statement.setString(2, testCarNum);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                log.info("--- 주차 기록 등록 결과: parkingId={}, carNum={} ---",
                        result.getInt("parking_id"), result.getString("car_num"));
                assertEquals(3, result.getInt("car_type"));
                assertNotNull(result.getTimestamp("entry_time"));
                assertFalse(result.getBoolean("paid"));
                assertFalse(result.next());
            }
        }
    }

    @Test
    void getParkingTest() throws SQLException {
        int parkingId = insertTestParkingRecord();
        String last4 = testCarNum.substring(testCarNum.length() - 4);

        ParkingDTO found = parkingService.getParking(last4);

        log.info("--- 차량번호 뒤 4자리 주차 조회 결과: last4={}, 주차 기록={} ---", last4, found);
        assertNotNull(found);
        assertEquals(parkingId, found.getParkingId());
        assertEquals(testCarNum, found.getCarNum());
        assertEquals(testSpaceId, found.getSpaceId());
        assertNotNull(found.getEntryTime());
    }

    @Test
    void getByIdParkingTest() throws SQLException {
        int parkingId = insertTestParkingRecord();

        ParkingDTO found = parkingService.getByIdParking(parkingId);

        log.info("--- 주차 기록 ID 조회 결과: parkingId={}, 주차 기록={} ---", parkingId, found);
        assertNotNull(found);
        assertEquals(parkingId, found.getParkingId());
        assertEquals(testCarNum, found.getCarNum());
        assertEquals(3, found.getCarType());
        assertNull(parkingService.getByIdParking(-1));
    }

    @Test
    void getAllParkingTest() throws SQLException {
        int parkingId = insertTestParkingRecord();

        List<ParkingDTO> allParking = parkingService.getAllParking();
        ParkingDTO found = allParking.stream()
                .filter(parking -> parking.getParkingId() == parkingId)
                .findFirst()
                .orElse(null);

        log.info("--- 전체 주차 기록 조회 결과: parkingId={}, 주차 기록={} ---", parkingId, found);
        assertNotNull(found);
        assertEquals(testCarNum, found.getCarNum());
        assertEquals(testSpaceId, found.getSpaceId());
    }

    @Test
    void getParkingByCarNumTest() throws SQLException {
        int parkingId = insertTestParkingRecord();

        ParkingDTO found = parkingService.getParkingByCarNum(testCarNum);

        log.info("--- 차량번호 주차 조회 결과: carNum={}, 주차 기록={} ---", testCarNum, found);
        assertNotNull(found);
        assertEquals(parkingId, found.getParkingId());
        assertEquals(3, found.getCarType());
        assertFalse(found.isPaid());
        assertNull(parkingService.getParkingByCarNum(null));
    }

    // 조회 테스트가 사용할 주차 기록은 서비스 등록 메서드와 별개로 직접 준비함
    private int insertTestParkingRecord() throws SQLException {
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO parking (space_id, car_num, car_type) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, testSpaceId);
            statement.setString(2, testCarNum);
            statement.setInt(3, 3);
            assertEquals(1, statement.executeUpdate());

            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getInt(1);
            }
        }
    }
}
