package zhangzhixian.monopoly.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import zhangzhixian.monopoly.configuration.CustomizeConfiguration;
import zhangzhixian.monopoly.enums.StatusEnum;
import zhangzhixian.monopoly.model.Grid;
import zhangzhixian.monopoly.model.User;
import zhangzhixian.monopoly.model.dto.MapDto;
import zhangzhixian.monopoly.model.dto.RequestDTO;
import zhangzhixian.monopoly.service.MainService;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Service
public class MainServiceImpl implements MainService {

    @Autowired
    private CustomizeConfiguration configuration;

    private List<Grid> map;

    private List<User> users;

    private List<String> messageList = new LinkedList<>();

    private int tempMax = -1;
    private User tempUser;

    private Random random = new Random();

    @PostConstruct
    private void init() {
        map = configuration.getMaps();
        users = configuration.getUsers();
        users.forEach(u -> u.setMoney(configuration.getInitMoney()));
    }


    @Override
    public MapDto getMap() {
        map.forEach( grid -> grid.getUsers().clear());
        users.forEach(u -> {
            if (u.getPosition() != -1) {
                map.get(u.getPosition()).getUsers().add(u);
            }
        });
        return MapDto.builder().map(map).users(users).messages(messageList).build();
    }

    @Override
    public List<User> listUsers() {
        return users;
    }

    @Override
    public void roll(RequestDTO requestDTO) {
        // 一开始用户状态都是null，第一次摇骰子后变成waiting并记录点数，全部都为waiting后，点数最大的用户变成active，游戏开始
        String token = requestDTO.getToken();
        User user = users.stream().filter(u -> StringUtils.equals(u.getToken(), token)).findAny().orElse(null);
        if (Objects.isNull(user)) {
            return;
        }
        if (user.getStatus() == null) {
            int num = rollNum();
            user.setStatus(StatusEnum.waiting);
            if (num > tempMax) {
                tempUser = user;
                tempMax = num;
            }
            addMessage(String.format("%s 骰子点数为 %s", user.getName(), num));
            if (users.stream().noneMatch(u -> u.getStatus() != StatusEnum.waiting)) {
                addMessage(String.format("%s 的点数最大，先出发", tempUser.getName()));
                tempUser.setStatus(StatusEnum.active);
            }
        } else if (user.getStatus() == StatusEnum.active) {
            int num = rollNum();
            if (user.getPosition() + num >= map.size()) {
                user.setPosition(user.getPosition() + num - map.size());
                user.setMoney(user.getMoney() + 2000);
                addMessage(String.format("%s 经过起点，得2000", user.getName()));
            } else {
                user.setPosition(user.getPosition() + num);
            }
            // 根据目的地块类型判断状态转化
            Grid grid = map.get(user.getPosition());
            addMessage(String.format("%s 骰子点数为 %s, 前进 %s 步，到达 %s", user.getName(), num, num, grid.getName()));
            switch (grid.getType()) {
                case tax:
                    user.setMoney(user.getMoney() - grid.getPrice());
                    nextUser(user, StatusEnum.waiting);
                    addMessage(String.format("%s 扣税 %s", user.getName(), grid.getPrice()));
                    break;
                case prison:
                    user.setPosition(10);
                    nextUser(user, StatusEnum.jailed);
                    addMessage(String.format("%s 进监狱了", user.getName()));
                    break;
                default:
                    nextUser(user, StatusEnum.waiting);
                    break;
            }
        }
    }

    private void nextUser(User user, StatusEnum status) {
        user.setStatus(status);
        Node node = new Node();
        Node head = node;
        Node current = null;
        for (int i = 0; i < users.size(); i++) {
            User tempUser = users.get(i);
            if (i == 0) {
                node.user = tempUser;
            }
            node.next = new Node();
            node.next.user = tempUser;
            node = node.next;
            if (StringUtils.equals(tempUser.getToken(), user.getToken())) {
                current = node;
            }
        }
        node.next = head;
        while (true) {
            assert current != null;
            current = current.next;
            if (current.user.getStatus() != StatusEnum.jailed) {
                current.user.setStatus(StatusEnum.active);
                break;
            } else {
                // 出狱
                current.user.setStatus(StatusEnum.waiting);
                addMessage(String.format("%s 出狱了", current.user.getName()));
            }

        }
    }

    private int rollNum() {
        return random.nextInt(12) + 1;
    }

    @Override
    public void addMessage(String message) {
        if (messageList.size() >= 6) {
            messageList.remove(0);
        }
        messageList.add(message);
    }

}

class Node {
    User user;
    Node next;

}
