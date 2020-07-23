package zhangzhixian.monopoly.service;

import zhangzhixian.monopoly.model.User;
import zhangzhixian.monopoly.model.dto.MapDto;
import zhangzhixian.monopoly.model.dto.RequestDTO;

import java.util.List;

public interface MainService {

    MapDto getMap();

    List<User> listUsers();

    void roll(RequestDTO requestDTO);

    void pass(RequestDTO requestDTO);

    void upgrade(RequestDTO requestDTO);

    void addMessage(String message);

}
