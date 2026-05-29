# Daily-Reports Setup

Die App stellt zwei Workflows bereit:

1. **Klassischer Sync-Job** (`POST /sync-jobs`): kopiert Time-Entries von Source → Target Mite.
2. **Daily-Reports** (`POST /daily-reports/{date}/preview` und `/book`): liest Google Calendar +
   Azure DevOps und erzeugt tägliche Buchungsvorschläge für die Source-Mite.

Für Daily-Reports muss einmalig OAuth2 für Google Calendar eingerichtet werden.

## Google Cloud OAuth2 Setup

1. **Google Cloud Project anlegen** (oder vorhandenes nutzen):
   <https://console.cloud.google.com/projectcreate>

2. **Calendar API aktivieren:**
   <https://console.cloud.google.com/apis/library/calendar-json.googleapis.com>
   → Auf „Activate" klicken.

3. **OAuth Consent Screen** konfigurieren:
   <https://console.cloud.google.com/apis/credentials/consent>
   - User Type: **External**
   - App name: `mite-sync` (egal)
   - User support email: deine Adresse
   - Scopes: `.../auth/calendar.readonly` hinzufügen
   - Test users: deine eigene Google-Adresse hinzufügen (sonst greift die App nicht auf den Account zu)

4. **OAuth-Client erzeugen:**
   <https://console.cloud.google.com/apis/credentials>
   - „Create Credentials" → „OAuth client ID"
   - Application type: **Desktop app**
   - Name: `mite-sync-desktop`
   - „Create" → JSON herunterladen

5. **JSON ablegen:**
   ```sh
   mkdir -p ~/.mite-sync
   mv ~/Downloads/client_secret_*.json ~/.mite-sync/google-client-secret.json
   ```

6. **App das erste Mal starten** (`mvn spring-boot:run`).
   Beim ersten Aufruf von `/daily-reports/{date}/preview` öffnet sich automatisch ein
   Browser-Fenster zur Authorization. Mit dem konfigurierten Google-Account anmelden, Zustimmung
   geben — Tokens werden in `~/.mite-sync/google-tokens/` persistiert. Folgeaufrufe brauchen keine
   Interaktion.

## Azure DevOps PAT

Der PAT wird via Umgebungsvariable `DAILY_REPORTS_AZURE_DEVOPS_PAT` injiziert (siehe
`.env.example`). Er braucht nur den Scope **Work Items: Read**.

## Beispiel-Aufrufe

Siehe [`mite-sync.http`](./mite-sync.http) — IntelliJ HTTP-Client unterstützt das direkt.

## Architektur-Übersicht

```
                     ┌──────────────────────┐
                     │   /sync-jobs         │  (existing)
                     │   Source → Target    │
                     └──────────────────────┘
                                ▲
       ┌─────────────────┐      │      ┌─────────────────────┐
       │ Google Calendar │──┐   │      │  /daily-reports     │
       │   (OAuth2)      │  │   │      │     /preview        │
       └─────────────────┘  │   │      │     /book           │
                            ▼   │      └─────────────────────┘
       ┌─────────────────┐ Build      ┌─────────────────────┐
       │ Azure DevOps    │─Proposal──→│  Source Mite        │
       │   (PAT)         │            │                     │
       └─────────────────┘            └─────────────────────┘
```

---

# Getting Started

### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.3.5/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.3.5/maven-plugin/build-image.html)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/3.3.5/reference/using/devtools.html)
* [Spring Web](https://docs.spring.io/spring-boot/3.3.5/reference/web/servlet.html)

### Guides

The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the
parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

