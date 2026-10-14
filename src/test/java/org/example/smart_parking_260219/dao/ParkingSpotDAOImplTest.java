package org.example.smart_parking_260219.dao;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.util.AppConfig;
import org.example.smart_parking_260219.vo.ParkingSpotVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// 설정된 DB의 주차 공간을 추가, 수정, 조회하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class ParkingSpotDAOImplTest {

    private ParkingSpotDAO parkingSpotDAO;
    // 데이터 생성과 검증, 정리에서 같은 임시 주차면 번호를 사용함
    private String testSpaceId;

    @BeforeEach
    void setUp() throws SQLException {
        // IDE에서 직접 실행해도 개발 DB에 데이터를 넣지 않도록 확인함
        assertEquals(
                "jdbc:mariadb://localhost:3308/smart_parking_test",
                AppConfig.get("db.url")
        );
        assertEquals("parking_test", AppConfig.get("db.username"));

        parkingSpotDAO = new ParkingSpotDAOImpl();

        // space_id는 최대 5글자이므로 T와 임의의 4글자를 사용함
        String newSpaceId = "T" + UUID.randomUUID().toString().substring(0, 4);

        // 조회, 수정할 데이터는 DAO 메서드와 별개로 직접 준비함
        try (Connection connection = DBConnection.INSTANCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO parking_spot (space_id) VALUES (?)")) {
            statement.setString(1, newSpaceId);
            statement.executeUpdate();
        }

        testSpaceId = newSpaceId;
        log.info("--- 테스트 주차면 생성: spaceId={} ---", testSpaceId);
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
            log.info("--- 테스트 주차면 정리: spaceId={}, 삭제 건수={} ---", testSpaceId, deletedCount);
        }
    }

    @Test
    void selectAllParkingSpotTest() {
        List<ParkingSpotVO> allSpots = parkingSpotDAO.selectAllParkingSpot();
        ParkingSpotVO found = allSpots.stream()
                .filter(spot -> testSpaceId.equals(spot.getSpaceId()))
                .findFirst()
                .orElseThrow();

        log.info("--- 전체 주차면 조회: 총 {}개, 테스트 주차면={} ---", allSpots.size(), found);
        assertEquals(Boolean.TRUE, found.getEmpty());
        assertNull(found.getCarNum());
    }

    @Test
    void selectEmptyParkingSpotTest() {
        List<ParkingSpotVO> emptySpots = parkingSpotDAO.selectEmptyParkingSpot();

        boolean containsTestSpace = emptySpots.stream()
                .anyMatch(spot -> testSpaceId.equals(spot.getSpaceId()));
        log.info("--- 빈 주차면 조회: 총 {}개, 테스트 주차면 포함={} ---", emptySpots.size(), containsTestSpace);

        assertTrue(containsTestSpace);
        assertTrue(emptySpots.stream()
                .allMatch(spot -> Boolean.TRUE.equals(spot.getEmpty())));
    }

    @Test
    void updateInputParkingSpotTest() {
        String carNum = "TEST1234";

        parkingSpotDAO.updateInputParkingSpot(ParkingSpotVO.builder()
                .spaceId(testSpaceId)
                .carNum(carNum)
                .build());

        ParkingSpotVO updated =
                parkingSpotDAO.selectParkingSpotBySpaceId(testSpaceId);

        log.info("--- 입차 상태 변경 결과: spaceId={}, 주차면={} ---", testSpaceId, updated);
        assertNotNull(updated);
        assertEquals(Boolean.FALSE, updated.getEmpty());
        assertEquals(carNum, updated.getCarNum());

        assertTrue(parkingSpotDAO.selectEmptyParkingSpot().stream()
                .noneMatch(spot -> testSpaceId.equals(spot.getSpaceId())));
    }
}
