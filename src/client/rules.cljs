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
