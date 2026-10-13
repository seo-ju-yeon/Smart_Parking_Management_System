package org.example.smart_parking_260219.dao;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.vo.ParkingSpotVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

// 설정된 DB의 주차 공간을 추가·수정·조회하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class ParkingSpotDAOImplTest {
    private ParkingSpotDAO parkingSpotDAO;

    @BeforeEach
    protected void ready() {
        parkingSpotDAO = new ParkingSpotDAOImpl();
    }

    @Test
    public void insertCarPark() {
        for (int i = 1; i <= 20; i++) {
            ParkingSpotVO parkingSpotVO = ParkingSpotVO.builder()
                    .spaceId("A" + i)
                    .build();
            parkingSpotDAO.insertParkingSpot(parkingSpotVO);
        }

    }

    @Test
    public void selectCarParkTest() {
        List<ParkingSpotVO> carParkVOList = parkingSpotDAO.selectAllParkingSpot();

        for (ParkingSpotVO carParkVO : carParkVOList) {
            log.info("carParks: {}", carParkVO);
        }
    }

    @Test
    public void updateInputCarCarParkTest() {
        String id = "A01";
        ParkingSpotVO carParkVO = ParkingSpotVO.builder()
                .carNum("가1111")
                .spaceId(id)
                .build();
        parkingSpotDAO.updateInputParkingSpot(carParkVO);
    }

    @Test
    public void selectEmptyParkingSpotTest() {
        List<ParkingSpotVO> parkingSpotVOList = parkingSpotDAO.selectEmptyParkingSpot();

        for (ParkingSpotVO parkingSpotVO : parkingSpotVOList) {
            log.info("carParks: {}", parkingSpotVO);
        }
    }
}
