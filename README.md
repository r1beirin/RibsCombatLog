# RibsCombatLog
RibsCombatLog is a Hytale plugin designed to prevent players from avoiding PvP consequences by disconnecting during combat. When a player leaves the server voluntarily while tagged, they are automatically killed and their items are dropped at their location.

## Config
When the plugin starts, it will create the following directory and file: `<dataDirectory>/RibsCombatLog/config.json`. The default value for the duration is 15 seconds.

### Default config.json
```json
{
  "combatTagDurationSeconds": 15
}
```

## Instruction
You can either download the pre-compiled .jar file directly, or modify the source code to suit your needs and compile it yourself.
### Compilation
To compile the plugin, use the following command:
```bash
mvn clean package -DskipTests
```

### Instalation
1. Locate the generated .jar file in the `target/` directory.
2. Copy the .jar file and paste it into the `mods/` folder of your Hytale server.
3. Restart the server to load the plugin.