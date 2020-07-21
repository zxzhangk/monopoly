package zhangzhixian.monopoly.controller;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import zhangzhixian.monopoly.model.Grid;
import zhangzhixian.monopoly.model.dto.MapDto;
import zhangzhixian.monopoly.service.MainService;

import java.util.List;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private MainService mainService;

    @Autowired
    private WebSocket webSocket;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<MapDto> helloWord() {
        return ResponseEntity.ok(mainService.getMap());
    }

    @RequestMapping("/sendAllWebSocket")
    public MapDto test() {
        webSocket.sendAllMessage(JSONObject.toJSONString(mainService.getMap()));
        return mainService.getMap();
    }
    @RequestMapping("/maps")
    public List<Grid> listMaps() {
        return mainService.getMap().getMap();
    }

    @RequestMapping("/test")
    public MapDto sendOneWebSocket() {
        mainService.getMap().getMap().get(0).setOwner(mainService.getMap().getUsers().get(0).getToken());
        mainService.getMap().getMap().get(0).setOwnerColor(mainService.getMap().getUsers().get(0).getColor());
        mainService.getMap().getMap().get(0).setRoomLevel(1);
        mainService.getMap().getMap().get(1).setRoomLevel(5);
        return mainService.getMap();
    }
}
