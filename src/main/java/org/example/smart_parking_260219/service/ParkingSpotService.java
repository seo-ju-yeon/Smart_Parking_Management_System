package org.example.smart_parking_260219.service;

import org.example.smart_parking_260219.dao.ParkingSpotDAO;
import org.example.smart_parking_260219.dao.ParkingSpotDAOImpl;
import org.example.smart_parking_260219.dto.ParkingSpotDTO;
import org.example.smart_parking_260219.util.MapperUtil;
import org.example.smart_parking_260219.vo.ParkingSpotVO;
import org.modelmapper.ModelMapper;

import java.util.List;

public enum ParkingSpotService {
    INSTANCE;

    private final ParkingSpotDAO parkingSpotDAO;
    private final ModelMapper modelMapper;

    ParkingSpotService() {
        parkingSpotDAO = new ParkingSpotDAOImpl();
        modelMapper = MapperUtil.INSTANCE.getInstance();
    }

    // 주차장 목록
    public List<ParkingSpotDTO> getAllParkingSpot() {
        List<ParkingSpotVO> parkingSpotVOList = parkingSpotDAO.selectAllParkingSpot();

        return parkingSpotVOList.stream()
                .map(parkingSpotVO -> modelMapper.map(parkingSpotVO, ParkingSpotDTO.class)).toList();
    }

    // 주차 공간 입차 갱신
    public void modifyInputParkingSpot(ParkingSpotDTO parkingSpotDTO) {
        ParkingSpotVO parkingSpotVO = modelMapper.map(parkingSpotDTO, ParkingSpotVO.class);
        parkingSpotDAO.updateInputParkingSpot(parkingSpotVO);
    }

    // 주차 공간 출차 갱신
    public void modifyOutputParkingSpot(ParkingSpotDTO parkingSpotDTO) {
        ParkingSpotVO parkingSpotVO = modelMapper.map(parkingSpotDTO, ParkingSpotVO.class);
        parkingSpotDAO.updateOutputParkingSpot(parkingSpotVO);
    }

    // 빈 주차 공간 찾기
    public List<ParkingSpotDTO> getEmptyParkingSpot() {
        List<ParkingSpotVO> parkingSpotVOList = parkingSpotDAO.selectEmptyParkingSpot();

        return parkingSpotVOList.stream()
                .map(parkingSpotVO -> modelMapper.map(parkingSpotVO, ParkingSpotDTO.class)).toList();
    }

    // 주차 구역 기준 조회
    public ParkingSpotDTO getParkingSpotBySpaceId(String spaceId) {
        if (spaceId == null) return null;
        ParkingSpotVO parkingSpotVO = parkingSpotDAO.selectParkingSpotBySpaceId(spaceId);
        if (parkingSpotVO == null) return null;
        return modelMapper.map(parkingSpotVO, ParkingSpotDTO.class);
    }
}
