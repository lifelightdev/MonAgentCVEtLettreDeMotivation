package life.light;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

public class MonAgentCVEtLettreDeMotivation {
    private static final System.Logger logger = System.getLogger(MonAgentCVEtLettreDeMotivation.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEFAULT_ROLE = "user";
    private static final String DEFAULT_CONTENT = "Dis-moi simplement bonjour en un seul mot et en Francais";
    private static final double RESPONSE_TEMPERATURE = 0.2;

    static void main() {
        try {
            String response = run(loadProperties());
            logger.log(INFO, "Réponse de l'IA : \n" + response);
        } catch (Exception e) {
            logger.log(ERROR, e.getMessage());
        }
    }

    static String run(Properties properties) throws Exception {
        String cvPath = properties.getProperty("cv.path");
        String jobOfferPath = properties.getProperty("job.offer.path");

        String response;
        if (jobOfferPath == null || jobOfferPath.isBlank()) {
            response = executeRequestAvecFichierWord(
                    properties.getProperty("address"),
                    properties.getProperty("model"),
                    cvPath
            );
        } else {
            String jobOfferContent = readTextFile(jobOfferPath);
            response = executeRequestAvecFichierWord(
                    properties.getProperty("address"),
                    properties.getProperty("model"),
                    cvPath,
                    jobOfferContent
            );
        }

        writeOutputFilesIfConfigured(response, properties, cvPath);
        return response;
    }

    private static void writeOutputFilesIfConfigured(
            String response,
            Properties properties,
            String cvTemplatePath
    ) throws IOException {
        String cvOutputPath = properties.getProperty("cv.output.path");
        String letterOutputPath = properties.getProperty("letter.output.path");
        boolean hasCvOutputPath = cvOutputPath != null && !cvOutputPath.isBlank();
        boolean hasLetterOutputPath = letterOutputPath != null && !letterOutputPath.isBlank();
        if (hasCvOutputPath != hasLetterOutputPath) {
            throw new IllegalArgumentException(
                    "Les propriétés cv.output.path et letter.output.path doivent être définies ensemble."
            );
        }
        if (!hasCvOutputPath) {
            return;
        }

        GeneratedDocuments documents = splitGeneratedDocuments(response);
        Path cvOutput = Path.of(cvOutputPath);
        if (cvOutput.toAbsolutePath().normalize()
                .equals(Path.of(cvTemplatePath).toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("cv.output.path doit être différent de cv.path pour préserver le CV original.");
        }
        writeCvFile(cvOutput, documents.cv(), Path.of(cvTemplatePath));
        writeTextFile(Path.of(letterOutputPath), documents.letter());
    }

    private static GeneratedDocuments splitGeneratedDocuments(String response) {
        String[] lines = response.split("\\R");
        int cvHeadingIndex = -1;
        int letterHeadingIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            String heading = normalizeHeading(lines[i]);
            if (heading.equalsIgnoreCase("CV adapté")) {
                cvHeadingIndex = i;
            } else if (heading.equalsIgnoreCase("Lettre de motivation")) {
                letterHeadingIndex = i;
            }
        }
        if (cvHeadingIndex < 0 || letterHeadingIndex <= cvHeadingIndex) {
            throw new IllegalArgumentException(
                    "La réponse doit contenir les sections « CV adapté » puis « Lettre de motivation »."
            );
        }

        String cv = String.join("\n", Arrays.copyOfRange(lines, cvHeadingIndex + 1, letterHeadingIndex)).strip();
        String letter = String.join("\n", Arrays.copyOfRange(lines, letterHeadingIndex + 1, lines.length)).strip();
        if (cv.isEmpty() || letter.isEmpty()) {
            throw new IllegalArgumentException("Les sections générées du CV et de la lettre doivent être non vides.");
        }
        return new GeneratedDocuments(cv, letter);
    }

    private static String normalizeHeading(String line) {
        return line.strip()
                .replaceAll("^[#*\\s]+|[#*\\s]+$", "")
                .replaceFirst("^\\d+[.)]\\s*", "")
                .replaceAll(":$", "")
                .strip();
    }

    private static void writeTextFile(Path outputPath, String content) throws IOException {
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (outputPath.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".docx")) {
            writeWordFile(outputPath, content);
        } else {
            Files.writeString(outputPath, content, StandardCharsets.UTF_8);
        }
    }

