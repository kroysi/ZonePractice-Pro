# ZonePractice

ZonePractice is a modular Minecraft PvP/practice plugin written in Java and built with Maven. It's organized as multiple
modules (core, platform-specific builds, and distribution) and supports several server forks and versions used by PvP
servers.

Features

- Ladder, Arena, FFA and Event managers
- Profile, Leaderboard (holograms), Sidebar and Tablist management
- Match handling, Kit management and inventory utilities
- Soft-dependencies: PlaceholderAPI (PAPI) integration when available
- Supports forks such as Paper/Spigot, FoxSpigot and Carbon where applicable

Supported versions

- Primary supported versions: 1.8.8, 1.8.9 (legacy) and modern targets (1.20.6, 1.21.4) as indicated by runtime checks
  from the codebase. See `VersionChecker` in `core` for exact supported values.

Dependencies

- PlaceholderAPI (optional): adds extra placeholder support when present. Install PlaceholderAPI by dropping its jar
  into the server's `plugins/` folder if you want the extra placeholders to be available.
- PacketEvents (runtime dependency, required for packet-level features): https://github.com/retrooper/packetevents
    - ZonePractice integrates with PacketEvents for low-level packet handling and certain features that rely on packet
      interception.
    - PacketEvents must be installed on the server as a separate plugin (i.e., place the PacketEvents jar in the
      server's `plugins/` directory alongside the ZonePractice jar). Do not attempt to only add PacketEvents as a
      build-time dependency; it must be present at runtime for those features to work.
    - Download PacketEvents from its GitHub Releases or build it from source. Use a PacketEvents build compatible with
      your server software (Paper/Spigot/Fork) and Minecraft version.
    - Installation steps (example):
        1. Download the PacketEvents jar from https://github.com/retrooper/packetevents/releases
        2. Stop your server.
        3. Copy both `PacketEvents-<version>.jar` and your chosen `ZonePractice` jar into the server `plugins/` folder.
        4. Start the server and check the console; PacketEvents should report that it enabled successfully before
           ZonePractice initializes.
    - Notes: Loading order can matter — if you see missing provider errors on startup, ensure PacketEvents is present
      and enabled, then restart the server. Do not shade or bundle PacketEvents into ZonePractice; keep it external to
      avoid compatibility and loading-order issues.

Repository layout

- `core/` — main plugin code and most logic; produces `practice-core-*.jar` under `core/target/`
- `spigot_1_8_8/` — legacy Spigot 1.8.8 build module
- `spigot_modern/` — modern platform builds (1.20.x / 1.21.x targets)
- `distribution/` — builds the runnable distribution JARs (`ZonePractice Pro v*.jar`) that bundle modules for release
- `libs/` — helper jars and server forks used during development

Getting started (build)

1. Make sure you have a JDK (Java 17+ recommended for modern targets) and Maven installed.
2. From the repository root, build with Maven:

   mvn clean package

   Optional parallel build (useful on multi-core machines):

   mvn -T 1C clean package

3. Built artifacts appear in each module's `target/` directory. Examples:
    - `core/target/practice-core-<version>.jar`
    - `spigot_1_8_8/target/practice-spigot_1_8_8-<version>.jar`
    - `distribution/target/ZonePractice Pro v<version>.jar`

Install (server)

1. Copy the appropriate JAR for your server to the server's `plugins/` directory.
    - For a single-server install you usually only need the distribution jar (if provided) or the `spigot_modern` /
      `spigot_1_8_8` module jar that matches your server.
2. Start the server and watch the console for plugin initialization messages (or check `logs/latest.log` inside the
   server directory).
3. On first run the plugin will create default configuration files; consult `core/src/main/resources` for default
   templates.

Note: the plugin's `name` in `plugin.yml` is `ZonePracticePro` — default data/config folder will be
`plugins/ZonePracticePro` (not `plugins/ZonePractice`).

Configuration & first run

- Default configuration files are created by the plugin on first run. Look for a `config.yml` or similarly named files
  in the server `plugins/ZonePracticePro` (or plugin-named) folder. Example template files live under
  `core/src/main/resources/1.8.8/` and `core/src/main/resources/modern/` in the repo (e.g. `config.yml`,
  `divisions.yml`, `guis.yml`, `inventories.yml`).
- `config.yml` contains a `VERSION` field (currently 13 in the 1.8.8 template). When upgrading across major plugin
  versions, review the template in the repo and migrate custom changes as necessary.
- The plugin contains optional MySQL support (see `core/src/main/resources/1.8.8/config.yml`:
  `MYSQL-DATABASE.ENABLED: false`). Enable and configure that if you want remote storage; backup configs before
  enabling.
- Check `logs/latest.log` or the running server console for messages such as Unsupported version warnings. If the server
  version is unsupported, the plugin will disable itself.
- If you use PlaceholderAPI, ensure it is installed to enable additional placeholders.

Commands & Permissions (summary)

- The plugin registers many commands (see `core/src/main/resources/plugin.yml`). Common commands include `/practice` (
  aliases: `/prac`, `/zonepractice`, `/zoneprac`, `/zonep`), `/arena`, `/ladder`, `/duel`, `/party`, `/spectate` and
  more.
- Permissions are under the `zpp.*` namespace (see `plugin.yml`); for example `zpp.admin` (default: op),
  `zpp.practice.*`, `zpp.staffmode`, and many granular permissions.

Soft-dependencies and load order

- `plugin.yml` declares `softdepend: [ PlaceholderAPI, Multiverse-Core, FastAsyncWorldEdit, LiteBans ]` and
  `loadbefore: [ CMI, CMILib ]`.
- These plugins are optional integrations that provide extra features or compatibility; the plugin functions without
  them but will enable additional functionality when present.

Troubleshooting

- PacketEvents not found / missing provider errors:
    - Ensure the PacketEvents jar is present in `plugins/` and enabled before ZonePractice.
    - If you still see errors, restart the server after placing PacketEvents in `plugins/` instead of relying on
      hot-loading.
- MySQL connection failures:
    - Verify `core/src/main/resources/1.8.8/config.yml` (or `modern/config.yml`) has the correct `MYSQL-DATABASE`
      settings and that your database accepts connections from the server host.
    - The plugin uses standard JDBC (`DriverManager`); if your server JRE/JDK lacks a suitable driver for your DB,
      ensure a compatible JDBC driver is available on the server classpath (for most MySQL setups the bundled connector
      works via DriverManager). Check the server console for detailed SQL exceptions.

plugin.yml and api-version

- The main `plugin.yml` (contains `name: ZonePracticePro`, `api-version: 1.13`, commands and permissions) is under
  `core/src/main/resources/plugin.yml`. Use it as the authoritative source for commands, permissions and
  soft-dependencies.

Contributing

- Contributions are welcome via pull requests. Keep changes focused and include tests where feasible.
- Follow the existing code style and naming conventions found in the `core` module.
- Open an issue if you find a bug or want to propose a feature.

License
This project is licensed under the MIT License — see the `LICENSE` file for details.

Maintainers / Contact

- For public issues, open an issue on the repository.
- Maintainership is credited to "ZonePractice contributors" in the LICENSE.

Assumptions

- I added the MIT license (year 2025) and used the generic owner string "ZonePractice contributors" as requested.
