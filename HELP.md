# Daily-Reports setup

The app exposes two workflows:

1. **Classic sync job** (`POST /sync-jobs`): copies time entries from source to target Mite.
2. **Daily-Reports** (`POST /daily-reports/{project}/{date}/preview` and `/book`): builds daily
   booking proposals and books them into the Mite instance configured for the project.

Daily-Reports needs a one-time Google Calendar OAuth2 setup.

## Project profiles

The `{project}` path segment selects a profile from `daily-reports.profiles.*` in
`application.yml`. A profile defines:

- the **workflow type**: `calendar-devops` (Google Calendar + Azure DevOps + fill-up onto a main
  PBI) or `git-activity` (proposal derived from local git history — not implemented yet)
- the **Mite instance** (`source` or `target`) that already-booked entries are read from and new
  entries are booked into, plus the Mite `project-id`/`service-id`
- the **rules**: daily-event summary and fixed minutes, rounding step, daily target minutes

The legacy routes without a project segment (`/daily-reports/{date}/preview`) use the profile
named by `daily-reports.default-profile`. Unknown project keys return 404; profiles whose
workflow type is not implemented yet return 501.

## Google Cloud OAuth2 setup

1. **Create a Google Cloud project** (or reuse an existing one):
   <https://console.cloud.google.com/projectcreate>

2. **Enable the Calendar API:**
   <https://console.cloud.google.com/apis/library/calendar-json.googleapis.com>
   → click "Activate".

3. **Configure the OAuth consent screen:**
   <https://console.cloud.google.com/apis/credentials/consent>
   - User type: **External**
   - App name: `mite-sync` (any name)
   - User support email: your address
   - Scopes: add `.../auth/calendar.readonly`
   - Test users: add your own Google address (otherwise the app cannot access the account)

4. **Create the OAuth client:**
   <https://console.cloud.google.com/apis/credentials>
   - "Create credentials" → "OAuth client ID"
   - Application type: **Desktop app**
   - Name: `mite-sync-desktop`
   - "Create" → download the JSON

5. **Place the JSON:**
   ```sh
   mkdir -p ~/.mite-sync
   mv ~/Downloads/client_secret_*.json ~/.mite-sync/google-client-secret.json
   ```

6. **Start the app for the first time** (`mvn spring-boot:run`).
   The first call to `/daily-reports/{date}/preview` opens a browser window for authorization.
   Sign in with the configured Google account and grant consent — tokens are persisted under
   `~/.mite-sync/google-tokens/`. Subsequent calls run without interaction.

## Azure DevOps PAT

The PAT is injected via the environment variable `DAILY_REPORTS_AZURE_DEVOPS_PAT` (see
`.env.example`). It only needs the **Work Items: Read** scope.

## Example requests

See [`mite-sync.http`](./mite-sync.http) — the IntelliJ HTTP client understands this format
directly.

## Architecture overview

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
