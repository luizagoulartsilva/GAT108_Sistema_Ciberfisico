package br.ufla.gat108;

import br.ufla.gat108.cooperation.NodeCoordinator;
import br.ufla.gat108.cooperation.NodeMessage;
import br.ufla.gat108.security.AuthenticatedCipher;
import br.ufla.gat108.security.KeyStoreManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAndCooperationTest {

    @Test
    void testKeyStoreManagerInitialization() {
        assertNotNull(KeyStoreManager.getOrCreateSecretKey(), "SecretKey deve ser gerada ou recuperada com sucesso");
    }

    @Test
    void testAuthenticatedCipherRoundtrip() throws Exception {
        String originalMessage = "Alerta: Trabalhador imobilizado no Posto 4 (Coordenadas sensíveis)";
        String encryptedBase64 = AuthenticatedCipher.encryptString(originalMessage);

        assertNotNull(encryptedBase64);
        assertNotEquals(originalMessage, encryptedBase64);

        String decryptedMessage = AuthenticatedCipher.decryptString(encryptedBase64);
        assertEquals(originalMessage, decryptedMessage);
    }

    @Test
    void testAuthenticatedCipherDynamicIv() throws Exception {
        String plaintext = "Dados confidenciais de sensor";
        byte[] cipher1 = AuthenticatedCipher.encrypt(plaintext.getBytes());
        byte[] cipher2 = AuthenticatedCipher.encrypt(plaintext.getBytes());

        // Com IV dinâmico, cifragem do mesmo texto deve produzir resultados diferentes
        assertFalse(java.util.Arrays.equals(cipher1, cipher2));

        // Ambos devem decifrar corretamente
        assertEquals(plaintext, new String(AuthenticatedCipher.decrypt(cipher1)));
        assertEquals(plaintext, new String(AuthenticatedCipher.decrypt(cipher2)));
    }

    @Test
    void testAuthenticatedCipherTamperingDetection() {
        assertThrows(Exception.class, () -> {
            String plaintext = "Dados integros";
            byte[] cipherBytes = AuthenticatedCipher.encrypt(plaintext.getBytes());

            // Corrompe um byte do ciphertext/tag
            cipherBytes[cipherBytes.length - 1] ^= 0x01;

            // Deve falhar na autenticação GCM e lançar exceção
            AuthenticatedCipher.decrypt(cipherBytes);
        });
    }

    @Test
    void testNodeCoordinatorHeartbeatAndPeers() {
        NodeCoordinator coordinator = new NodeCoordinator("Node-A");
        assertEquals("Node-A", coordinator.getLocalNodeId());

        long now = System.currentTimeMillis();
        coordinator.registerPeerHeartbeat("Node-B", now);

        int activePeers = coordinator.getActivePeerCount(now + 1000, 5000);
        assertEquals(1, activePeers);

        // Expirado
        int expiredPeers = coordinator.getActivePeerCount(now + 6000, 5000);
        assertEquals(0, expiredPeers);
    }

    @Test
    void testClockDriftCalculation() {
        NodeCoordinator coordinator = new NodeCoordinator("Node-A");
        long peerTime = 1000500L;
        long localTime = 1000000L;

        long drift = coordinator.calculateClockDrift(peerTime, localTime);
        assertEquals(500L, drift);
    }

    @Test
    void testAlertFusionConsolidation() {
        NodeCoordinator coordinator = new NodeCoordinator("Node-A");

        long now = System.currentTimeMillis();
        NodeMessage alert1 = new NodeMessage(
                "Node-B",
                NodeMessage.TYPE_FALL_ALERT,
                now,
                -21.2284, -44.9911,
                15.5,
                "Queda detectada"
        );

        NodeMessage alert2Nearby = new NodeMessage(
                "Node-C",
                NodeMessage.TYPE_FALL_ALERT,
                now + 2000, // 2 segundos depois
                -21.2285, -44.9912, // Muito próximo (< 20 metros)
                16.2,
                "Queda corroborada"
        );

        NodeMessage alert3Far = new NodeMessage(
                "Node-D",
                NodeMessage.TYPE_FALL_ALERT,
                now,
                -22.0000, -45.0000, // Longe (> 50 km)
                18.0,
                "Queda distante"
        );

        // Primeiro alerta não deve ser consolidado (é o pioneiro)
        assertFalse(coordinator.shouldConsolidateAlert(alert1, 50.0, 10000L));

        // Segundo alerta (próximo no espaço e tempo) deve ser consolidado (ignorado como novo alarme isolado)
        assertTrue(coordinator.shouldConsolidateAlert(alert2Nearby, 50.0, 10000L));

        // Terceiro alerta (longe) não deve ser consolidado (é um evento independente)
        assertFalse(coordinator.shouldConsolidateAlert(alert3Far, 50.0, 10000L));

        List<NodeMessage> alerts = coordinator.getRecentAlerts();
        assertEquals(2, alerts.size()); // alert1 e alert3Far armazenados
    }
}
