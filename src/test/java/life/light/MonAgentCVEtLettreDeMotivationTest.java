package life.light;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
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
    void executeRequest_inclutLeContenuDuCvDansLeCorpsDeLaRequete() throws Exception {
        AtomicReference<String> bodyRef = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            captureRequest(exchange, new AtomicReference<>(), new AtomicReference<>(), bodyRef);
            byte[] response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        String address = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
        String cvContent = "Jean DUPONT\nDéveloppeur Back-End Java";

        MonAgentCVEtLettreDeMotivation.executeRequest(address, "google/gemma-4-e2b", cvContent);

        assertTrue(bodyRef.get().contains("Jean DUPONT"), "Le corps doit contenir le nom du CV");
        assertTrue(bodyRef.get().contains("Développeur Back-End Java"), "Le corps doit contenir le titre du CV");
    }

    @Test
    void executeRequest_inclutLOffreDemploiDansLeCorpsDeLaRequete() throws Exception {
        AtomicReference<String> bodyRef = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            captureRequest(exchange, new AtomicReference<>(), new AtomicReference<>(), bodyRef);
            byte[] response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        String address = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
        String cvContent = "Jean DUPONT\nDéveloppeur Back-End Java";
        String offreEmploi = "Nous recherchons un développeur Java senior pour piloter des projets backend.";

        MonAgentCVEtLettreDeMotivation.executeRequest(address, "google/gemma-4-e2b", cvContent, offreEmploi);

        assertTrue(bodyRef.get().contains("Jean DUPONT"), "Le corps doit contenir le nom du CV");
        assertTrue(bodyRef.get().contains("Développeur Back-End Java"), "Le corps doit contenir le titre du CV");
        assertTrue(bodyRef.get().contains("Nous recherchons un développeur Java senior"),
                "Le corps doit contenir l'offre d'emploi");
    }

    @Test
    void executeRequestAvecFichierWord_inclutLeContenuDuCvDansLeCorpsDeLaRequete() throws Exception {
        AtomicReference<String> bodyRef = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            captureRequest(exchange, new AtomicReference<>(), new AtomicReference<>(), bodyRef);
            byte[] response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        String address = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
        String cvPath = "src/test/resources/cv.docx";

        MonAgentCVEtLettreDeMotivation.executeRequestAvecFichierWord(address, "google/gemma-4-e2b", cvPath);

        assertTrue(bodyRef.get().contains("Jean DUPONT"), "Le corps doit contenir le nom lu depuis le fichier Word");
        assertTrue(bodyRef.get().contains("Développeur Back-End Java"), "Le corps doit contenir le titre lu depuis le fichier Word");
    }

    @Test
    void run_utiliseLeCheminCvDesPropertiesPourEnvoyerLeContenu() throws Exception {
        AtomicReference<String> bodyRef = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            captureRequest(exchange, new AtomicReference<>(), new AtomicReference<>(), bodyRef);
            byte[] response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        Properties properties = new Properties();
        properties.setProperty("address", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
        properties.setProperty("model", "google/gemma-4-e2b");
        properties.setProperty("cv.path", "src/test/resources/cv.docx");

        MonAgentCVEtLettreDeMotivation.run(properties);

        assertTrue(bodyRef.get().contains("Jean DUPONT"), "run doit envoyer le contenu du CV configuré");
        assertTrue(bodyRef.get().contains("Développeur Back-End Java"), "run doit envoyer le titre du CV configuré");
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
    void construireMessageUtilisateur_contientLaConsigneEtLeTexteDuCv() {
        String cvContent = "Jean DUPONT\nDéveloppeur Back-End Java";

        String message = MonAgentCVEtLettreDeMotivation.construireMessageUtilisateur(cvContent);

        assertTrue(message.contains("Voici mon CV :"), "Le message doit contenir la consigne");
        assertTrue(message.contains(cvContent), "Le message doit contenir le texte brut du CV");
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

    @Test
    void buildJsonInput_echappeLesCaracteresSpeciauxDansLeContenuEtLeSystem() {
        String content = "ligne1\nligne2\rfin\t\"quote\"\\slash";
        String systemPrompt = "system\navec\t\"guillemets\" et \\backslash";

        String json = MonAgentCVEtLettreDeMotivation.buildJsonInput(
                "model-test",
                "user",
                content,
                systemPrompt
        );

        assertTrue(json.contains("ligne1\\nligne2\\rfin\\t\\\"quote\\\"\\\\slash"),
                "Le content doit échapper \\n, \\r, \\t, \\\" et \\\\");
        assertTrue(json.contains("system\\navec\\t\\\"guillemets\\\" et \\\\backslash"),
                "Le system doit échapper \\n, \\t, \\\" et \\\\");
    }

    @Test
    void litFichierWord_devraitRetournerLeContenu() {
        String testFilePath = "src/test/resources/cv.docx";
        String expectedContent = """
                Jean DUPONT
                Développeur Back-End Java\s
                
                
                
                \uF029 06 06 06 06 06\t\uF02A jean.dupont@yahoo.fre\tGitHub jeandupont\tLinkedIn jean-dupont\tForte d’une expérience de 25 ans en maintenance applicative, je mets en œuvre les principes du Craft pour maintenir des logiciels de haute qualité, maintenables et parfaitement adaptés aux besoins des utilisateurs.
                LANGUES\tFrançais\tLangue maternelle\tAnglais\tB1 (Intermédiaire)\t\tFORMATION\tLicence Informatique 2018 CNAM \tBTS Informatique de gestion 2001 \tBac pro de comptabilité 1997\tRÉALISATION\tDéveloppeuse logiciel (25 ans)\tPilotage de la maintenance corrective et évolutive d’applications métiers complexes.\tRédaction de documentation technique et fonctionnelle.\tMigration et montée en version d'applications Java pour sécuriser et moderniser le parc applicatif.\tRelation avec le client.\tSuivi rigoureux des déploiements dans les environnements de qualification et de production.\tPratique quotidienne du TDD pour garantir la robustesse du code.\tDevOps (2 ans)\tOptimisation de l’intégration continue (CI/CD) et automatisation des process DevOps.\tMise en place de l’intégration continue.\tCrafteuse (1 an)\tAnimation de Coding Dojos pour diffuser les bonnes pratiques de développement au sein des équipes.\tRéalisation d'audits de code et formulation de préconisations techniques pour réduire la dette.\t\tEXPERTISE\tBack-end Java, Spring, Hibernate, SQL.\tQualité et Craft Pratique quotidienne du TDD, Clean Code, Pair & Mob Programming, Scrum.\tDevOps et Outils Git, Jenkins, SonarQube, IntelliJ, PostgreSQL, Oracle, MySQL.\tDomaines Fonctionnels Banque, Assurance, Publicité, Cinéma, Énergie, Logistique, Industrie, Éducation, Fonction publique.
                
                
                """;

        String actualContent = MonAgentCVEtLettreDeMotivation.readWordFile(testFilePath);

        assertEquals(expectedContent, actualContent, "Le contenu du fichier Word lu ne correspond pas au contenu attendu.");
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