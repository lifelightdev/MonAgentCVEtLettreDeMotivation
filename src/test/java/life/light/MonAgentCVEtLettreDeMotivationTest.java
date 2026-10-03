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
    void buildJsonInput_shouldContainModelRoleAndContent() {
        String json = MonAgentCVEtLettreDeMotivation.buildJsonInput("model-test", "user", "bonjour");

        assertTrue(json.contains("\"model\": \"model-test\""));
        assertTrue(json.contains("\"role\": \"user\""));
        assertTrue(json.contains("\"content\": \"bonjour\""));
    }

    @Test
    void executeRequest_shouldSendPostJsonAndReturnResponse() throws Exception {
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