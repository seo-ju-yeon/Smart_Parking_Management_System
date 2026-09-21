package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dao.FeePolicyDAO;
import org.example.smart_parking_260219.dao.MemberDAO;
import org.example.smart_parking_260219.dao.StatisticsDAO;
import org.example.smart_parking_260219.dto.StatisticsDTO;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public enum StatisticsService {
    INSTANCE;

    private final StatisticsDAO statisticsDAO;

    private StatisticsService() {
        statisticsDAO = StatisticsDAO.getInstance();
    }

    /*
     * 시간대별 매출 0시~23시까지 빈 곳을 0으로 채워 리턴
     */
    public List<StatisticsDTO> getHourlySales(String targetDate) {
        List<StatisticsDTO> rawData = statisticsDAO.selectHourlySalesByDay(targetDate);
        List<StatisticsDTO> filledData = new ArrayList<>();

        for (int i = 0; i <= 23; i++) {
            String label = i + "시";
            // DB 결과 중 해당 시간이 있는지 확인, 없으면 0L 세팅
            StatisticsDTO match = rawData.stream()
                    .filter(dto -> dto.getLabel().equals(label))
                    .findFirst()
                    .orElse(StatisticsDTO.builder().label(label).value(0L).build());
            filledData.add(match);
        }
        return filledData;
    }

    /*
     * 시간대별 입차량 - 0시~23시까지 빈 곳을 0으로 채워 리턴
     */
    public List<StatisticsDTO> getHourlyCount(String targetDate) {
        List<StatisticsDTO> rawData = statisticsDAO.selectHourlyCountByDay(targetDate);
        List<StatisticsDTO> filledData = new ArrayList<>();

        for (int i = 0; i <= 23; i++) {
            String label = i + "시";
            StatisticsDTO match = rawData.stream()
                    .filter(dto -> dto.getLabel().equals(label))
                    .findFirst()
                    .orElse(StatisticsDTO.builder().label(label).value(0L).build());
            filledData.add(match);
        }
        return filledData;
    }

    /*
     * 월간 일별 매출 - 1일부터 해당 월의 마지막 날까지 빈 곳을 0으로 채움
     */
    public List<StatisticsDTO> getDailySales(int year, int month) {
        List<StatisticsDTO> rawData = statisticsDAO.selectMonthlySalesByYearMonth(year, month);
        List<StatisticsDTO> filledData = new ArrayList<>();

        // 해당 월의 마지막 날짜 계산 (예: 28, 30, 31)
        int lastDay = YearMonth.of(year, month).lengthOfMonth();

        for (int i = 1; i <= lastDay; i++) {
            // DB 라벨 형식 "2026-02-12"와 맞춰서 비교
            String dateLabel = String.format("%d-%02d-%02d", year, month, i);

            StatisticsDTO match = rawData.stream()
                    .filter(dto -> dto.getLabel().equals(dateLabel))
                    .findFirst()
                    .orElse(StatisticsDTO.builder().label(dateLabel).value(0L).build());
            filledData.add(match);
        }
        return filledData;
    }

     // 차종별 비율 - 데이터가 없는 차종은 0으로 나오거나 리스트에서 제외됨
    public List<StatisticsDTO> getCarTypeStats(int year, int month) {
        return statisticsDAO.selectCarTypeStatsByPeriod(year, month);
    }

    // 월정액 회원 월별 합계
    public int getMonthlySales() throws SQLException {
        try {
            int count = MemberDAO.getInstance().countSubscribedMembers();
            int fee = FeePolicyService.getInstance().getPolicy().getSubscribedFee();
            return count * fee;
        } catch (Exception e) {
            log.error("월정액 매출 계산 중 오류 발생", e);
            return 0;
        }
    }

}
