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

    // 입차된 시간
    public void addParking(ParkingDTO parkingDTO) {
        ParkingVO parkingVO = modelMapper.map(parkingDTO, ParkingVO.class);
        parkingDAO.insertParking(parkingVO);
        log.info("입차 처리 완료 - 차량번호: {}", parkingVO.getCarNum());
    }

    // 차번호 뒷자리 일치 차량 조회
    public ParkingDTO getParking(String last4) {
        return modelMapper.map(parkingDAO.selectParkingByLast4(last4), ParkingDTO.class);
    }

    // 출차된 시간
    public void modifyParking(ParkingDTO parkingDTO) {
        ParkingVO parkingVO = modelMapper.map(parkingDTO, ParkingVO.class);
        parkingDAO.updateParking(parkingVO);
    }

    // 차량 타입 업네이트
    public void modifyParkingCarType(ParkingDTO parkingDTO) {
        ParkingVO parkingVO = modelMapper.map(parkingDTO, ParkingVO.class);
        parkingDAO.updateParking(parkingVO);
    }

    // 프라이머리키 기준 차량 조회
    public ParkingDTO getByIdParking(int id) {
        return modelMapper.map(parkingDAO.selectParkingByParkingId(id), ParkingDTO.class);
    }

    public List<ParkingDTO> getAllParking() {
        List<ParkingVO> parkingVOList = parkingDAO.selectAllParking();
        return parkingVOList.stream()
                .map(parkingVO -> modelMapper.map(parkingVO, ParkingDTO.class)).toList();
    }

    public ParkingDTO getParkingByCarNum(String carNum) {
        if (carNum == null) return null;
        ParkingVO parkingVO = parkingDAO.selectParkingByCarNum(carNum);
        if (parkingVO == null) return null;
        return modelMapper.map(parkingDAO.selectParkingByCarNum(carNum), ParkingDTO.class);
    }
}
