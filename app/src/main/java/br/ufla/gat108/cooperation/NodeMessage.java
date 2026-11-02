package br.ufla.gat108.cooperation;

/**
 * Representa uma mensagem trocada entre nós da frota ciberfísica (ex: heartbeats, alertas de queda, sincronização).
 */
public class NodeMessage {
    public static final String TYPE_HEARTBEAT = "HEARTBEAT";
    public static final String TYPE_FALL_ALERT = "FALL_ALERT";
    public static final String TYPE_CLOCK_SYNC = "CLOCK_SYNC";

    private final String senderId;
    private final String messageType;
    private final long timestampUtc;
    private final double latitude;
    private final double longitude;
    private final double accelerationPeak;
    private final String payload;

    public NodeMessage(String senderId, String messageType, long timestampUtc, double latitude, double longitude, double accelerationPeak, String payload) {
        this.senderId = senderId;
        this.messageType = messageType;
        this.timestampUtc = timestampUtc;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accelerationPeak = accelerationPeak;
        this.payload = payload;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getMessageType() {
        return messageType;
    }

    public long getTimestampUtc() {
        return timestampUtc;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAccelerationPeak() {
        return accelerationPeak;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "NodeMessage{" +
                "senderId='" + senderId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", timestampUtc=" + timestampUtc +
                ", lat=" + latitude +
                ", lon=" + longitude +
                ", peak=" + accelerationPeak +
                ", payload='" + payload + '\'' +
                '}';
    }
}
