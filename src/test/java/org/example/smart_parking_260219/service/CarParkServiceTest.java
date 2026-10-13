package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.ParkingSpotDTO;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

// 설정된 DB의 주차 공간을 조회하고 입차 상태를 변경하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class CarParkServiceTest {

    private ParkingSpotService parkingSpotService = ParkingSpotService.INSTANCE;

    @Test
    public void getAllCarParkTest() {
        List<ParkingSpotDTO> parkingSpotDTOList = parkingSpotService.getAllParkingSpot();
        for (ParkingSpotDTO parkingSpotDTO : parkingSpotDTOList) {
            log.info("carParkDTO : {}", parkingSpotDTO);
        }
        log.info("목록 조회 완료");
    }

    @Test
    @Disabled("전용 테스트 DB와 검증 코드가 준비될 때까지 실행하지 않음")
    public void modifyInputCarParkTest() {
        parkingSpotService.modifyInputParkingSpot(ParkingSpotDTO.builder()
                        .spaceId("A02")
                        .carNum("다3222")
                .build());
    }
    @Test
    public void getParkingSpotTest() {
        List<ParkingSpotDTO> parkingSpotDTOList = parkingSpotService.getEmptyParkingSpot();
        for (ParkingSpotDTO parkingSpotDTO : parkingSpotDTOList) {
            log.info("carParkDTO : {}", parkingSpotDTO);
        }
        log.info("빈 공간 주차 목록 조회 완료");
    }
}
