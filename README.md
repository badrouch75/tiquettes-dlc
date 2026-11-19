# Étiquettes DLC

Application Android pour terminal Sunmi (imprimante intégrée 58 mm) : on touche un produit,
l'étiquette sort avec la date de fabrication ou d'ouverture, la date limite, la température
de conservation et le nom de la personne.

## Installer sur la tablette

Ouvrir dans Chrome sur la Sunmi :
`https://github.com/badrouch75/etiquettes-dlc/releases/latest/download/etiquettes-dlc.apk`

## Modifier

- Interface et liste de produits : `app/src/main/assets/index.html`
- Lien avec l'imprimante : `app/src/main/java/app/etiquettes/dlc/MainActivity.java`

Chaque modification poussée sur `main` recompile l'application automatiquement (onglet Actions)
et publie une nouvelle version (onglet Releases).

Les durées de conservation fournies sont des valeurs de départ, à valider avec le plan de
maîtrise sanitaire de l'établissement.
