# MatrixJoinLink - A bot that allows the creation of Join Links to Rooms in Matrix

This bot allows the creation of JoinLinks to rooms.
The technical details are described on the bottom of this README.

:warning: No liability of any kind is assumed. This project is in alpha. It is possible that all implemented mechanisms can change.
Nevertheless, I am of course interested in feedback. Feel free to use the matrix chat (see below).

## Setup

1. Get a matrix account for the bot (e.g., on your own homeserver or on `matrix.org`)
2. Prepare configuration:
    * Copy `config-sample.json` to `config.json`
    * Enter `baseUrl` to the matrix server and `username` / `password` for the bot user
    * Add yourself or your homeserver (e.g., `:matrix.org`) to the `users` (empty list == allow all)
3. Either run the bot via jar or run it via the provided docker.
    * If you run it locally, you can use the environment variable `CONFIG_PATH` to point at your `config.json` (defaults to `./config.json`)
    * If you run it in docker, you can use a command similar to
      this `docker run -itd -v $LOCAL_PATH_TO_CONFIG:/usr/src/bot/data/config.json:ro ghcr.io/dfuchss/matrixjoinlink`

## Usage

* A user (see user list) can invite the bot to a room.
* After the bot has joined use `!join help` to get an overview about the features of the bot (remember: the bot only respond to users in the user list)
* In order to create a Join Link simply type `!join link` and the bot will create a join link. Please make sure that the bot has the ability to invite users.

## Development

Join our discussion at our matrix channel [#matrixjoinlink:fuchss.org](https://matrix.to/#/#matrixjoinlink:fuchss.org)

* The basic functionality (commands) are located in [Main.kt](src/main/kotlin/org/fuchss/matrix/joinlink/Main.kt). There you can also find the main method of
  the program.

### How does the bot work

1. Let's assume you have a private room with id `!private:room.domain`
2. You invite the bot and enter `!join link`. This will create a new public room with a random id (not listed in the room directory). We call this
   room `!public123:room.domain`.
3. The bot saves two state events: First in `!private:room.domain` a state called `org.fuchss.matrix.joinlink` that contains a pointer to the public room.
   Second, a state called `org.fuchss.matrix.rooms_to_join` in the public room that contains pointers to private rooms to join.
4. If someone joins the public room, the bot reads the protected state from the public room and invites the user to all roomIds that are present in the state.
5. If you want to invalidate the join link, you can simply type `!join unlink`.
