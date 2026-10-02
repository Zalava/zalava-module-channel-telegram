# CHANNEL-TELEGRAM-MOD-01 — Telegram external channel module

## Scope

Deliver a public Telegram adapter against released `module-api:0.1.0-alpha.3` from
`https://zalava.github.io/zalava-maven/`. The module maps only private Telegram updates into
semantic channel interactions and renders semantic Core events. It does not own account linking,
authorization, conversation state, approval validation, replay handling, or persistence.

## Security boundary

- The adapter maps the stable numeric Telegram sender ID to the external identity subject. Usernames
  are intentionally not mapped as identity authority.
- Telegram group/supergroup input is denied until a separate explicit policy is designed.
- Bot credentials are configuration/secrets only; no token is committed or logged.
- Callback data is a Core-issued opaque action ID. Core still rechecks actor, request, expiry and
  authorization before recording an approval decision.

## Acceptance and evidence

- The contract-kit test loads the built JAR in isolation, verifies the declared channel and ensures
  module API classes are not packaged.
- Local fixture tests cover private numeric identity mapping, group denial, malformed input rejection,
  text delivery and approval buttons. They are not proof of live Telegram delivery.
- A token-backed long-polling/live acceptance requires separately configured credentials and remains
  environment-gated.

The published adapter exposes `TelegramSdkTransport.start(token, channel)` for the host's approved
secret/configuration lifecycle. The current SPI has no channel configuration/start lifecycle injection,
so connecting that configured transport is a Core follow-up rather than silently reading an environment
variable in a module constructor.
