(ns client.rules)

(defn get-points 
  "Determine how many points were scored."
  [rows-cleared]
  (case rows-cleared
    1 (* 40 rows-cleared)
    2 (* 100 rows-cleared)
    3 (* 300 rows-cleared)
    4 (* 1200 rows-cleared)))