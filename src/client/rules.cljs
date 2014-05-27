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
  ; GameBoy Tetris speed:
  ; http://tetrisconcept.net/wiki/Tetris_%28Game_Boy%29
  (let [frames (case (min 20 level)
                0 	53
                1 	49
                2 	45
                3 	41
                4 	37
                5 	33
                6 	28
                7 	22
                8 	17
                9 	11
                10 	10
                11 	9
                12 	8
                13 	7
                14 	6
                15 	6
                16 	5
                17 	5
                18 	4
                19 	4
                20 	3)]
    (-> frames (/ 60) (* 1000) js/Math.floor)))
