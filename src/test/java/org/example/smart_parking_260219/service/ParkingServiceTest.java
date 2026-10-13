package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dto.ParkingDTO;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// 설정된 DB의 주차 기록을 추가하고 조회하므로 통합 테스트로 분류함
@Tag("integration")
@Log4j2
class ParkingServiceTest {

    private ParkingService parkingService = ParkingService.INSTANCE;

    @Test
    @Disabled("전용 테스트 DB와 검증 코드가 준비될 때까지 실행하지 않음")
    public void addParkingTest() {
        parkingService.addParking(ParkingDTO.builder()
                        .carNum("나2222")
                        .memberId(2)
                        .spaceId("A02")
                        .carType(2)
                        .build()
        );
        log.info("추가 완료");
    }

    @Test
    public void getParkingTest() {
        String carNum = "2222";
        ParkingDTO parkingDTO = parkingService.getParking(carNum);
        log.info("parkingDTO: {}", parkingDTO);
    }

    @Test
    public void getByIdParkingTest() {
        int id = 1;
        ParkingDTO parkingDTO = parkingService.getByIdParking(id);
        log.info("parkingDTO: {}", parkingDTO);
    }
}
