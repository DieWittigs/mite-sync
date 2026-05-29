# mite-sync

Spring-Boot-App rund um die [mite](https://mite.de/) Zeiterfassung. Bietet zwei unabhängige
Workflows als REST-Endpunkte:

1. **`POST /sync-jobs`** — kopiert Time-Entries einer Source-Mite-Instanz in eine
   Target-Mite-Instanz (delete-then-recreate für einen Datumsbereich). Nützlich, um eine
   billing-relevante Mite in eine private Mirror-Mite zu spiegeln.
2. **`POST /daily-reports/{date}/preview`** und **`/book`** — erzeugt aus Google-Calendar-Events
   und Azure-DevOps-Work-Items einen täglichen Buchungsvorschlag und bucht ihn (nach manueller
   Review) in die Source-Mite.

## Stack

- Java 25, Spring Boot 3.5
- Mite-Client: [`io.seventytwo.oss:mite-java`](https://github.com/72services/mite-java)
- Google Calendar API (OAuth2)
- Azure DevOps REST API (PAT)

## Setup

1. **Konfiguration:** `.env.example` → `.env` kopieren und Werte eintragen (Mite-Hosts/API-Keys,
   Azure-DevOps-PAT, Google-Calendar-ID). `.env` ist gitignored.
2. **Google OAuth einrichten:** siehe [HELP.md](./HELP.md) für die einmalige Google-Cloud-
   Console-Konfiguration und das Ablegen der `client_secret.json`.
3. **Starten:**
   - Lokal: `./mvnw spring-boot:run`
   - Docker Compose: `docker compose up` (Port 8888 nur beim ersten OAuth-Login öffnen)

Beispiel-Requests in [`mite-sync.http`](./mite-sync.http) (IntelliJ HTTP-Client-Format).

## Build & Test

```sh
./mvnw verify              # Tests + JaCoCo-Coverage-Gate (≥80%)
./mvnw spring-boot:run     # Lokaler Start auf :8080
docker build -t mite-sync . # Container-Image
```

## License

[MIT](./LICENSE) — Thomas Wittig, 2026
