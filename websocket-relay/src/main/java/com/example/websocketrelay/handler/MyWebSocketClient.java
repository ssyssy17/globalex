package com.example.websocketrelay.handler;

import com.example.websocketrelay.service.RelayService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

@Slf4j
@Component
public class MyWebSocketClient extends BinaryWebSocketHandler {

    @Value("${websocket.url}")
    private String WEBSOCKET_URL;
    private static final String CHANNEL = "{\"id\":\"24dd0e35-56a4-4f7a-af8a-394c7060909c\",\"reqType\": \"sub\",\"dataType\":\"BTC-USDT@markPrice\"}";

    private WebSocketClient client;
    private WebSocketSession currentSession;
    private final RelayService relayService;

    private long SEQUENCE = 0;

    public MyWebSocketClient(RelayService relayService) {
        this.client = new StandardWebSocketClient();
        this.relayService = relayService;
    }

    @PostConstruct
    public void connect() throws URISyntaxException {
//        client.doHandshake(this, WEBSOCKET_URL);
        URI uri = new URI(WEBSOCKET_URL); // URI 생성
        // WebSocket 연결 설정 및 핸들러 등록
        client.execute(this, new WebSocketHttpHeaders(), uri);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket connected");
        this.currentSession = session;
        session.sendMessage(new TextMessage(CHANNEL));
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        byte[] compressedData = message.getPayload().array();
        String decompressedData = decompressGzip(compressedData);
//        System.out.println("Received: " + decompressedData);
        if ("Ping".equals(decompressedData)) {
            session.sendMessage(new TextMessage("Pong"));
//            System.out.println("Pong");
        } else {
            // 데이터 유실 확인용 sequence
            relayService.relayMessageToClients(""+SEQUENCE++);
            // 받은 데이터 relay
            relayService.relayMessageToClients(message.getPayload());
        }
    }

    private String decompressGzip(byte[] compressedData) throws IOException {
        try (InputStream byteStream = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipStream = new GZIPInputStream(byteStream)) {
            StringBuilder outStr = new StringBuilder();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = gzipStream.read(buffer)) > 0) {
                outStr.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
            }
            return outStr.toString();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("Transport error: " + exception.getMessage());
        reconnect();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("Connection closed: " + status);
        reconnect();
    }

    private void reconnect() {
        try {
            if (currentSession != null && currentSession.isOpen()) {
                log.error("connection closed");
                currentSession.close();
            }
            Thread.sleep(5000); // 재연결 전에 잠시 대기
//            client.doHandshake(this, WEBSOCKET_URL);
            URI uri = new URI(WEBSOCKET_URL); // URI 생성
            // WebSocket 연결 설정 및 핸들러 등록
            client.execute(this, new WebSocketHttpHeaders(), uri);
        } catch (InterruptedException | IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
