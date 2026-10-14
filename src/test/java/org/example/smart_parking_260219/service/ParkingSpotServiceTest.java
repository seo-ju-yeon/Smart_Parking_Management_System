package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.dto.ParkingSpotDTO;
import org.example.smart_parking_260219.util.AppConfig;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 설정된 DB의 주차 공간을 조회하고 입차 상태를 변경하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class ParkingSpotServiceTest {

    private final ParkingSpotService parkingSpotService = ParkingSpotService.INSTANCE;
    private String testSpaceId;

    @BeforeEach
    void setUp() throws SQLException {
        // IDE에서 직접 실행해도 개발 DB에 데이터를 넣지 않도록 확인함
        assertEquals("jdbc:mariadb://localhost:3308/smart_parking_test",
                AppConfig.get("db.url"));
        assertEquals("parking_test", AppConfig.get("db.username"));

        // 테스트마다 고유한 주차면을 만들어 기존 주차면을 변경하지 않음
        String newSpaceId = "T" + UUID.randomUUID().toString().substring(0, 4);

        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO parking_spot (space_id) VALUES (?)")) {
            statement.setString(1, newSpaceId);
            assertEquals(1, statement.executeUpdate());
        }

        testSpaceId = newSpaceId;
        log.info("--- 서비스 테스트 주차면 준비: spaceId={} ---", testSpaceId);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (testSpaceId == null) {
            return;
        }

        // 이번 테스트에서 만든 주차면만 삭제함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM parking_spot WHERE space_id = ?")) {
            statement.setString(1, testSpaceId);
            int deletedCount = statement.executeUpdate();

            log.info("--- 서비스 테스트 주차면 정리: spaceId={}, 삭제 {}건 ---", testSpaceId, deletedCount);
        }
    }

    @Test
    void getAllParkingSpotTest() {
        List<ParkingSpotDTO> spots = parkingSpotService.getAllParkingSpot();
        ParkingSpotDTO found = spots.stream()
                .filter(spot -> testSpaceId.equals(spot.getSpaceId()))
                .findFirst()
                .orElse(null);

        log.info("--- 전체 주차면 조회 결과: spaceId={}, 주차면={} ---", testSpaceId, found);
        assertNotNull(found);
        assertEquals(Boolean.TRUE, found.getEmpty());
        assertNull(found.getCarNum());
    }

    @Test
    void getEmptyParkingSpotTest() {
        List<ParkingSpotDTO> emptySpots = parkingSpotService.getEmptyParkingSpot();
        boolean containsTestSpot = emptySpots.stream()
                .anyMatch(spot -> testSpaceId.equals(spot.getSpaceId()));

        log.info("--- 빈 주차면 조회 결과: spaceId={}, 포함 여부={} ---", testSpaceId, containsTestSpot);
        assertTrue(containsTestSpot);
        assertTrue(emptySpots.stream()
                .allMatch(spot -> Boolean.TRUE.equals(spot.getEmpty())));
    }

    @Test
    void modifyInputParkingSpotTest() {
        String carNum = "TEST1234";
        ParkingSpotDTO input = ParkingSpotDTO.builder()
                .spaceId(testSpaceId)
                .carNum(carNum)
                .build();

        parkingSpotService.modifyInputParkingSpot(input);

        ParkingSpotDTO updated =
                parkingSpotService.getParkingSpotBySpaceId(testSpaceId);

        log.info("--- 입차 상태 변경 결과: spaceId={}, 주차면={} ---", testSpaceId, updated);
        assertNotNull(updated);
        assertEquals(Boolean.FALSE, updated.getEmpty());
        assertEquals(carNum, updated.getCarNum());
        assertTrue(parkingSpotService.getEmptyParkingSpot().stream()
                .noneMatch(spot -> testSpaceId.equals(spot.getSpaceId())));
    }

    @Test
    void getParkingSpotBySpaceIdTest() {
        ParkingSpotDTO found =
                parkingSpotService.getParkingSpotBySpaceId(testSpaceId);

        log.info("--- 주차면 번호 조회 결과: spaceId={}, 주차면={} ---", testSpaceId, found);
        assertNotNull(found);
        assertEquals(testSpaceId, found.getSpaceId());
        assertNull(parkingSpotService.getParkingSpotBySpaceId(null));
    }
}
