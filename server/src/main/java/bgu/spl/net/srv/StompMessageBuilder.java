package bgu.spl.net.srv;

import java.util.Objects;

public class StompMessageBuilder {
    

    public StompMessageBuilder() {
    }
    public static String buildConnectedMessage(String version) {
        StringBuilder sb = new StringBuilder();
        sb.append("CONNECTED\n");
        sb.append("version:1.2\n");
        sb.append("\u0000\n"); // סוף ההודעה
        return sb.toString();
    }

    public static String buildSubscribeedMessage(String RecitId) {
        StringBuilder sb = new StringBuilder();
        sb.append("RECEIPT\n");
        sb.append("receipt - id:/").append(RecitId).append("\n");
        sb.append("\u0000\n"); // סוף ההודעה
        return sb.toString();
    }

    public static String buildUnsubscribeMessage(String id) {
        StringBuilder sb = new StringBuilder();
        sb.append("RECEIPT\n");
        sb.append("receipt - id:/").append(id).append("\n");
        sb.append("\u0000\n"); // סוף ההודעה
        return sb.toString();
    }

    public static String buildSendMessage(int ConectionID, int messageID, String destination, String messageBody) {
        StringBuilder sb = new StringBuilder();
        sb.append("MESSAGE\n");
        sb.append("subscription:/").append(ConectionID).append("\n");
        sb.append("message - id:/").append(messageID).append("\n");
        sb.append("destination:/").append(destination).append("\n");
        sb.append("\n"); // רווח לפני גוף ההודעה
        sb.append(messageBody).append("\n"); // גוף ההודעה
        sb.append("\u0000\n"); // סוף ההודעה
        return sb.toString();
    }

    public static String builderrotMessage(int messageID,String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("receipt - id:/").append(messageID).append("\n");
        sb.append("message:/").append(message).append("\n");
        sb.append("\u0000\n"); // סוף ההודעה
        return sb.toString();
    }
}