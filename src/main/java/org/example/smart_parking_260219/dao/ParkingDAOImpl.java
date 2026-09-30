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

    // parkingId로 결제 및 출차 대상이 되는 주차 기록을 조회함
    private static final String SELECT_PARKING_BY_ID_SQL =
            "SELECT * FROM smart_parking_team2.parking WHERE parking_id = ?";

    // 정산 트랜잭션이 끝날 때까지 조회한 행을 잠가 동일 기록의 동시 결제를 방지함
    private static final String SELECT_PARKING_FOR_UPDATE_SQL =
            SELECT_PARKING_BY_ID_SQL + " FOR UPDATE";

    // 미정산 기록에만 출차 정보를 반영하고 paid를 true로 변경함
    private static final String UPDATE_PARKING_FOR_PAYMENT_SQL =
            "UPDATE smart_parking_team2.parking "
                    + "SET exit_time = ?, car_type = ?, total_time = ?, paid = true "
                    // 이미 결제된 주차 기록을 다시 출차 처리하지 않음
                    + "WHERE parking_id = ? AND paid = false";

    private static ParkingDAO instance;

    public ParkingDAOImpl() {}

    public static ParkingDAO getInstance() {
        if (instance == null) {
            instance = new ParkingDAOImpl();
        }
        return instance;
    }

    // 신규 입차 기록 저장
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

    // 차량번호 뒤 4자리로 주차 기록 조회
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

    // 차량번호로 차량 유형을 포함한 미정산 주차 기록 조회
    @Override
    public ParkingVO selectParkingByCarNum(String carNum) {
        String sql = "SELECT * FROM smart_parking_team2.parking WHERE car_num = ? AND paid = false";
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, carNum);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return mapParking(resultSet);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // 출차 시간과 총 주차 시간을 계산하여 정산 완료 처리
    @Override
    public void updateParking(ParkingVO parkingVO) {
        LocalDateTime entry = selectParkingByCarNum(parkingVO.getCarNum()).getEntryTime();
        if (entry == null) {
            log.error("출차 처리에 필요한 입차 시간이 없습니다.");
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

    // 주차 기록 ID로 결제, 출차 대상 조회
    @Override
    public ParkingVO selectParkingByParkingId(int parkingId) {
        try {
            @Cleanup Connection connection = DBConnection.INSTANCE.getConnection();
            @Cleanup PreparedStatement preparedStatement =
                    connection.prepareStatement(SELECT_PARKING_BY_ID_SQL);
            preparedStatement.setInt(1, parkingId);
            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return mapParking(resultSet);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // 같은 주차 기록이 동시에 정산되지 않도록 트랜잭션 종료 시점까지 행을 잠금
    @Override
    public ParkingVO selectParkingByParkingIdForUpdate(
            Connection connection,
            int parkingId
    ) throws SQLException {
        try (PreparedStatement preparedStatement =
                     connection.prepareStatement(SELECT_PARKING_FOR_UPDATE_SQL)) {
            preparedStatement.setInt(1, parkingId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapParking(resultSet);
                }
            }
        }
        return null;
    }

    // 전달받은 Connection으로 출차 정보를 반영하며 Connection은 DAO에서 닫지 않음
    @Override
    public int updateParkingForPayment(
            Connection connection,
            int parkingId,
            int carType,
            LocalDateTime exitTime,
            long totalMinutes
    ) throws SQLException {
        try (PreparedStatement preparedStatement =
                     connection.prepareStatement(UPDATE_PARKING_FOR_PAYMENT_SQL)) {
            preparedStatement.setTimestamp(1, Timestamp.valueOf(exitTime));
            preparedStatement.setInt(2, carType);
            preparedStatement.setLong(3, totalMinutes);
            preparedStatement.setInt(4, parkingId);

            // 이미 정산된 기록은 WHERE paid = false 조건으로 갱신되지 않음
            return preparedStatement.executeUpdate();
        }
    }

    // 전체 주차 기록을 최근 입차 순으로 조회
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

    // 일반 조회와 잠금 조회가 동일한 주차 정보를 반환하도록 매핑을 한 곳에서 관리함
    private ParkingVO mapParking(ResultSet resultSet) throws SQLException {
        Timestamp exitTimestamp = resultSet.getTimestamp("exit_time");

        return ParkingVO.builder()
                .parkingId(resultSet.getInt("parking_id"))
                .memberId(resultSet.getInt("member_id"))
                .spaceId(resultSet.getString("space_id"))
                .carNum(resultSet.getString("car_num"))
                .carType(resultSet.getInt("car_type"))
                .entryTime(resultSet.getTimestamp("entry_time").toLocalDateTime())
                .exitTime(exitTimestamp == null
                        ? null
                        : exitTimestamp.toLocalDateTime())
                .totalTime(resultSet.getInt("total_time"))
                .paid(resultSet.getBoolean("paid"))
                .build();
    }
}