    private static void writeCvFile(Path outputPath, String content, Path templatePath) throws IOException {
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (outputPath.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".docx")) {
            writeWordFile(outputPath, content, templatePath);
        } else {
            Files.writeString(outputPath, content, StandardCharsets.UTF_8);
        }
    }

    private static void writeWordFile(Path outputPath, String content) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             OutputStream outputStream = Files.newOutputStream(outputPath)) {
            for (String line : content.split("\\R", -1)) {
                document.createParagraph().createRun().setText(line);
            }
            document.write(outputStream);
        }
    }

    private static void writeWordFile(Path outputPath, String content, Path templatePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(templatePath);
             XWPFDocument document = new XWPFDocument(inputStream);
             OutputStream outputStream = Files.newOutputStream(outputPath)) {
            String[] lines = content.split("\\R", -1);
            List<XWPFParagraph> paragraphs = new ArrayList<>();
            collectBodyParagraphs(paragraphs, document.getBodyElements());
            for (int i = 0; i < Math.max(lines.length, paragraphs.size()); i++) {
                XWPFParagraph paragraph = i < paragraphs.size()
                        ? paragraphs.get(i)
                        : document.createParagraph();
                replaceParagraphText(paragraph, i < lines.length ? lines[i] : "");
            }
            document.write(outputStream);
        }
    }

    private static void collectBodyParagraphs(List<XWPFParagraph> paragraphs, List<IBodyElement> bodyElements) {
        for (IBodyElement bodyElement : bodyElements) {
            if (bodyElement instanceof XWPFParagraph paragraph) {
                paragraphs.add(paragraph);
            } else if (bodyElement instanceof XWPFTable table) {
                for (var row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        collectBodyParagraphs(paragraphs, cell.getBodyElements());
                    }
                }
            }
        }
    }

    private static void replaceParagraphText(XWPFParagraph paragraph, String text) {
        List<XWPFRun> runs = paragraph.getRuns();
        XWPFRun firstRun = runs.isEmpty() ? paragraph.createRun() : runs.getFirst();
        firstRun.setText(text, 0);
        for (int textIndex = firstRun.getCTR().sizeOfTArray() - 1; textIndex > 0; textIndex--) {
            firstRun.getCTR().removeT(textIndex);
        }
        for (int runIndex = runs.size() - 1; runIndex > 0; runIndex--) {
            paragraph.removeRun(runIndex);
        }
    }

    private record GeneratedDocuments(String cv, String letter) {}

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
              "temperature": %s,
              "max_tokens": 2000
            }
            """.formatted(model, role, escapeJson(content), escapeJson(system), RESPONSE_TEMPERATURE);
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
        return construireMessageUtilisateur(cvContent, "");
    }

    static String construireMessageUtilisateur(String cvContent, String jobOffer) {
        String message = "Voici mon CV :\n" + cvContent;
        if (jobOffer != null && !jobOffer.isBlank()) {
            message += "\n\nVoici l'offre d'emploi :\n" + jobOffer;
        }
        message += """

                À partir de mon CV et de l'offre d'emploi, rédige les deux sections suivantes.
                Respecte exactement ces titres, chacun seul sur sa ligne et dans cet ordre, sans numéro ni autre titre :

                CV adapté
                Propose une version concise de mon CV adaptée à l'offre, sans inventer d'expérience, de compétence ou de diplôme.
                Reprends exactement le nom et les informations personnelles présents dans le CV. Ne les omets pas et ne les remplace pas par un nom générique.

                Lettre de motivation
                Rédige une lettre de motivation personnalisée en faisant le lien entre mon parcours et les besoins de l'offre.
                N'ajoute aucune introduction avant « CV adapté » ni aucun autre titre de section entre ces deux sections.
                """;
        return message;
    }

    static String executeRequest(String address, String model) throws Exception {
        return executeRequestAvecContenu(address, model, DEFAULT_CONTENT);
    }

    static String executeRequestAvecFichierWord(String address, String model, String cvPath) throws Exception {
        String cvContent = readWordFile(cvPath);
        return executeRequest(address, model, cvContent);
    }

    static String executeRequestAvecFichierWord(String address, String model, String cvPath, String jobOffer) throws Exception {
        String cvContent = readWordFile(cvPath);
        return executeRequest(address, model, cvContent, jobOffer);
    }

    static String executeRequest(String address, String model, String cvContent) throws Exception {
        return executeRequestAvecContenu(address, model, construireMessageUtilisateur(cvContent));
    }

    static String executeRequest(String address, String model, String cvContent, String jobOffer) throws Exception {
        return executeRequestAvecContenu(address, model, construireMessageUtilisateur(cvContent, jobOffer));
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
                String responseBody = response.toString();
                JsonNode contentNode = objectMapper.readTree(responseBody)
                        .path("choices")
                        .path(0)
                        .path("message")
                        .path("content");
                return contentNode.isTextual() ? contentNode.textValue() : responseBody;
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
        try (InputStream inputStream = Files.newInputStream(Path.of(testFilePath));
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (Exception e) {
            logger.log(ERROR, "Erreur lors de la lecture du fichier Word : " + e.getMessage());
            return "";
        }
    }

    static String readTextFile(String filePath) {
        try (InputStream inputStream = new FileInputStream(filePath);
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!content.isEmpty()) {
                    content.append(System.lineSeparator());
                }
                content.append(line);
            }
            return content.toString();
        } catch (Exception e) {
            logger.log(ERROR, "Erreur lors de la lecture du fichier texte : " + e.getMessage());
            return "";
        }
    }
}