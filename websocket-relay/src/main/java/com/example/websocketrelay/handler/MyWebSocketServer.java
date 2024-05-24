package com.example.websocketrelay.handler;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

@Component
public class MyWebSocketServer extends BinaryWebSocketHandler {

    private final Set<WebSocketSession> sessions = new HashSet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket connected");
        sessions.add(session);
    }

    @Override
    @SneakyThrows
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 텍스트 메시지를 다른 클라이언트로 중계
        for (WebSocketSession s : sessions) {
            if (s.isOpen() && !s.getId().equals(session.getId())) {
                s.sendMessage(message);
            }
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        // 바이너리 메시지를 다른 클라이언트로 중계
        for (WebSocketSession s : sessions) {
            if (s.isOpen() && !s.getId().equals(session.getId())) {
                s.sendMessage(message);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    public void relayMessageToClients(ByteBuffer message) {
        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(new BinaryMessage(message));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void relayMessageToClients(String message) {
        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
