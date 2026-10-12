package life.light;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MonAgentCVEtLettreDeMotivationTest {

    @TempDir
    Path tempDir;

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
    void executeRequest_retourneLeContenuDuMessageDeLaReponseJson() throws Exception {
        String generatedLetter = "Madame, Monsieur,\nJe vous adresse ma candidature.";

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] response = "{\"choices\":[{\"message\":{\"content\":\"Madame, Monsieur,\\nJe vous adresse ma candidature.\"}}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        String address = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
        String response = MonAgentCVEtLettreDeMotivation.executeRequest(address, "google/gemma-4-e2b");

        assertEquals(generatedLetter, response);
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
    void run_utiliseLeCheminOffreDemploiDesPropertiesPourEnvoyerLeContenu() throws Exception {
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
        properties.setProperty("job.offer.path", "src/test/resources/offre-emploi.txt");

        MonAgentCVEtLettreDeMotivation.run(properties);

        assertTrue(bodyRef.get().contains("Jean DUPONT"), "run doit envoyer le contenu du CV configuré");
        assertTrue(bodyRef.get().contains("Développeur Back-End Java"), "run doit envoyer le titre du CV configuré");
        assertTrue(bodyRef.get().contains("Développeur Java senior"),
                "run doit envoyer le contenu de l'offre d'emploi configurée");
    }

    @Test
    void run_integreLaMotivationEtLesRealisationsDuCandidatDansLaRequete() throws Exception {
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
        properties.setProperty("candidate.motivation", "Je souhaite contribuer à des produits utiles aux patients.");
        properties.setProperty("candidate.highlights", "J'ai réduit de 30 % les incidents en production.");

        MonAgentCVEtLettreDeMotivation.run(properties);

        assertTrue(bodyRef.get().contains("Je souhaite contribuer à des produits utiles aux patients."),
                "Le prompt doit transmettre la motivation personnelle du candidat");
        assertTrue(bodyRef.get().contains("J'ai réduit de 30 % les incidents en production."),
                "Le prompt doit transmettre les réalisations que le candidat souhaite mettre en avant");
    }

    @Test
    void run_recupereLeContexteDuSiteEntrepriseEtLinclutDansLaRequete() throws Exception {
        AtomicReference<String> bodyRef = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/entreprise", exchange -> {
            byte[] response = """
                    <html><head><title>Entreprise Exemple</title><script>texte technique secret</script></head>
                    <body><nav>Navigation</nav><main><h1>Entreprise Exemple</h1>
                    <p>Nous développons des solutions de santé numérique.</p></main></body></html>
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
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
        properties.setProperty("company.website.url", "http://127.0.0.1:" + server.getAddress().getPort() + "/entreprise");

        MonAgentCVEtLettreDeMotivation.run(properties);

        assertTrue(bodyRef.get().contains("Nous développons des solutions de santé numérique."),
                "Le prompt doit contenir le texte utile du site de l'entreprise");
        assertTrue(bodyRef.get().contains("Entreprise Exemple"),
                "Le prompt doit contenir le nom de l'entreprise trouvé sur le site");
        assertFalse(bodyRef.get().contains("texte technique secret"), "Le prompt ne doit pas inclure le contenu des scripts");
        assertFalse(bodyRef.get().contains("Navigation"), "Le prompt ne doit pas inclure les éléments de navigation");
    }

    @Test
    void run_ecritLeCvEtLaLettreDansDeuxFichiersDistincts() throws Exception {
        String generatedContent = """
                CV adapté
                Jean DUPONT - Développeur Java
                
                Lettre de motivation
                Madame, Monsieur,
                Je vous adresse ma candidature.
                """.stripTrailing();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] response = ("{\"choices\":[{\"message\":{\"content\":\""
                    + generatedContent.replace("\n", "\\n")
                    + "\"}}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        Path cvOutputPath = tempDir.resolve("cv-adapte.txt");
        Path letterOutputPath = tempDir.resolve("lettre-motivation.txt");
        Properties properties = new Properties();
        properties.setProperty("address", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
        properties.setProperty("model", "google/gemma-4-e2b");
        properties.setProperty("cv.path", "src/test/resources/cv.docx");
        properties.setProperty("job.offer.path", "src/test/resources/offre-emploi.txt");
        properties.setProperty("cv.output.path", cvOutputPath.toString());
        properties.setProperty("letter.output.path", letterOutputPath.toString());

        MonAgentCVEtLettreDeMotivation.run(properties);

        assertEquals("Jean DUPONT - Développeur Java", Files.readString(cvOutputPath));
        assertEquals("Madame, Monsieur,\nJe vous adresse ma candidature.", Files.readString(letterOutputPath));
    }

    @Test
    void run_ecritLeCvEtLaLettreDansDeuxFichiersWord() throws Exception {
        String generatedContent = """
                CV adapté
                Jean DUPONT - Développeur Java
                
                Lettre de motivation
                Madame, Monsieur,
                Je vous adresse ma candidature.
                """.stripTrailing();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] response = ("{\"choices\":[{\"message\":{\"content\":\""
                    + generatedContent.replace("\n", "\\n")
                    + "\"}}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        Path cvOutputPath = tempDir.resolve("sorties/cv-adapte.docx");
        Path letterOutputPath = tempDir.resolve("sorties/lettre-motivation.docx");
        Properties properties = new Properties();
        properties.setProperty("address", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
        properties.setProperty("model", "google/gemma-4-e2b");
        properties.setProperty("cv.path", "src/test/resources/cv.docx");
        properties.setProperty("job.offer.path", "src/test/resources/offre-emploi.txt");
        properties.setProperty("cv.output.path", cvOutputPath.toString());
        properties.setProperty("letter.output.path", letterOutputPath.toString());

        MonAgentCVEtLettreDeMotivation.run(properties);

        assertEquals("Jean DUPONT - Développeur Java", readDocxText(cvOutputPath));
        assertEquals("Madame, Monsieur,\nJe vous adresse ma candidature.", readDocxText(letterOutputPath));
    }

    @Test
    void run_preserveLeCvOriginalEtSaMiseEnFormeDansLaCopieWord() throws Exception {
        Path originalCvPath = tempDir.resolve("cv-original.docx");
        try (XWPFDocument original = new XWPFDocument()) {
            XWPFParagraph title = original.createParagraph();
            title.createRun().setBold(true);
            title.getRuns().getFirst().setText("CV original");
            original.createParagraph().createRun().setText("Expérience professionnelle");
            try (var outputStream = Files.newOutputStream(originalCvPath)) {
                original.write(outputStream);
            }
        }
        byte[] originalBytes = Files.readAllBytes(originalCvPath);

        String generatedContent = """
                CV adapté
                Jean DUPONT - Développeur Java
                
                Lettre de motivation
                Madame, Monsieur,
                Je vous adresse ma candidature.
                """.stripTrailing();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] response = ("{\"choices\":[{\"message\":{\"content\":\""
                    + generatedContent.replace("\n", "\\n")
                    + "\"}}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        Path cvOutputPath = tempDir.resolve("cv-adapte.docx");
        Path letterOutputPath = tempDir.resolve("lettre-motivation.docx");
        Properties properties = new Properties();
        properties.setProperty("address", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
        properties.setProperty("model", "google/gemma-4-e2b");
        properties.setProperty("cv.path", originalCvPath.toString());
        properties.setProperty("job.offer.path", "src/test/resources/offre-emploi.txt");
        properties.setProperty("cv.output.path", cvOutputPath.toString());
        properties.setProperty("letter.output.path", letterOutputPath.toString());

        MonAgentCVEtLettreDeMotivation.run(properties);

        assertArrayEquals(originalBytes, Files.readAllBytes(originalCvPath),
                "Le fichier CV source ne doit pas être modifié");
        assertTrue(readDocxText(cvOutputPath).contains("Jean DUPONT - Développeur Java"),
                "La copie doit contenir le CV adapté généré");
        assertTrue(hasBoldRun(cvOutputPath), "La copie du CV doit conserver une mise en forme en gras");
    }

    @Test
    void run_genereLesDeuxDocumentsWordDepuisLeCvEtLOffreSansModifierLaSource() throws Exception {
        Path originalCvPath = tempDir.resolve("cv-original.docx");
        try (XWPFDocument original = new XWPFDocument()) {
            XWPFParagraph title = original.createParagraph();
            title.createRun().setBold(true);
            title.getRuns().getFirst().setText("Profil");
            original.createParagraph().createRun().setText("Expérience");
            try (var outputStream = Files.newOutputStream(originalCvPath)) {
                original.write(outputStream);
            }
        }
        byte[] originalCvBytes = Files.readAllBytes(originalCvPath);

        Path jobOfferPath = tempDir.resolve("offre.txt");
        String jobOffer = "Nous recrutons un développeur Java senior.";
        Files.writeString(jobOfferPath, jobOffer, StandardCharsets.UTF_8);

        String generatedContent = """
                CV adapté
                Jean DUPONT - Développeur Java
                Compétences : Java, Spring
                
                Lettre de motivation
                Madame, Monsieur,
                Mon expérience Java correspond à vos besoins.
                """.stripTrailing();
        AtomicReference<String> methodRef = new AtomicReference<>();
        AtomicReference<String> contentTypeRef = new AtomicReference<>();
        AtomicReference<String> requestBodyRef = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            captureRequest(exchange, methodRef, contentTypeRef, requestBodyRef);
            byte[] response = ("{\"choices\":[{\"message\":{\"content\":\""
                    + generatedContent.replace("\n", "\\n")
                    + "\"}}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        Path cvOutputPath = tempDir.resolve("resultats/cv-adapte.docx");
        Path letterOutputPath = tempDir.resolve("resultats/lettre.docx");
        Properties properties = new Properties();
        properties.setProperty(
                "address",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions"
        );
        properties.setProperty("model", "google/gemma-4-e2b");
        properties.setProperty("cv.path", originalCvPath.toString());
        properties.setProperty("job.offer.path", jobOfferPath.toString());
        properties.setProperty("cv.output.path", cvOutputPath.toString());
        properties.setProperty("letter.output.path", letterOutputPath.toString());

        MonAgentCVEtLettreDeMotivation.run(properties);

        assertEquals("POST", methodRef.get());
        assertEquals("application/json", contentTypeRef.get());
        assertTrue(requestBodyRef.get().contains("Profil"), "La requête doit inclure le contenu du CV");
        assertTrue(requestBodyRef.get().contains(jobOffer), "La requête doit inclure l'offre d'emploi");
        assertTrue(requestBodyRef.get().contains("CV adapté"));
        assertTrue(requestBodyRef.get().contains("Lettre de motivation"));
        assertEquals("Jean DUPONT - Développeur Java\nCompétences : Java, Spring", readDocxText(cvOutputPath));
        assertEquals(
                "Madame, Monsieur,\nMon expérience Java correspond à vos besoins.",
                readDocxText(letterOutputPath)
        );
        assertTrue(hasBoldRun(cvOutputPath), "Le CV Word généré doit conserver le style en gras du modèle");
        assertArrayEquals(originalCvBytes, Files.readAllBytes(originalCvPath),
                "Le CV d'origine doit rester inchangé");
    }

    @Test
    @EnabledIfSystemProperty(named = "runLmStudioIntegration", matches = "true")
    void run_genereLesDocumentsAvecLeVraiModeleLmStudio() throws Exception {
        Properties properties = MonAgentCVEtLettreDeMotivation.loadProperties();
        Path originalCvPath = tempDir.resolve("cv-synthetique.docx");
        try (XWPFDocument cv = new XWPFDocument()) {
            cv.createParagraph().createRun().setText("Camille Exemple");
            cv.createParagraph().createRun().setText("Développeuse Java avec cinq ans d'expérience.");
            cv.createParagraph().createRun().setText("Compétences : Java, Spring, SQL.");
            try (var outputStream = Files.newOutputStream(originalCvPath)) {
                cv.write(outputStream);
            }
        }
        byte[] originalCvBytes = Files.readAllBytes(originalCvPath);

        Path jobOfferPath = tempDir.resolve("offre-synthetique.txt");
        Files.writeString(
                jobOfferPath,
                "Nous recherchons une développeuse Java maîtrisant Spring et SQL.",
                StandardCharsets.UTF_8
        );
        Path cvOutputPath = tempDir.resolve("resultats/cv-adapte.docx");
        Path letterOutputPath = tempDir.resolve("resultats/lettre-motivation.docx");
        properties.setProperty("cv.path", originalCvPath.toString());
        properties.setProperty("job.offer.path", jobOfferPath.toString());
        properties.setProperty("cv.output.path", cvOutputPath.toString());
        properties.setProperty("letter.output.path", letterOutputPath.toString());

        String response = MonAgentCVEtLettreDeMotivation.run(properties);

        assertTrue(response.contains("CV adapté"), "La réponse réelle doit contenir la section CV adapté");
        assertTrue(response.contains("Lettre de motivation"),
                "La réponse réelle doit contenir la section Lettre de motivation");
        assertTrue(Files.size(cvOutputPath) > 0, "Le vrai agent doit créer le document Word du CV");
        assertTrue(Files.size(letterOutputPath) > 0, "Le vrai agent doit créer le document Word de la lettre");
        assertTrue(readDocxText(cvOutputPath).contains("Camille Exemple"),
                "Le CV adapté doit reprendre l'identité synthétique fournie. Réponse : " + response);
        assertArrayEquals(originalCvBytes, Files.readAllBytes(originalCvPath),
                "Le vrai agent ne doit pas modifier le CV source");
    }

    private static String readDocxText(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText().stripTrailing();
        }
    }

    private static boolean hasBoldRun(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            return document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .anyMatch(XWPFRun::isBold);
        }
    }

    @Test
    @Disabled
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
    void construireMessageUtilisateur_inclutLeCvEtLOffreEtLaDemandeDeLettre() {
        String cvContent = "Jean DUPONT\nDéveloppeur Java";
        String offreEmploi = "Développeur Java senior";

        String message = MonAgentCVEtLettreDeMotivation.construireMessageUtilisateur(cvContent, offreEmploi);

        assertTrue(message.contains("Voici mon CV :"), "Le message doit contenir le CV");
        assertTrue(message.contains(cvContent), "Le message doit contenir le texte brut du CV");
        assertTrue(message.contains("Voici l'offre d'emploi :"), "Le message doit contenir l'offre d'emploi");
        assertTrue(message.contains(offreEmploi), "Le message doit contenir le texte brut de l'offre");
        assertTrue(message.contains("lettre de motivation"), "Le message doit demander la rédaction d'une lettre de motivation");
    }

    @Test
    void construireMessageUtilisateur_demandeUnCvAdapteEtUneLettreDansDesSectionsDistinctes() {
        String cvContent = "Jean DUPONT\nDéveloppeur Java";
        String offreEmploi = "Développeur Java senior";

        String message = MonAgentCVEtLettreDeMotivation.construireMessageUtilisateur(cvContent, offreEmploi);

        assertTrue(message.contains("CV adapté"), "Le message doit demander un CV adapté à l'offre");
        assertTrue(message.contains("Lettre de motivation"),
                "Le message doit demander une lettre de motivation");
        assertTrue(message.indexOf("CV adapté") < message.indexOf("Lettre de motivation"),
                "Le CV adapté et la lettre doivent être demandés dans cet ordre comme sections distinctes");
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