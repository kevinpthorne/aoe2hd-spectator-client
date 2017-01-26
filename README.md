[![Logo](https://github.com/kevinpthorne/aoe2hd-spectator-client/raw/master/src/main/resources/icons/ready.png)]() 
Age of Empires 2 HD - Spectator Client
======

This is an *unofficial* companion app that streams AoE2 HD games live, with the help of the [Relay Server](https://github.com/kevinpthorne/aoe2hd-spectator-server)

Forenote, currently broken, please don't try to run yet!

## Overview

This server is broken into 2 parts: **Websocket** server backend that manages the actually streaming of recordings and **Laravel** for a web frontend to display available live games. Both parts are written on PHP 7.

## How it works

![Overview](https://github.com/kevinpthorne/aoe2hd-spectator-server/blob/master/docs/graphics/Overview.png)

This is essentially what Voobly does under the hood, just not as integrated since HD doesn't expose controls like UserPatch does.

![UpStream](https://github.com/kevinpthorne/aoe2hd-spectator-server/blob/master/docs/graphics/Upstream.png)

Age of Empires 2 saves the recording files as the game progresses. (This is why recording games on older computers/hard drives lags the game). The Client (this repo) take the recording file, as it's coming, and upload them to a [Relay Server](https://github.com/kevinpthorne/aoe2hd-spectator-server). Clients can then, after accessing the Server's webpage, can then download and watch the game.

![DownStream](https://github.com/kevinpthorne/aoe2hd-spectator-server/blob/master/docs/graphics/Downstream.png)

## Planned

- [x] Streaming, *coined "Upstreaming"*
- [ ] Spectating, *coined "Downstreaming"*
  - [x] Core function - actually streaming the file
  - [ ] Web front - where people can find games to spectate
  - [ ] In-game spectating status, using a [Direct3D Overlay](https://github.com/kevinpthorne/Java-DX9-Overlay-API)
- [ ] Live (sort of) stats
  - [ ] Utilizing [recanalyst](https://github.com/goto-bus-stop/recanalyst) to report *some* game stats, like the Voobly spectator dashboard. This is limited, however, [due to the nature of recordings.](https://github.com/goto-bus-stop/recanalyst/issues/1)
- [ ] Spectator Chat

## Requirements
 
At the moment, you'll need a fast hard drive to minimize load times. Other requirements:

- Java 8

## How to run & Settings

Once this is stable, native executables/installations will be available. For now, run the **JAR** as *Administrator*

The first time it runs, it won't know where your Age2HD savegame directory is. Right-click the icon, click Settings, and then close the App. You should have a ```config.txt``` open. Change the ```save_game_directory``` and the ```relay_server``` to what you'd like. Restart app.

## DISCLAIMER

This is not only served AS-IS, but Microsoft, Ensemble Studios, Skybox, and whoever else you want to associate Age of Empires 2 with has nothing to do with these projects. Purely started by me, Kevin "echospot" Thorne.

## License & Contributing

NOTICE.md includes all licenses, mostly for libraries included (such as [PHP-Websockets](https://github.com/ghedipunk/PHP-Websockets), Java Websocket Libraries, etc). LICENSE.md licenses this under the Apache License 2.0. [This means you can contribute freely! Just tell everyone what you did and include previous licenses!](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))
