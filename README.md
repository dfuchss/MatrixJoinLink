# MatrixJoinLink - A bot that allows the creation of Join Links to non-public Rooms in Matrix

This bot allows the creation of join links to non-public rooms in matrix. It uses the [Trixnity](https://trixnity.gitlab.io/trixnity/) framework.

## Reason for this Bot

I always struggled with the problem that I have private rooms, I want to share with a group of friends. Before the bot, I had to invite all the people. Now I
can invite _JoinLink_ and create an invite link. This link can be shared to my friends who want to join my room (including spaces).

## Setup

1. Get a matrix account for the bot (e.g., on your own homeserver or on `matrix.org`)
2. Prepare configuration:
    * Copy `config-sample.json` to `config.json`
    * Enter `baseUrl` to the matrix server and `username` / `password` for the bot user
    * Set an encryption key. The bot will use this string as key to encrypt the state events.
    * Add yourself (e.g., `@user:matrix.org`) or your homeserver (e.g., `:matrix.org`) to the `users` (empty == allow all). Users can interact with the bot.
    * Add yourself to the `admins` (can't be empty)
3. Either run the bot via jar or run it via the provided docker.
    * If you run it locally, you can use the environment variable `CONFIG_PATH` to point at your `config.json` (defaults to `./config.json`)
    * If you run it in docker, you can use a command similar to
      this `docker run -itd -v $LOCAL_PATH_TO_CONFIG:/usr/src/bot/data/config.json:ro ghcr.io/dfuchss/matrixjoinlink`
    * If you want to persist sessions, you should persist the data volume `-v $LOCAL_PATH_TO_DATA:/usr/src/bot/data`

## Usage

* A user (see user list in configuration file) can invite the bot to a room.
* After the bot has joined use `!join help` to get an overview about the features of the bot (remember: the bot only respond to users in the user list)
* In order to create a Join Link simply type `!join link SomeFancyNameForTheLink` and the bot will create a join link. Please make sure that the bot has the ability to invite users.

![Help](.docs/help.png)

### Creation of Join (Invite) Links
![Creation](.docs/creation.png)

### Entering a Join (Invite) Link Room
![Enter](.docs/joined.png)

### Unlinking a Join (Invite) Link
![Unlink](.docs/unlink.png)

## Development

I'm typically online in the [Trixnity channel](https://matrix.to/#/#trixnity:imbitbu.de). So feel free to tag me there if you have any questions.

* The basic functionality is located in [Main.kt](src/main/kotlin/org/fuchss/matrix/joinlink/Main.kt). There you can also find the main method of the bot.

### How does the bot work

1. Let's assume that you want to share the private room `!private:room.domain`
2. After you've invited the bot, you can enter `!join link IShareLinksWithYou`
3. The bot creates a new public room that contains "IShareLinksWithYou" in its name. This room will not be listed in the room directory; for this example its ID
   is `!public:room.domain`
4. If somebody joins the public room, the bot verifies based on two encrypted state events in `!private:room.domain` and `!public:room.domain` whether the rooms
   belong to each other. If so, the bot simply invites the user to the private room.
5. If you want to disable the share simply type `!join unlink` in the private room. This will invalidate the join link.
