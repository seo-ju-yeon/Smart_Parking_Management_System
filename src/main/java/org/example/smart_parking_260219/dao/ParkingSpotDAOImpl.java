package org.example.smart_parking_260219.dao;

import lombok.Cleanup;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.vo.ParkingSpotVO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParkingSpotDAOImpl implements ParkingSpotDAO {
    private static ParkingSpotDAO instance;

    public ParkingSpotDAOImpl() {}

    public static ParkingSpotDAO getInstance() {
        if (instance == null) {
            instance = new ParkingSpotDAOImpl();
        }
        return instance;
    }

    @Override
    public void insertParkingSpot(ParkingSpotVO parkingSpotVO) {
        String sql = "INSERT INTO smart_parking_team2.parking_spot (space_id, `empty`, last_update) VALUES (?, true ,now())";
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, parkingSpotVO.getSpaceId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ParkingSpotVO> selectAllParkingSpot() {
        String sql = "SELECT * FROM smart_parking_team2.parking_spot";
        List<ParkingSpotVO> ParkingSpotVOList = new ArrayList<>();
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                ParkingSpotVO parkingSpotVO = ParkingSpotVO.builder()
                        .spaceId(resultSet.getString("space_id"))
                        .empty(resultSet.getBoolean("empty"))
                        .carNum(resultSet.getString("car_num"))
                        .lastUpdate(resultSet.getTimestamp("last_update").toLocalDateTime())
                        .build();
                ParkingSpotVOList.add(parkingSpotVO);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return ParkingSpotVOList;
    }

    @Override
    public void updateInputParkingSpot(ParkingSpotVO parkingSpotVO) {
        String sql = "UPDATE smart_parking_team2.parking_spot SET `empty` = false, car_num =?, last_update = now() WHERE space_id = ?";
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, parkingSpotVO.getCarNum());
            preparedStatement.setString(2, parkingSpotVO.getSpaceId());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // [버그수정] empty = false → true 로 수정 (출차 시 자리를 빈 상태로 변경해야 함)
    @Override
    public void updateOutputParkingSpot(ParkingSpotVO parkingSpotVO) {
        String sql = "UPDATE smart_parking_team2.parking_spot SET `empty` = true, car_num = null, last_update = now() WHERE car_num = ?";
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, parkingSpotVO.getCarNum());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ParkingSpotVO> selectEmptyParkingSpot() {
        String sql = "SELECT * FROM smart_parking_team2.parking_spot WHERE `empty` = true";
        List<ParkingSpotVO> ParkingSpotVOList = new ArrayList<>();
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                ParkingSpotVO parkingSpotVO = ParkingSpotVO.builder()
                        .spaceId(resultSet.getString("space_id"))
                        .empty(resultSet.getBoolean("empty"))
                        .carNum(resultSet.getString("car_num"))
                        .lastUpdate(resultSet.getTimestamp("last_update").toLocalDateTime())
                        .build();
                ParkingSpotVOList.add(parkingSpotVO);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return ParkingSpotVOList;
    }

    @Override
    public ParkingSpotVO selectParkingSpotBySpaceId(String spaceId) {
        String sql = "SELECT * FROM smart_parking_team2.parking_spot WHERE space_id = ?";
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, spaceId);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                ParkingSpotVO parkingSpotVO = ParkingSpotVO.builder()
                        .spaceId(resultSet.getString("space_id"))
                        .empty(resultSet.getBoolean("empty"))
                        .carNum(resultSet.getString("car_num"))
                        .lastUpdate(resultSet.getTimestamp("last_update").toLocalDateTime())
                        .build();
                return parkingSpotVO;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
