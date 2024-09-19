package com.demo.myapp.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * @Author: Yupeng Li
 * @Date: 18/8/2024 14:52
 * @Description:
 */

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();//线程安全的list
    private final Logger logger = Logger.getLogger(WebSocketHandler.class.getName());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    public void sendMessageToClients(String message) {
        sessions.forEach(session -> {
            synchronized (session){//同步session, 防止多线程操作session
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    logger.warning("WebSocketHandler failed to send message to client: " + e.getMessage());
                    sessions.remove(session); // 移除无效的session
                }
            }
        });
    }
}

