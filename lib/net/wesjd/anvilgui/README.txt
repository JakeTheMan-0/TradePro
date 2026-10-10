AnvilGUI (https://github.com/WesJD/AnvilGUI), MIT licensed, (c) WesJD and contributors.

This directory is a vendored copy of the net.wesjd:anvilgui 1.10.13-SNAPSHOT build published to
https://mvn.wesjd.net/ on 2026-07-14 (timestamp 20260714.043149-1, vendored as version 1.10.13-vendored-20260714; sha1 of the jar is in the .jar.sha1 file).
It is the first build whose Minecraft 26.x wrapper (Wrapper26_R1) is Java 21 bytecode, so it can be shaded and
loaded without needing Java 25.

TradePro resolves it through the "local-lib" file repository declared in pom.xml. To update it, deploy the new
build in the same layout and change the version in pom.xml.
