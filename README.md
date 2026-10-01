# Zalava Telegram channel module

An external Telegram adapter for the Zalava semantic channel SPI. It is built against the public,
released `org.zalava:module-api:0.1.0-alpha.3` artifact from
[`zalava-maven`](https://zalava.github.io/zalava-maven/).

The module maps private Telegram messages and inline callback buttons into semantic interactions.
It maps the stable numeric Telegram user ID as the external identity subject and uses the numeric
chat ID only as an opaque delivery handle. Telegram usernames are never identity authority.

Core remains responsible for linking, permissions, conversation state, approval validation,
deduplication and persistence. Group and supergroup messages are rejected by default until an
explicit group policy is added.

## Development

Run `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew test check`.

The contract-kit test loads the built JAR in an isolated classloader. Its local transport fixtures
prove mapping and rendering behavior only; they do not prove live Telegram acceptance. Live
long-polling is explicitly started by the host via `TelegramSdkTransport.start` with a secret
token, which must never be committed or logged.
