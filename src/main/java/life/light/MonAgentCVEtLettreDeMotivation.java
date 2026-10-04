package life.light;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

public class MonAgentCVEtLettreDeMotivation {
    private static final System.Logger logger = System.getLogger(MonAgentCVEtLettreDeMotivation.class.getName());
    private static final String DEFAULT_ROLE = "user";
    private static final String DEFAULT_CONTENT = "Dis-moi simplement bonjour en un seul mot et en Francais";

    static void main() {
        try {
            Properties properties = loadProperties();
            String response = executeRequest(properties.getProperty("address"), properties.getProperty("model"));
            logger.log(INFO, "Réponse de l'IA : \n" + response);
        } catch (Exception e) {
            logger.log(ERROR, e.getMessage());
        }
    }

    static Properties loadProperties() throws Exception {
        Properties properties = new Properties();
        try (InputStream inputStream = MonAgentCVEtLettreDeMotivation.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {
            properties.load(Objects.requireNonNull(inputStream, "application.properties introuvable"));
        }
        return properties;
    }

    static String buildJsonInput(String model, String role, String content, String system) {
        return """
            {
              "model": "%s",
              "messages": [
                { "role": "%s", "content": "%s" }
              ],
              "system": "%s",
              "temperature": 0.7,
              "max_tokens": 200
            }
            """.formatted(model, role, content, system);
    }

    static String executeRequest(String address, String model) throws Exception {
        URL url = new URI(address).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        String systemPrompt = "Tu es un expert en rédaction de CV et lettres de motivation.";
        String jsonInput = buildJsonInput(model, DEFAULT_ROLE, DEFAULT_CONTENT, systemPrompt);

        // Étape 1 : Envoyer le corps de la requête
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonInput.getBytes(StandardCharsets.UTF_8));
        }

        // Étape 2 : Vérifier le code de réponse HTTP
        int responseCode = conn.getResponseCode();

        if (responseCode >= 200 && responseCode < 300) {
            // Succès : Lire le flux de la réponse
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }
                return response.toString();
            }
        } else {
            // Échec : Lire le flux d'erreur si disponible
            String errorResponse = "";
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                errorResponse = br.lines().reduce("", (acc, line) -> acc + line + "\n");
            } catch (Exception e) {
                // Ignorer si le flux d'erreur n'est pas disponible ou lisible
            }
            throw new RuntimeException("Erreur HTTP " + responseCode + ": " + errorResponse, new RuntimeException("HTTP Error"));
        }
    }
}