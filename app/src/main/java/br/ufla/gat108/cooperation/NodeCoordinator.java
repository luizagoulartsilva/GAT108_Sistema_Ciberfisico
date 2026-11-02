package br.ufla.gat108.cooperation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Coordena a rede ad-hoc de nós cooperativos, gerencia heartbeat de peers,
 * calcula deriva de relógio (clock drift) e executa fusão de alertas de queda próximos.
 */
public class NodeCoordinator {
    private final String localNodeId;
    private final ConcurrentMap<String, Long> peerLastSeenMap = new ConcurrentHashMap<>();
    private final List<NodeMessage> recentAlerts = new ArrayList<>();

    public NodeCoordinator(String localNodeId) {
        this.localNodeId = localNodeId;
    }

    public String getLocalNodeId() {
        return localNodeId;
    }

    /**
     * Registra o batimento (heartbeat) de um nó peer.
     */
    public void registerPeerHeartbeat(String peerId, long timestampUtc) {
        peerLastSeenMap.put(peerId, timestampUtc);
    }

    public int getActivePeerCount(long currentTimestampUtc, long timeoutThresholdMs) {
        int active = 0;
        for (long lastSeen : peerLastSeenMap.values()) {
            if (currentTimestampUtc - lastSeen <= timeoutThresholdMs) {
                active++;
            }
        }
        return active;
    }

    /**
     * Calcula a deriva de relógio (clock drift) em milissegundos entre o peer e o nó local.
     */
    public long calculateClockDrift(long peerTimestampUtc, long localTimestampUtc) {
        return peerTimestampUtc - localTimestampUtc;
    }

    /**
     * Realiza a fusão de alertas de queda (Alert Fusion):
     * Verifica se o alerta recebido de um peer é próximo no tempo e no espaço (raio geográfico)
     * de um alerta já registrado, evitando duplicidade de chamados de socorro na frota.
     */
    public synchronized boolean shouldConsolidateAlert(NodeMessage incomingAlert, double maxDistanceMeters, long maxTimeWindowMs) {
        if (!NodeMessage.TYPE_FALL_ALERT.equals(incomingAlert.getMessageType())) {
            return false;
        }

        for (NodeMessage existing : recentAlerts) {
            // Se for do mesmo nó, ignora duplicação direta
            if (existing.getSenderId().equals(incomingAlert.getSenderId())) {
                continue;
            }

            // Verifica janela temporal
            long timeDiff = Math.abs(existing.getTimestampUtc() - incomingAlert.getTimestampUtc());
            if (timeDiff <= maxTimeWindowMs) {
                // Verifica proximidade espacial (aproximação em metros usando graus decimais aprox 111km/grau)
                double distanceMeters = calculateApproximateDistanceMeters(
                        existing.getLatitude(), existing.getLongitude(),
                        incomingAlert.getLatitude(), incomingAlert.getLongitude()
                );

                if (distanceMeters <= maxDistanceMeters) {
                    // Alerta já coberto por outro nó próximo! Consolida (não reenvia/dispara novo alarme isolado).
                    return true;
                }
            }
        }

        // Se não encontrou duplicado, armazena e retorna falso (novo alerta genuíno)
        recentAlerts.add(incomingAlert);
        return false;
    }

    public synchronized List<NodeMessage> getRecentAlerts() {
        return new ArrayList<>(recentAlerts);
    }

    /**
     * Calcula distância aproximada em metros entre dois pontos geográficos (Fórmula de Haversine simplificada).
     */
    public static double calculateApproximateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Raio da terra em metros
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
