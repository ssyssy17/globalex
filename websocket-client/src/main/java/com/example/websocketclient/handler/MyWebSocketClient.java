package com.example.websocketclient.handler;

import com.example.websocketclient.response.MarketPriceResponse;
import com.example.websocketclient.service.Summary;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    private final Summary summary = new Summary();
    private final Gson gson = new GsonBuilder().create();

    private WebSocketClient client;
    private WebSocketSession currentSession;



    private long sequence = 0;

    public MyWebSocketClient() {
        this.client = new StandardWebSocketClient();
    }

    @PostConstruct
    public void connect() throws URISyntaxException {
        URI uri = new URI(WEBSOCKET_URL); // URI 생성
        // WebSocket 연결 설정 및 핸들러 등록
        client.execute(this, new WebSocketHttpHeaders(), uri);
//        client.doHandshake(this, WEBSOCKET_URL).addCallback(
//                result -> {
//                    this.currentSession = result;
//                    System.out.println("Connected to WebSocket server");
//                },
//                ex -> System.err.println("Failed to connect to WebSocket server: " + ex.getMessage())
//        );
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket connected");
        this.currentSession = session;
        session.sendMessage(new TextMessage(CHANNEL));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        long dataSeq = Long.parseLong(message.getPayload());
        if (sequence != 0) {
            if (sequence != dataSeq - 1) {
                summary.incrementLose();
            }
        }
        sequence = dataSeq;
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
            MarketPriceResponse marketPriceResponse = gson.fromJson(decompressedData, MarketPriceResponse.class);
//            log.info("marketPriceResponse: " + marketPriceResponse);
            if (marketPriceResponse.getData() != null) {
                summary.increment(marketPriceResponse.getEventTime());
            }
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
            // 초기화
            summary.init();
//            client.doHandshake(this, WEBSOCKET_URL);
            URI uri = new URI(WEBSOCKET_URL); // URI 생성
            // WebSocket 연결 설정 및 핸들러 등록
            client.execute(this, new WebSocketHttpHeaders(), uri);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
