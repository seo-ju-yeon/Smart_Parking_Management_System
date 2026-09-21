package org.example.smart_parking_260219.dao;

import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.vo.ParkingVO;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class ParkingDAOImpl implements ParkingDAO {
    private static ParkingDAO instance;

    public ParkingDAOImpl() {}

    public static ParkingDAO getInstance() {
        if (instance == null) {
            instance = new ParkingDAOImpl();
        }
        return instance;
    }

    // 입차 확인
    @Override
    public void insertParking(ParkingVO parkingVO) {
        String sql = "INSERT INTO smart_parking_team2.parking (car_num, space_id, entry_time, car_type) VALUES (?, ?, now(), ?)";
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, parkingVO.getCarNum());
            preparedStatement.setString(2, parkingVO.getSpaceId());
            preparedStatement.setInt(3, parkingVO.getCarType());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 주차 차량 조회
    @Override
    public ParkingVO selectParkingByLast4(String last4) {
        String sql = "SELECT * FROM smart_parking_team2.parking WHERE RIGHT(car_num, 4) = ?";
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, last4);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                ParkingVO parkingVO = ParkingVO.builder()
                        .parkingId(resultSet.getInt("parking_id"))
                        .memberId(resultSet.getInt("member_id"))
                        .carNum(resultSet.getString("car_num"))
                        .spaceId(resultSet.getString("space_id"))
                        .entryTime(resultSet.getTimestamp("entry_time").toLocalDateTime())
                        .totalTime(resultSet.getInt("total_time"))
                        .paid(resultSet.getBoolean("paid"))
                        .build();
                return parkingVO;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public ParkingVO selectParkingByCarNum(String carNum) {
        String sql = "SELECT * FROM smart_parking_team2.parking WHERE car_num = ? AND paid = false";
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, carNum);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                ParkingVO parkingVO = ParkingVO.builder()
                        .parkingId(resultSet.getInt("parking_id"))
                        .carNum(resultSet.getString("car_num"))
                        .memberId(resultSet.getInt("member_id"))
                        .spaceId(resultSet.getString("space_id"))
                        .entryTime(resultSet.getTimestamp("entry_time").toLocalDateTime())
                        .paid(resultSet.getBoolean("paid"))
                        .build();
                return parkingVO;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // 출차 확인
    @Override
    public void updateParking(ParkingVO parkingVO) {
        LocalDateTime entry = selectParkingByCarNum(parkingVO.getCarNum()).getEntryTime();
        if (entry == null) {
            log.error("entry is null");
            return;
        }
        LocalDateTime exit = LocalDateTime.now();
        long totalMinutes = Duration.between(entry, exit).toMinutes();

        String sql = "UPDATE smart_parking_team2.parking SET exit_time= ?,car_type =?, total_time= ?, paid=true WHERE car_num=?";
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setTimestamp(1, Timestamp.valueOf(exit));
            preparedStatement.setInt(2, parkingVO.getCarType());
            preparedStatement.setLong(3, totalMinutes);
            preparedStatement.setString(4, parkingVO.getCarNum());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 주차 기록 기준 차량 조회
    @Override
    public ParkingVO selectParkingByParkingId(int parkingId) {
        String sql = "SELECT * FROM smart_parking_team2.parking WHERE parking_id = ?";
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, parkingId);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                ParkingVO parkingVO = ParkingVO.builder()
                        .parkingId(resultSet.getInt("parking_id"))
                        .carNum(resultSet.getString("car_num"))
                        .memberId(resultSet.getInt("member_id"))
                        .spaceId(resultSet.getString("space_id"))
                        .entryTime(resultSet.getTimestamp("entry_time").toLocalDateTime())
                        .paid(resultSet.getBoolean("paid"))
                        .build();
                return parkingVO;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // 전체 주차 차량 조회
    @Override
    public List<ParkingVO> selectAllParking() {
        String sql = "SELECT * FROM smart_parking_team2.parking ORDER BY entry_time DESC";
        List<ParkingVO> ParkingVOList = new ArrayList<>();
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                ParkingVO parkingVO = ParkingVO.builder()
                        .parkingId(resultSet.getInt("parking_id"))
                        .carNum(resultSet.getString("car_num"))
                        .memberId(resultSet.getInt("member_id"))
                        .spaceId(resultSet.getString("space_id"))
                        .entryTime(resultSet.getTimestamp("entry_time").toLocalDateTime())
                        .paid(resultSet.getBoolean("paid"))
                        .build();
                ParkingVOList.add(parkingVO);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return ParkingVOList;
    }
}
