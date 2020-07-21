package zhangzhixian.monopoly.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import zhangzhixian.monopoly.configuration.CustomizeConfiguration;
import zhangzhixian.monopoly.model.Grid;
import zhangzhixian.monopoly.model.User;
import zhangzhixian.monopoly.model.dto.MapDto;
import zhangzhixian.monopoly.service.MainService;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;

@Service
public class MainServiceImpl implements MainService {

    @Autowired
    private CustomizeConfiguration configuration;

    private List<Grid> map;

    private List<User> users;

    @PostConstruct
    private void init() {
        map = configuration.getMaps();
        users = configuration.getUsers();
    }


    @Override
    public MapDto getMap() {
        map.forEach( grid -> grid.getUsers().clear());
        users.forEach(u -> {
            if (u.getPosition() != -1) {
                map.get(u.getPosition()).getUsers().add(u);
            }
        });
        return MapDto.builder().map(map).users(users).build();
    }

    @Override
    public List<User> listUsers() {
        return users;
    }
}
