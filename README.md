TradePro - The Ultimate Trading Plugin for Your Server!
TradePro is a very customizable trading plugin, designed to give your players a seamless and secure way to exchange items, XP, and more! Whether you're managing an economy-driven server or an RPG adventure world, TradePro ensures fair and balanced trades with extensive configuration options.

Compatibility
- Spigot / Paper from 1.8 up to 26.x, one jar. Java: whatever your server version needs (26.x servers need Java 25).
- The plugin is Java 8 bytecode, compiled against the 1.17.1 API, so the old versions keep working.

Entering amounts for XP / money / points (extras.<name>.mode in config.yml)
- anvil: an anvil prompt opens where the player types the amount (default for new installs).
  If the anvil cannot be opened on a server version, the plugin logs a warning and uses the chat prompt instead.
- chat: the player types the amount in chat.
- anything else: click to add/remove the configured increment.
- Changing a mode needs a server restart (/tradepro reload does not re-read it).

Building from source
- JDK 21 (or newer) + Maven: mvn package  ->  target/TradePro-<version>.jar
- AnvilGUI is vendored in lib/ as a pinned build (see lib/net/wesjd/anvilgui/README.txt).
- Keep minimizeJar=false in pom.xml. Minimizing silently deletes AnvilGUI's per-version classes.

Features
- Fully Customizable – Modify every menu item, message, and setting to fit your server's needs.
- Trade Logging – All trades between players can be logged & saved into a logs folder.
- Simple Web interface – You can view & search the logs of specific players on the web panel. (Disabled by default)
- Cross-World Trading – Enable or restrict trading between worlds and block specific ones.
- WorldGuard Support – Block trades in designated WorldGuard regions.
- Flexible Trade Distance – Set a maximum trade range to control player trading interactions.
- Trade Spectating – Admins can observe ongoing trades to prevent scams and disputes.
- Smooth & Simple Commands – Intuitive commands for both players and administrators.

Trade Anything – Players can exchange:
Items & XP
PlayerPoints
GriefPrevention blocks
TokenManager tokens

Item Blacklisting – Prevent certain items from being traded, including:
Specific materials
Items with particular lore text
Regex-based item restrictions


Commands:
/trade <player> – Send a trade request.
/tradepro reload – Reload configuration files.
/tradepro force <player1> <player2> – Force two players into a trade.
/tradepro spectate <player1> <player2> – Spectate an active trade.

Permissions:
️ tradepro.admin – Grants access to admin commands and trade notifications.
️ tradepro.trade – Allows players to send trade requests (enabled by default).

A huge thank you to Trophonix for open-sourcing the original project, making this newer version possible!
