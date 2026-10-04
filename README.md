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

## Catalog discovery and release indexes

The Zalava catalog locates this source repository and `releases/index.yaml`.
Versions, artifacts, digests, source commits, permissions and compatibility belong
to this module's release index. Do not update the catalog for each release.

The release-index workflow verifies every indexed artifact against its anonymous
GitHub Release download and immutable source tag. On a newly published release it
generates a reviewed index-update PR; merge that PR to expose the release through
catalog discovery. Enable Actions to create pull requests in repository settings.
The proposing workflow verifies the exact generated index. Maintainers must run
the normal module gate before approving it. The workflow never merges.

For manual generation, install `scripts/requirements.txt`, then run
`python scripts/release_index.py --artifact <published-jar> --tag <immutable-tag>
--revision <full-tag-commit> --repository https://github.com/Zalava/<repository>`.
Existing version entries cannot be rewritten. JSON syntax in `index.yaml` is valid
YAML and is accepted by the host's strict release-index loader.

The index workflow is called directly by the release workflow after publishing,
so GitHub token event suppression does not prevent index maintenance. Every
indexed source tag must belong to public `main`. Index updates remain reviewed
PRs; repository Actions permissions must allow their creation, and automation
never merges them. GitHub may require approval before running bot-created PR CI.
