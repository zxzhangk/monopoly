package zhangzhixian.monopoly.service;

import zhangzhixian.monopoly.model.User;
import zhangzhixian.monopoly.model.dto.MapDto;

import java.util.List;

public interface MainService {

    MapDto getMap();

    List<User> listUsers();

}
