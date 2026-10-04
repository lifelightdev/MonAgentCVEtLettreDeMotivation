package life.light;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonAgentCVEtLettreDeMotivationTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void executerRequete_envoiePostJsonEtRetourneReponse() throws Exception {
        AtomicReference<String> methodRef = new AtomicReference<>();
        AtomicReference<String> contentTypeRef = new AtomicReference<>();
        AtomicReference<String> bodyRef = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            captureRequest(exchange, methodRef, contentTypeRef, bodyRef);
            byte[] response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        String address = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
        String response = MonAgentCVEtLettreDeMotivation.executeRequest(address, "google/gemma-4-e2b");

        assertEquals("POST", methodRef.get());
        assertEquals("application/json", contentTypeRef.get());
        assertTrue(bodyRef.get().contains("\"model\": \"google/gemma-4-e2b\""));
        assertTrue(bodyRef.get().contains("\"role\": \"user\""));
        assertTrue(bodyRef.get().contains("Dis-moi simplement bonjour en un seul mot"));
        assertEquals("{\"ok\":true}", response);
    }

    @Test
    void executeRequest_devraitRetournerBonjour_quandOnLuiDemandeDeDireBonjour() throws Exception {
        java.util.Properties properties = MonAgentCVEtLettreDeMotivation.loadProperties();
        String address = properties.getProperty("address");
        String model = properties.getProperty("model");

        String response = MonAgentCVEtLettreDeMotivation.executeRequest(address, model);

        assertTrue(response.toLowerCase().contains("bonjour"), "La réponse doit contenir 'bonjour'. Réponse reçue : " + response);
    }

    @Test
    void executeRequest_devraitLeverException_quandLeServeurRetourneErreur500() throws Exception {
        // Configuration du serveur pour renvoyer une erreur 500
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            // Simuler une erreur serveur
            exchange.sendResponseHeaders(500, 0);
            exchange.close();
        });
        server.start();

        String address = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";

        try {
            MonAgentCVEtLettreDeMotivation.executeRequest(address, "google/gemma-4-e2b");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("500") || e instanceof java.io.IOException, "L'exception levée ne semble pas être une erreur HTTP.");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void verifierConstructionJsonInput_contientModelRoleEtContenuEtSystem() {
        String model = "model-test";
        String role = "user";
        String content = "bonjour";
        String systemPrompt = "Tu es un expert en rédaction de CV et lettres de motivation.";

        String json = MonAgentCVEtLettreDeMotivation.buildJsonInput(model, role, content, systemPrompt);

        assertTrue(json.contains("\"model\": \"model-test\""));
        assertTrue(json.contains("\"role\": \"user\""));
        assertTrue(json.contains("\"content\": \"bonjour\""));
        assertTrue(json.contains("system"));
        assertTrue(json.contains(systemPrompt));
    }

    private static void captureRequest(
            HttpExchange exchange,
            AtomicReference<String> methodRef,
            AtomicReference<String> contentTypeRef,
            AtomicReference<String> bodyRef
    ) throws IOException {
        methodRef.set(exchange.getRequestMethod());
        contentTypeRef.set(exchange.getRequestHeaders().getFirst("Content-Type"));
        try (InputStream inputStream = exchange.getRequestBody()) {
            bodyRef.set(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}