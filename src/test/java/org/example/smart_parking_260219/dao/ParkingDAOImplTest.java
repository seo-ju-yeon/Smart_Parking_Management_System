package org.example.smart_parking_260219.dao;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.vo.ParkingVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// 설정된 DB에 주차 기록을 추가하고 조회하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class ParkingDAOImplTest {
    private ParkingDAO parkingDAO;

    @BeforeEach
    protected void ready() {
        parkingDAO = new ParkingDAOImpl();
    }

    @Test
    public void insertParkingTest() {
        ParkingVO parkingVO = ParkingVO.builder()
                .memberId(6)
                .carNum("바1111")
                .spaceId("A16")
                .carType(4)
                .build();
        parkingDAO.insertParking(parkingVO);
    }

    @Test
    public void selectParkingTest() {
        String carNum = "1111";

        ParkingVO parkingVO = parkingDAO.selectParkingByLast4(carNum);
        Assertions.assertNotNull(parkingVO);
        log.info("parking: {}", parkingVO);
    }

    @Test
    public void selectParkingByIdTest() {
        int id = 1;

        ParkingVO parkingVO = parkingDAO.selectParkingByParkingId(id);
        Assertions.assertNotNull(parkingVO);
        log.info("parking: {}", parkingVO);
    }
}
