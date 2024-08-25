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

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
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
            try {
                System.out.println("\nSending message to client: \n" + message);
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                e.printStackTrace();
                logger.warning("Failed to send message to client: " + e.getMessage());
            }
        });
    }
}

