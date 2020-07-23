package zhangzhixian.monopoly.controller;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import zhangzhixian.monopoly.configuration.SpringContext;
import zhangzhixian.monopoly.model.User;
import zhangzhixian.monopoly.model.dto.RequestDTO;
import zhangzhixian.monopoly.service.MainService;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@ServerEndpoint(value = "/websocket/{token}")
@Slf4j
public class WebSocket {

    private Session session;

    private final MainService mainService = SpringContext.getBean(MainService.class);


    private static final CopyOnWriteArraySet<WebSocket> webSockets =new CopyOnWriteArraySet<>();
    private static final Map<String,Session> sessionPool = new HashMap<>();

    public WebSocket() {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
    }


    @OnOpen
    public void onOpen(Session session, @PathParam(value="token")String token) {
        User user = mainService.listUsers().stream().filter(u -> StringUtils.equals(token, u.getToken())).findAny().orElse(null);
        if (Objects.isNull(user)) {
            closeSession(session);
            return;
        }
        if (sessionPool.containsKey(token)) {
            Session tempSession = sessionPool.get(token);
            closeSession(tempSession);
        }
        if (user.getPosition() == -1) {
            user.setPosition(0);
        }
        this.session = session;
        webSockets.add(this);
        sessionPool.put(token, session);
        mainService.addMessage(String.format("%s 加入游戏", user.getName()));
        sendAllMessage(JSONObject.toJSON(mainService.getMap()).toString());
        log.info("【websocket消息】有新的连接，总数为:" + webSockets.size());
    }

    private void closeSession(Session session) {
        try {
            log.info("session closing [{}]", session.getId());
            session.close();
        } catch (IOException e) {
            log.error("close session error", e);
        }
    }

    @OnClose
    public void onClose() {
        webSockets.remove(this);
        System.out.println("【websocket消息】连接断开，总数为:"+webSockets.size());
    }

    @OnMessage
    public synchronized void onMessage(String message) {
        log.info("【websocket消息】收到客户端消息:{}", message);
        RequestDTO requestDTO = JSONObject.parseObject(message, RequestDTO.class);
        switch (requestDTO.getMethod()) {
            case sell:
                log.warn("sell");
                break;
            case transaction:
                log.warn("transaction");
                break;
            case pass:
                mainService.pass(requestDTO);
                sendAllMessage(JSONObject.toJSON(mainService.getMap()).toString());
                break;
            case roll:
                mainService.roll(requestDTO);
                sendAllMessage(JSONObject.toJSON(mainService.getMap()).toString());
                break;
            case upgrade:
                mainService.upgrade(requestDTO);
                sendAllMessage(JSONObject.toJSON(mainService.getMap()).toString());
                break;
            default:
                log.warn("todo");
                break;
        }
    }

    // 此为广播消息
    public void sendAllMessage(String message) {
        for(WebSocket webSocket : webSockets) {
            System.out.println("【websocket消息】广播消息:"+message);
            try {
                webSocket.session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 此为单点消息
    public void sendOneMessage(String token, String message) {
        Session session = sessionPool.get(token);
        if (session != null) {
            try {
                session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
