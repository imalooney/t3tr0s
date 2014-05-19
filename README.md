# t3tr0s

<img src="http://i.imgur.com/XzonCuN.gif" align="right" width="100px">

A 30th anniversary celebration of a certain tetrominos game... called Tetris.
It was created by Alexey Pajitnov ["to produce the pleasure, to make
fun"](https://www.youtube.com/watch?v=nTDRY8aPy7c).  Tetris was released on
June 6, 1984.

__DevBlog__

- [Day 1](devblog/day01.md)
- [Day 2](devblog/day02.md)
- [Day 3](devblog/day03.md)

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

If you want a REPL connected to the browser for testing:

```
lein repl
> (brepl)
```

### Recording GIFs

If you want to record a GIF of gameplay:

1. Create empty "gif" directory at the project root.
1. Open the game in the browser.
1. Open the Javascript console.
1. Start playing.
1. Press "shift" to start/stop recording. (new ones override old ones)
1. In browser REPL:

    ```clj
    (ns client.core)
    (publish-record!)
    ```

1. In the "gif" folder, you should see "anim.gif" and intermediate PNG frames.

## Planning

__References__

- <http://www.tetrisfriends.com/help/tips_appendix.php>
- <http://tetris.wikia.com/wiki/Tetris_Guideline>

__Pieces Data__

- 7 piece types (I,L,J,S,Z,O,T)
- Pick rotation center for each piece
- Pick start rotation and position of each piece
- Board Size (10x22, top two hidden)

__Piece Controls__

- Move
- Rotate
- Soft Drop (time allowed to rotate/move before registering a drop)
- Hard Drop (immediate drop)

__Visuals__

- Themes change per level, as a montage of the game's evolution
- "Ghost" piece to allow easy hard-drops
- Preview of next pieces

__Scoring/Difficulty__

- Piece Drop score (some points rewarded for every drop)
- Row Clear score (some points rewarded for # of rows cleared at once, with level multiplier)
- Level Progression (clear n rows to advance a level)
- Drop Speed increases for each level

__Modes__

- Single Player
- Multiplayer
    - Server (simple http request/response)
    - Lobby (view current players, set name, set color)
    - Game (view live bar chart of current scores with finish line at top)
    - Round End (show who won, with current tally, and time until next round)
    - Game End (show final winner, with end tally, and ability to start new game)


