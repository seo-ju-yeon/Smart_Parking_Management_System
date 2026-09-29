package org.example.smart_parking_260219.dao;

import org.example.smart_parking_260219.vo.ParkingVO;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface ParkingDAO {
    // 신규 입차 기록 저장
    void insertParking(ParkingVO parkingVO);

    // 차량번호 뒤 4자리로 주차 기록 조회
    ParkingVO selectParkingByLast4(String last4);

    // 차량번호로 미정산 주차 기록 조회
    ParkingVO selectParkingByCarNum(String carNum);

    // 기존 독립 출차 흐름에서 출차 및 정산 완료 처리
    void updateParking(ParkingVO parkingVO);

    // 주차 기록 ID로 결제, 출차 대상 조회
    ParkingVO selectParkingByParkingId(int parkingId);

    // 트랜잭션에서 결제 대상을 잠금 조회
    ParkingVO selectParkingByParkingIdForUpdate(
            Connection connection,
            int parkingId
    ) throws SQLException;

    // 트랜잭션에서 출차 정보와 정산 완료 상태 갱신
    int updateParkingForPayment(
            Connection connection,
            int parkingId,
            int carType,
            LocalDateTime exitTime,
            long totalMinutes
    ) throws SQLException;

    // 전체 주차 기록 조회
    List<ParkingVO> selectAllParking();
}
