package org.example.smart_parking_260219.service;

import lombok.extern.log4j.Log4j2;
import org.example.smart_parking_260219.dao.ParkingDAO;
import org.example.smart_parking_260219.dao.ParkingDAOImpl;
import org.example.smart_parking_260219.dto.ParkingDTO;
import org.example.smart_parking_260219.util.MapperUtil;
import org.example.smart_parking_260219.vo.ParkingVO;
import org.modelmapper.ModelMapper;

import java.util.List;

@Log4j2
public enum ParkingService {
    INSTANCE;

    private final ParkingDAO parkingDAO;
    private final ModelMapper modelMapper;

    ParkingService() {
        parkingDAO = new ParkingDAOImpl();
        modelMapper = MapperUtil.INSTANCE.getInstance();
    }

    // 신규 입차 요청을 주차 기록으로 저장
    public void addParking(ParkingDTO parkingDTO) {
        ParkingVO parkingVO = modelMapper.map(parkingDTO, ParkingVO.class);
        parkingDAO.insertParking(parkingVO);
        log.info("입차 처리 완료 - 차량번호: {}", parkingVO.getCarNum());
    }

    // 차량번호 뒤 4자리로 주차 기록 조회
    public ParkingDTO getParking(String last4) {
        return modelMapper.map(parkingDAO.selectParkingByLast4(last4), ParkingDTO.class);
    }

    // 주차 기록 ID로 결제, 출차 대상 조회
    public ParkingDTO getByIdParking(int parkingId) {
        ParkingVO parkingVO = parkingDAO.selectParkingByParkingId(parkingId);

        // 존재하지 않는 주차 ID는 ModelMapper에 전달하지 않고 조회 실패로 반환함
        if (parkingVO == null) {
            return null;
        }

        return modelMapper.map(parkingVO, ParkingDTO.class);
    }

    // 전체 주차 기록 조회
    public List<ParkingDTO> getAllParking() {
        List<ParkingVO> parkingVOList = parkingDAO.selectAllParking();
        return parkingVOList.stream()
                .map(parkingVO -> modelMapper.map(parkingVO, ParkingDTO.class)).toList();
    }

    // 차량번호로 미정산 주차 기록 조회
    public ParkingDTO getParkingByCarNum(String carNum) {
        if (carNum == null) {
            return null;
        }

        ParkingVO parkingVO =
                parkingDAO.selectParkingByCarNum(carNum);

        if (parkingVO == null) {
            return null;
        }

        return modelMapper.map(parkingVO, ParkingDTO.class);
    }
}
