package org.example.smart_parking_260219.dao;

import org.example.smart_parking_260219.vo.ParkingSpotVO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface ParkingSpotDAO {
    // 신규 주차 공간 저장
    void insertParkingSpot(ParkingSpotVO parkingSpotVO);

    // 전체 주차 공간과 현재 사용 상태 조회
    List<ParkingSpotVO> selectAllParkingSpot();

    // 기존 독립 입차 흐름에서 주차 공간을 사용 중 상태로 변경
    void updateInputParkingSpot(ParkingSpotVO parkingSpotVO);

    // 기존 독립 출차 흐름에서 차량이 사용하던 주차 공간 반환
    void updateOutputParkingSpot(ParkingSpotVO parkingSpotVO);

    // 트랜잭션에서 출차 대상 차량이 사용 중인 주차 공간 반환
    int updateParkingSpotForExit(
            Connection connection,
            String spaceId,
            String carNum
    ) throws SQLException;

    // 현재 사용 가능한 주차 공간 조회
    List<ParkingSpotVO> selectEmptyParkingSpot();

    // 주차 공간 번호로 단일 공간 조회
    ParkingSpotVO selectParkingSpotBySpaceId(String spaceId);
}
