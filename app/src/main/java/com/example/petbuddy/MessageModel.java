package com.example.petbuddy;

public class MessageModel {

    public String messageId;
    public String senderId;
    public String senderName;
    public String receiverId;
    public String text;
    public long timestamp;
    public boolean isRead;

    public MessageModel() {
        // Required empty constructor
    }

    public MessageModel(String senderId, String senderName, String receiverId, String text, long timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.text = text;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
