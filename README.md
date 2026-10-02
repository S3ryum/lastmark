# LastMark

LastMark remembers each player's most recent death location and helps them find their way back. Death records are stored locally by the server and survive restarts.

## Requirements

- Paper 26.2
- Java 25

## Build

Clone this repository and build the plugin:

```bash
git clone https://github.com/S3ryum/lastmark.git
cd lastmark
./gradlew build
```

On Windows, run `gradlew.bat build` instead. The plugin JAR is written to `build/libs/`.

Copy the JAR into your Paper server's `plugins/` folder, then restart the server.

## Use

After a player dies, LastMark records that location automatically.

- `/lastmark` — show the saved world's name and coordinates
- `/lastmark distance` — show the straight-line distance in blocks when you are in the same world
- `/lastmark age` — show how long ago the saved death was recorded
- `/lastmark compass` — point a compass in either hand to the saved location; you must be in the same world
- `/lastmark clear` — remove your saved death location

The command also has the `/lastdeath` alias. Age is shown using the largest one or two time units, such as `2 hours 15 minutes ago`. No additional permissions or configuration are required.

Death records are kept in `plugins/LastMark/deaths.yml`. LastMark does not send player data to an external service.
