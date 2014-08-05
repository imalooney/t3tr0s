<a href="http://t3tr0s.com">
<img src="public/img/logo.png">

<img src="http://i.imgur.com/Y3cvDx2.gif">
</a>

[Click to Play!](http://t3tr0s.com)

We are re-creating Tetris™ in
[ClojureScript](https://github.com/shaunlebron/ClojureScript-Syntax-in-15-minutes).
We are mainly doing this to
[produce the pleasure](http://youtu.be/nTDRY8aPy7c?t=3m14s)
and to celebrate the 30th anniversary of its original release in 1984.  Our
remake will enable us to host a small, local tournament and to share a montage
of the game's history, with each level resembling a different version from its
past.  (We are working on the game at least once a week):

- [DevBlog 1](devblog/day01.md) - data, collision, rotation, drawing
- [DevBlog 2](devblog/day02.md) - basic piece control
- [DevBlog 3](devblog/day03.md) - gravity, stack, collapse, hard-drop
- [DevBlog 4](devblog/day04.md) - ghost piece, flash before collapse
- [DevBlog 5](devblog/day05.md) - game over animation, score
- [DevBlog 6](devblog/day06.md) - level speeds, fluid drop, improve collapse animation, etc.
- [DevBlog 7](devblog/day07.md) - draw next piece, tilemap for themes
- [DevBlog 8](devblog/day08.md) - allow connected tiles for richer graphics
- DevBlog 9 - live board broadcasting
- DevBlog 10 - chat room, more tilemaps, page layouts
- DevBlog 11 - page routing, username

## Setup

1. Install [Leiningen] and [Node.js].
1. Run the following in the project directory

    ```sh
    # install node.js dependencies
    npm install

    # compile LESS file
    grunt less

    # compile ClojureScript files (this may take a minute)
    lein cljsbuild once

    # copy the example config file (edit as needed)
    cp example.config.json config.json

    # start the node.js server
    node server.js
    ```

1. Open <http://localhost:1984> in your browser.

### Browser REPL

If you want a REPL connected to the browser for interactive testing:

```
lein repl
> (brepl)
```

### Recording GIFs

If you want to record a GIF of gameplay:

1. Create empty "gif" directory at the project root.
1. Play the game in the browser (with the developer console open).
1. Press "shift" to start/stop recording. (watch the console)
1. To write the recording to a file, type this in the browser REPL:

    ```clj
    (ns client.vcr)
    (publish-record!)
    ```

1. In the "gif" folder, you should see "anim.gif" and intermediate PNG frames.

## References

- <http://www.tetrisfriends.com/help/tips_appendix.php>
- <http://tetris.wikia.com/wiki/Tetris_Guideline>

## License

All code licensed under the terms of the [MIT License](https://github.com/imalooney/t3tr0s/blob/master/LICENSE).

The image files of past versions of the games (ie: the tilemap files) belong to their respective copyright holders.

Tetris is a registered trademark of The Tetris Company.

[Node.js]:http://nodejs.org
[Leiningen]:http://leiningen.org