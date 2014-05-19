# t3tr0s

<img src="http://i.imgur.com/XzonCuN.gif" align="right" style="132px">

We are re-creating Tetris™ in ClojureScript.  We are mainly doing this to
[produce the pleasure](http://youtu.be/nTDRY8aPy7c?t=3m14s) and to celebrate
the 30th anniversary of its original release in 1984.  Our remake will enable
us to host a small, local tournament and to share a montage of the game's
history, with each level resembling a different version from its past.  (We are
working on the game at least once a week):

- [DevBlog 1](devblog/day01.md)
- [DevBlog 2](devblog/day02.md)
- [DevBlog 3](devblog/day03.md)

## Setup

1. Install [Leiningen](http://leiningen.org/) and [NodeJS](http://nodejs.org/).
1. Run the following in the project directory:

    ```
    npm install
    node server
    ```

1. In another terminal, run the auto-compiler from the project directory:

    ```
    lein cljsbuild auto
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
    (ns client.core)
    (publish-record!)
    ```

1. In the "gif" folder, you should see "anim.gif" and intermediate PNG frames.

## References

- <http://www.tetrisfriends.com/help/tips_appendix.php>
- <http://tetris.wikia.com/wiki/Tetris_Guideline>

