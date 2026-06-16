package org.example.smart_parking_260219.dao;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.vo.MemberVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Log4j2
class MemberDAOTest {
    private final MemberDAO memberDAO = MemberDAO.getInstance();

    @Test
    public void insertTest() {
        try {
            MemberVO memberVO = MemberVO.builder()
                    .carNum("12341234")
                    .carType(2)
                    .name("test")
                    .phone("010-1111-2222")
                    .subscribed(true)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusMonths(1))
                    .subscribedFee(100000)
                    .createDate(LocalDate.from(LocalDateTime.now()))
                    .build();
            memberDAO.insertMember(memberVO);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 더미 데이터용. 페이징 테스트를 위해 최소 3번은 돌려 봐야함
    @Test
    public void memberDummy() throws SQLException {
        for (int i = 0; i < 9; i++) {
            MemberVO memberVO = MemberVO.builder()
                    .carNum((i+1) + "33다1234")
                    .carType(2)
                    .name("test")
                    .phone("010-1111-2222")
                    .subscribed(true)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusMonths(1))
                    .subscribedFee(100000)
                    .createDate(LocalDate.from(LocalDateTime.now().plusDays(i)))
                    .build();
            memberDAO.insertMember(memberVO);
        }
    }

    @Test
    public void selectAllTest() throws SQLException {
        var member = memberDAO.selectAllMember();

        for (MemberVO memberVO : member) {
            log.info(memberVO);
        }

    }

    @Test
    public void selectOneTest()throws SQLException {
        String carNum = "12341234";

        MemberVO memberVO = memberDAO.selectOneMember(carNum);
        log.info(memberVO);

        Assertions.assertNotNull(memberVO);
    }

    @Test
    public void updateTest() throws SQLException {
        String carNum = "12341234";

        MemberVO memberVO = MemberVO.builder()
                .carType(2)
                .name("update")
                .phone("010-1111-2222")
                .subscribed(true)
                .carNum("12341234")
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusMonths(3))
                .build();
        memberDAO.updateMember(memberVO);
        MemberVO memberVO1 = memberDAO.selectOneMember(carNum);
        if (memberVO.getCarNum().equals(memberVO1.getCarNum()) &&
                memberVO.getCarType() == (memberVO1.getCarType()) &&
                memberVO.getName().equals(memberVO1.getName()) &&
                memberVO.getPhone().equals(memberVO1.getPhone()) &&
                memberVO.isSubscribed() == (memberVO1.isSubscribed()) &&
                memberVO.getStartDate().equals(memberVO1.getStartDate()) &&
                memberVO.getEndDate().equals(memberVO1.getEndDate())) {
            log.info("변경 선공");
        }

    }

    @Test
    public void deleteTest() throws SQLException {
        String carNum = "12341234";
        memberDAO.deleteMember(carNum);
    }

}