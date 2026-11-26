# Musica 🎵

Lettore musicale personale per Android, stile YT Music: grafica nera e minimale, brani presi dalla memoria del telefono.

## Cosa fa
- Elenca tutta la musica del telefono (MP3, FLAC, M4A…) con le copertine, più una ricerca per titolo, artista o album
- Tasto **Casuale** per ascoltare tutto in ordine sparso
- Mini-player in basso; toccalo per aprire il player a schermo intero (copertina grande, barra per scorrere il brano, shuffle, repeat)
- **La musica continua a schermo spento** (servizio in primo piano Media3)
- **Notifica e schermata di blocco** con titolo, artista, copertina e i tasti ⏮ ⏯ ⏭ ⏹
- Si mette in pausa da sola se arriva una chiamata o se stacchi le cuffie

## Come ottenere l'APK

### Opzione A: GitHub, senza installare niente
1. Crea un repository su GitHub (anche privato) e carica dentro il contenuto di questa cartella.
2. Vai su **Actions**: la build parte da sola (ci mette circa 5 minuti).
3. Apri la build finita e scarica l'artifact **Musica-apk**: è uno zip con dentro l'APK.
4. Copia l'APK sul telefono, aprilo e consenti l'installazione da "origini sconosciute".

### Opzione B: Android Studio
1. Apri la cartella con Android Studio e aspetta il Gradle sync.
2. Collega il telefono con il debug USB attivo e premi ▶ Run.

## Se in standby la musica si ferma
Alcuni produttori (Xiaomi, Samsung, Huawei, Oppo…) chiudono le app in background in modo aggressivo. In quel caso vai in **Impostazioni → App → Musica → Batteria** e scegli **Senza restrizioni**.
