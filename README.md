# MonAgentCVEtLettreDeMotivation

Application Java qui adapte un CV à une offre d'emploi et génère une lettre de motivation avec un modèle local via LM Studio.

## Lancer l'application

1. Installer LM Studio, télécharger le modèle configuré (par défaut `google/gemma-4-e2b`), le charger, puis démarrer le serveur local compatible OpenAI. La configuration par défaut utilise `http://127.0.0.1:1234/v1/chat/completions`.
2. Modifier `src/main/resources/application.properties` pour indiquer les chemins de vos fichiers d'entrée et de sortie, par exemple :

   ```properties
   address=http://127.0.0.1:1234/v1/chat/completions
   model=google/gemma-4-e2b
   cv.path=C:/Documents/MonCV.docx
   job.offer.path=C:/Documents/offre.txt
   company.website.url=https://www.entreprise.fr
   candidate.motivation=Je souhaite contribuer à des produits utiles aux patients.
   candidate.highlights=J'ai réduit de 30 % les incidents en production.
   cv.output.path=C:/Documents/resultats/CV-adapte.docx
   letter.output.path=C:/Documents/resultats/Lettre-de-motivation.docx
   analysis.output.path=C:/Documents/resultats/Analyse-candidature.txt
   ```

3. Ouvrir le projet avec un JDK 26 et lancer la classe `life.light.MonAgentCVEtLettreDeMotivation` depuis l'IDE.

Le CV et l'offre peuvent être stockés où vous le souhaitez ; leurs emplacements sont définis par `cv.path` et `job.offer.path`. Le CV source reste inchangé. Les dossiers de sortie sont créés automatiquement. Les deux propriétés de sortie doivent être définies ensemble. Une extension `.docx` produit un document Word ; les autres extensions produisent du texte. Le CV Word de sortie utilise le document `cv.path` comme modèle pour en conserver les styles et la structure.

`company.website.url` est facultative. Si elle est définie, l'application lit uniquement la page indiquée, retire les scripts et éléments de navigation, et envoie au modèle au plus 5 000 caractères de texte avec le CV et l'offre. L'accès expire après 5 secondes ; une URL invalide, une redirection ou une erreur HTTP arrête l'exécution avec une erreur explicite. Le modèle reçoit pour consigne de traiter le texte du site comme du contexte factuel, pas comme des instructions.

`candidate.motivation` et `candidate.highlights` sont facultatives. Elles permettent de préciser pourquoi le poste intéresse le candidat et quelles réalisations ou quels points forts, il souhaite mettre en avant. Utilisez des exemples concrets et exacts ; l'application demande au modèle de ne pas inventer de motivation ni d'ajouter des faits absents des informations fournies.

`analysis.output.path` est facultative. Quand elle est configurée, l'application écrit une analyse de correspondance qui liste les exigences importantes de l'offre, les preuves repérées dans le CV et les éléments à clarifier. Cette analyse est générée par le modèle : vérifiez les citations et les correspondances avant d'envoyer votre candidature.

Pour n'utiliser que le CV, omettre `job.offer.path`. Sans les deux chemins de sortie, la réponse est affichée par l'application, mais aucun document n'est écrit.

## Test d'intégration avec LM Studio

Le test d'intégration avec le vrai modèle est désactivé par défaut. Démarrer LM Studio avec le modèle configuré, puis lancer :

```powershell
mvn test "-Dtest=MonAgentCVEtLettreDeMotivationTest#run_genereLesDocumentsAvecLeVraiModeleLmStudio" -DrunLmStudioIntegration=true
```

Ce test utilise un CV et une offre synthétique temporaire.