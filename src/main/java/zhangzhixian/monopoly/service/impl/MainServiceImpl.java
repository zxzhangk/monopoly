package zhangzhixian.monopoly.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import zhangzhixian.monopoly.configuration.CustomizeConfiguration;
import zhangzhixian.monopoly.enums.GridEnum;
import zhangzhixian.monopoly.enums.StatusEnum;
import zhangzhixian.monopoly.model.Card;
import zhangzhixian.monopoly.model.Grid;
import zhangzhixian.monopoly.model.User;
import zhangzhixian.monopoly.model.dto.MapDto;
import zhangzhixian.monopoly.model.dto.RequestDTO;
import zhangzhixian.monopoly.service.MainService;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MainServiceImpl implements MainService {

    @Autowired
    private CustomizeConfiguration configuration;

    private List<Grid> map;

    private List<Grid> asset;

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
        return MapDto.builder().map(map).users(users).asset(asset).messages(messageList).build();
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
                    addMessage(String.format("%s 扣税 %s", user.getName(), grid.getPrice()));
                    nextUser(user, StatusEnum.waiting);
                    break;
                case prison:
                    user.setPosition(10);
                    addMessage(String.format("%s 进监狱了", user.getName()));
                    nextUser(user, StatusEnum.jailed);
                    break;
                case estate:
                case station:
                case livelihood:
                    getToEstate(user, grid, num);
                    break;
                case chance:
                    getToChance(user, grid);
                default:
                    nextUser(user, StatusEnum.waiting);
                    break;
            }
        }
    }

    private void getToChance(User user, Grid grid) {
        int i = random.nextInt(configuration.getChances().size());
        Card card = configuration.getChances().get(i);
        addMessage(user.getName() + " " + card.getName());
        switch (card.getType()) {
            case cost:
                user.setMoney(user.getMoney() - card.getPrice());
                break;
            case gain:
                user.setMoney(user.getMoney() + card.getPrice());
                break;
            case gainFromOther:
                user.setMoney(user.getMoney() + (users.size() - 1) * card.getPrice());
                users.forEach(u -> {
                    if (!u.getToken().equals(user.getToken())) {
                        u.setMoney(u.getMoney() - card.getPrice());
                        addMessage(String.format("%s失去%s元", u.getName(), card.getPrice()));
                    }
                });
                break;
            case costTogether:
                users.forEach(u -> {
                    u.setMoney(u.getMoney() - card.getPrice());
                    addMessage(String.format("%s失去%s元", u.getName(), card.getPrice()));
                });
                break;
            case houseDuty:
                int sum = map.stream().filter(g -> StringUtils.equals(g.getOwner(), user.getToken()))
                        .filter(g -> g.getType() == GridEnum.estate).mapToInt(g -> {
                            if (g.getRoomLevel() == 0) {
                                return 0;
                            } else if (g.getRoomLevel() < 5) {
                                return g.getRoomLevel() * card.getPrice();
                            } else {
                                return card.getHotelPrice();
                            }
                        }).sum();
                user.setMoney(user.getMoney() - sum);
                addMessage(String.format("%s失去%s元", user.getName(), sum));
                break;
            case impunity:
                // TODO
                break;
            case jailed:
                user.setPosition(10);
                nextUser(user, StatusEnum.jailed);
                return;
            default:
                break;
        }
        nextUser(user, StatusEnum.waiting);
    }


    @Override
    public void pass(RequestDTO requestDTO) {
        asset = Collections.emptyList();
        String token = requestDTO.getToken();
        User user = users.stream().filter(u -> StringUtils.equals(u.getToken(), token)).findAny().orElse(null);
        if (Objects.isNull(user)) {
            return;
        }
        // 用户金钱为负 破产
        if (user.getMoney() < 0) {
            map.stream().filter(grid -> StringUtils.equals(user.getToken(), grid.getOwner())).forEach(grid -> {
                grid.setRoomLevel(0);
                grid.setOwner(null);
                grid.setOwnerColor("white");
            });
            nextUser(user, StatusEnum.broke);
            addMessage(String.format("%s破产了", user.getName()));
        } else {
            nextUser(user, StatusEnum.waiting);
        }

    }

    @Override
    public void upgrade(RequestDTO requestDTO) {
        String token = requestDTO.getToken();
        User user = users.stream().filter(u -> StringUtils.equals(u.getToken(), token)).findAny().orElse(null);
        if (Objects.isNull(user)) {
            return;
        }
        asset = Collections.emptyList();
        Grid grid = map.get(user.getPosition());
        int cost = grid.getPrice();
        // 买地
        if (Objects.nonNull(grid.getOwner())) {
            cost = grid.getType() == GridEnum.estate ? grid.getDetail().get(6) : 0;
        }
        if (user.getMoney() < cost) {
            addMessage(String.format("%s剩余金钱%s，不足%s", user.getName(), user.getMoney(), cost));
            return;
        }
        if (Objects.isNull(grid.getOwner())) {
            user.setMoney(user.getMoney() - cost);
            grid.setOwnerColor(user.getColor());
            grid.setOwner(user.getToken());
            if (grid.getType() == GridEnum.station) {
                List<Grid> station = map.stream().filter(g -> g.getType() == GridEnum.station)
                        .filter(g -> StringUtils.equals(g.getOwner(), token)).collect(Collectors.toList());
                station.forEach(s -> s.setRoomLevel(station.size() - 1));
            }
            addMessage(String.format("%s花费%s购买了%s", user.getName(), cost, grid.getName()));
        } else {
            user.setMoney(user.getMoney() - cost);
            grid.setRoomLevel(grid.getRoomLevel() + 1);
            addMessage(String.format("%s花费%s在%s盖了一栋房子", user.getName(), cost, grid.getName()));
        }
        nextUser(user, StatusEnum.waiting);
    }

    @Override
    public synchronized void sell(RequestDTO requestDTO) {
        // 返回当前用户可以变卖的地产
        String token = requestDTO.getToken();
        User user = users.stream().filter(u -> StringUtils.equals(u.getToken(), token)).findAny().orElse(null);
        if (Objects.isNull(user)) {
            return;
        }
        if (StringUtils.isNotEmpty(requestDTO.getSellName())) {
            map.stream().filter(g -> StringUtils.equals(g.getName(), requestDTO.getSellName())).findAny().ifPresent(grid -> {
                if (StringUtils.equals(requestDTO.getSellType(), "room") && grid.getRoomLevel() >= 1) {
                    grid.setRoomLevel(grid.getRoomLevel() - 1);
                    user.setMoney(user.getMoney() + grid.getDetail().get(6) / 2);
                    addMessage(String.format("%s卖出了%s的一栋房子，获得%s元", user.getName(), grid.getName(), grid.getDetail().get(6) / 2));
                } else if (StringUtils.equals(requestDTO.getSellType(), "estate") && StringUtils.equals(token, grid.getOwner())){
                    user.setMoney(user.getMoney() + grid.getPrice() / 2);
                    grid.setOwnerColor("white");
                    grid.setOwner(null);
                    addMessage(String.format("%s抵押了%s，获得%s元", user.getName(), grid.getName(), grid.getPrice() / 2));
                }
            });
        }

        List<Grid> collect = map.stream().filter(g -> StringUtils.equals(g.getOwner(), user.getToken()))
                .sorted(Comparator.comparing(Grid::getType)).collect(Collectors.toList());
        if (collect.isEmpty()) {
            addMessage(String.format("%s没有可以变卖的资产", user.getName()));
        }
        user.setStatus(StatusEnum.active_selling);
        this.asset = collect;
    }

    private void getToEstate(User user, Grid grid, int num) {
        // 无主，可购买
        if (Objects.isNull(grid.getOwner())) {
            user.setStatus(StatusEnum.active_estate);
            addMessage(String.format("%s地价%s，是否购买？", grid.getName(), grid.getPrice()));
        }
        // 有主，别人的，扣钱
        else if (!StringUtils.equals(grid.getOwner(), user.getToken())) {
            int cost = 0;
            if (grid.getType() == GridEnum.livelihood) {
                cost = map.stream().filter(g -> g.getType() == GridEnum.livelihood)
                        .allMatch(g -> StringUtils.equals(g.getOwner(), user.getToken())) ? num * 100 : num * 10;
            } else {
                cost = grid.getDetail().get(grid.getRoomLevel());
            }
            User tempUser = users.stream().filter(u -> StringUtils.equals(u.getToken(), grid.getOwner())).findAny().orElse(null);
            assert tempUser != null;
            // 如果对方在监狱，不扣钱
            if (tempUser.getStatus() == StatusEnum.jailed) {
                addMessage(String.format("%s在监狱里，%s不用付钱", tempUser.getName(), user.getName()));
                nextUser(user, StatusEnum.waiting);
                return;
            }
            // 检查是不是集齐了颜色
            if (grid.getType() == GridEnum.estate && grid.getRoomLevel() == 0
                    && map.stream().filter(g -> StringUtils.equals(g.getColor(), grid.getColor()))
                    .allMatch(g -> StringUtils.equals(g.getOwner(), grid.getOwner()))) {
                cost = cost * 2;
            }
            user.setMoney(user.getMoney() - cost);
            tempUser.setMoney(tempUser.getMoney() + cost);
            addMessage(String.format("%s付%s给%s", user.getName(), cost, tempUser.getName()));
            nextUser(user, StatusEnum.waiting);
        } else {
            // 有主 自己的 可升级
            if (grid.getRoomLevel() == 5 || grid.getType() == GridEnum.station || grid.getType() == GridEnum.livelihood) {
                // 最高级了 跳过
                nextUser(user, StatusEnum.waiting);
                return;
            }
            user.setStatus(StatusEnum.active_estate);
            addMessage(String.format("%s当前有%s个房子，是否再买一栋", grid.getName(), grid.getRoomLevel()));
        }

    }

    private void nextUser(User user, StatusEnum status) {
        // 如果用户的钱为负，不转移，状态变为资金不足，等待用户处理
        if (user.getMoney() < 0 && status != StatusEnum.broke) {
            user.setStatus(StatusEnum.active_insufficient);
            addMessage(String.format("%s 钱不够了，变卖家产吧", user.getName()));
            return;
        }
        user.setStatus(status);
        Node node = new Node();
        Node head = node;
        Node current = null;
        for (int i = 0; i < users.size(); i++) {
            User tempUser = users.get(i);
            if (i == 0) {
                node.user = tempUser;
            } else {
                node.next = new Node();
                node.next.user = tempUser;
                node = node.next;
            }
            if (StringUtils.equals(tempUser.getToken(), user.getToken())) {
                current = node;
            }
        }
        node.next = head;
        if (user.getStatus() == StatusEnum.broke) {
            users.remove(user);
        }
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
        return random.nextInt(13);
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
