(ns client.multiplayer)

(defn opponent-scale
  "Provides the opponent boards scale"
  [cell-size]
  (let [scale 0.5] ; <- Define the scale multiplier for the opponent boards
    (* cell-size scale)))
