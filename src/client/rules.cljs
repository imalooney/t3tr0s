(ns client.rules)

(defn get-points
  "Determine how many points were scored."
  [rows-cleared level]
  (case rows-cleared
    1 (* 40 level)
    2 (* 100 level)
    3 (* 300 level)
    4 (* 1200 level)))

(defn level-up?
  "Determine if we should level up given the current level lines cleared."
  [level-lines]
  (>= level-lines 10))

(defn get-level-speed 
  "Determine speed of gravity at current level in ms/drop."
  [level]
  ; Taken from: http://www.gamedev.net/topic/430237-tetris-block-speeds/
  (case level
    0 1000
    1 660
    2 440
    3 290
    4 190
    5 120
    6 80
    7 50
    8 30
    9 20))
