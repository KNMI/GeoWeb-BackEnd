package nl.knmi.geoweb.backend.triggers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class WebSocketListener {

    @Autowired
    private SimpMessagingTemplate webSocket;

    @Async
    @MessageMapping("/websocket")
    @SendTo("/trigger/messages")
    public void pushMessageToWebSocket (String message){

        webSocket.convertAndSend("/trigger/messages", message);

    }

}