package nl.knmi.geoweb.backend.services;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServiceResult {
    private List<String> messages;
    private ServiceResultStatus status;
    private Integer code;
    private JSONObject payload;

    public ServiceResult() {
        this.messages=new ArrayList<>();
        this.status=ServiceResultStatus.ERR;
    }

    public String getStatus() {
        return status.toString();
    }

    public void setStatus(String status) {
        this.status = ServiceResultStatus.valueOf(status);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }

    public String[] getMessagesAsStrings() {
        return messages.toArray(new String[0]);
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(String[] messages) {
        this.messages.addAll(Arrays.asList(messages));
    }

    public void addMessage(String message) {
        messages.add(message);
    }
}
