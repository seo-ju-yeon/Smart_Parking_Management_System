package org.example.smart_parking_260219.dao;

import lombok.Cleanup;
import org.example.smart_parking_260219.connection.DBConnection;
import org.example.smart_parking_260219.vo.ParkingSpotVO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParkingSpotDAOImpl implements ParkingSpotDAO {
    // 지정한 차량이 사용 중인 주차 공간만 빈 상태로 변경함
    private static final String UPDATE_PARKING_SPOT_FOR_EXIT_SQL =
            "UPDATE smart_parking_team2.parking_spot "
                    + "SET `empty` = true, car_num = null, last_update = now() "
                    // 해당 차량이 실제로 사용 중인 공간만 반환함
                    + "WHERE space_id = ? AND car_num = ? AND `empty` = false";

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

    // 기존 독립 출차 흐름에서 차량번호로 주차 공간을 반환함
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

    // 전달받은 Connection으로 공간을 반환하며 Connection은 DAO에서 닫지 않음
    @Override
    public int updateParkingSpotForExit(
            Connection connection,
            String spaceId,
            String carNum
    ) throws SQLException {
        try (PreparedStatement preparedStatement =
                     connection.prepareStatement(UPDATE_PARKING_SPOT_FOR_EXIT_SQL)) {
            preparedStatement.setString(1, spaceId);
            preparedStatement.setString(2, carNum);

            // 공간, 차량, 사용 상태가 모두 일치할 때만 1건이 갱신됨
            return preparedStatement.executeUpdate();
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
