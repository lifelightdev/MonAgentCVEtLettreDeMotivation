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

    static String buildJsonInput(String model, String role, String content) {
        return """
                {
                  "model": "%s",
                  "messages": [
                    { "role": "%s", "content": "%s" }
                  ],
                  "temperature": 0.7,
                  "max_tokens": 200
                }
                """.formatted(model, role, content);
    }

    static String executeRequest(String address, String model) throws Exception {
        URL url = new URI(address).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String jsonInput = buildJsonInput(model, DEFAULT_ROLE, DEFAULT_CONTENT);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonInput.getBytes(StandardCharsets.UTF_8));
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
            return response.toString();
        }
    }
}
