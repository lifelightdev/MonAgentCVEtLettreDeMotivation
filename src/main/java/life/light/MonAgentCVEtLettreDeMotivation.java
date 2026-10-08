package life.light;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class MonAgentCVEtLettreDeMotivation {
    private static final System.Logger logger = System.getLogger(MonAgentCVEtLettreDeMotivation.class.getName());
    private static final String DEFAULT_ROLE = "user";
    private static final String DEFAULT_CONTENT = "Dis-moi simplement bonjour en un seul mot et en Francais";

    static void main() {
        try {
            String response = run(loadProperties());
            logger.log(INFO, "Réponse de l'IA : \n" + response);
        } catch (Exception e) {
            logger.log(ERROR, e.getMessage());
        }
    }

    static String run(Properties properties) throws Exception {
        return executeRequestAvecFichierWord(
                properties.getProperty("address"),
                properties.getProperty("model"),
                properties.getProperty("cv.path")
        );
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
            """.formatted(model, role, escapeJson(content), escapeJson(system));
    }

    static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    static String construireMessageUtilisateur(String cvContent) {
        return "Voici mon CV :\n" + cvContent;
    }

    static String executeRequest(String address, String model) throws Exception {
        return executeRequestAvecContenu(address, model, DEFAULT_CONTENT);
    }

    static String executeRequestAvecFichierWord(String address, String model, String cvPath) throws Exception {
        String cvContent = readWordFile(cvPath);
        return executeRequest(address, model, cvContent);
    }

    static String executeRequest(String address, String model, String cvContent) throws Exception {
        return executeRequestAvecContenu(address, model, construireMessageUtilisateur(cvContent));
    }

    private static String executeRequestAvecContenu(String address, String model, String content) throws Exception {
        URL url = new URI(address).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        String systemPrompt = "Tu es un expert en rédaction de CV et lettres de motivation.";
        String jsonInput = buildJsonInput(model, DEFAULT_ROLE, content, systemPrompt);

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

    public static String readWordFile(String testFilePath) {
        try {
            XWPFDocument document = new XWPFDocument(new FileInputStream(testFilePath));
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        } catch (Exception e) {
            logger.log(ERROR, "Erreur lors de la lecture du fichier Word : " + e.getMessage());
            return ""; 
        }
    }
}