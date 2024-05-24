package com.example.websocketrelay.service;

import com.example.websocketrelay.handler.MyWebSocketServer;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;

@Service
public class RelayService {

    private final MyWebSocketServer myWebSocketServer;

    public RelayService(MyWebSocketServer myWebSocketServer) {
        this.myWebSocketServer = myWebSocketServer;
    }

    public void relayMessageToClients(ByteBuffer message) {
        myWebSocketServer.relayMessageToClients(message);
    }

    public void relayMessageToClients(String message) {
        myWebSocketServer.relayMessageToClients(message);
    }
}

