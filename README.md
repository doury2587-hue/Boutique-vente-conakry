# Boutique Conakry — vraie application Android

Application Android native en Java, sans bibliothèque externe obligatoire.

Fonctions :
- Tableau de bord
- Produits et stock
- Prix d'achat / vente
- Vente avec diminution automatique du stock
- Calcul automatique du bénéfice
- Dépenses
- Historique des ventes et dépenses
- Rapport partageable vers WhatsApp
- Données conservées localement sur le téléphone
- Fonctionne hors connexion

## Obtenir l'APK

Le dossier contient un workflow GitHub Actions :
`.github/workflows/build-apk.yml`

Après avoir placé le projet dans un dépôt GitHub :
1. Ouvrir l'onglet Actions.
2. Choisir « Construire l'APK Boutique Conakry ».
3. Cliquer sur « Run workflow ».
4. À la fin, télécharger l'artefact « Boutique-Conakry-APK ».
5. L'artefact contient `app-debug.apk`.

L'APK de debug est installable sur un téléphone Android pour tester l'application.

## Important

Le projet est fourni prêt à compiler. La compilation locale n'a pas été possible dans l'environnement actuel car le SDK Android et Gradle ne sont pas installés et l'environnement de compilation n'a pas d'accès réseau.
