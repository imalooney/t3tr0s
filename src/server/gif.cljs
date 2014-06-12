(ns server.gif
  (:require
    [clojure.string :refer [join]]))

;;------------------------------------------------------------
;; Node libraries
;;------------------------------------------------------------

(def fs      (js/require "fs"))
(def exec    (.-exec (js/require "child_process")))

;;------------------------------------------------------------
;; Animated GIF creator
;;------------------------------------------------------------

(defn data-url->buffer
  "Converts a Data URL to a file buffer (for writing to file)."
  [data-url]
  (let [regex #"^data:.+/(.+);base64,(.*)$"
       [_ ext data] (.match data-url regex)]
    (js/Buffer. data "base64")))

(defn create-gif
  "Create an animated gif from the given image names and delays."
  [delays images]
  (let [cwd "gif"
        gif-file (str cwd "/anim.gif")
        cmd-file (str cwd "/anim.sh")
        cmd (join " " (concat
               ["convert"]
               (map #(str "-delay " %1 " " %2) delays images)
               ["-layers OptimizeTransparency -loop 0" gif-file]))]

    ; Run imagemagick command for generating gif.
    (println cmd)
    (.writeFile fs cmd-file cmd)
    (exec cmd (fn [error stdout stderr]
                (println "stdout:\n" stdout)
                (println "stderr:\n" stderr)
                (if error
                  (println "ERROR:\n" error)
                  (println "SUCCESS.  Wrote " gif-file))))))

(defn create-html-gif
  "Create an animated gif from the given states using Mac's `screencapture` command.  (Slow)"
  []
  nil)

(defn create-canvas-gif
  "Make an animated gif from the given frame images."
  [frames]
  (let [; current working directory
        cwd "gif"

        ; frame filename
        fname (fn [i] (str cwd "/" i ".png"))

        ; Create a list of the image filenames.
        images (map fname (range (count frames)))

        ; create file buffers
        buffers (map #(data-url->buffer (:data-url %)) frames)

        ; create the delays
        dts (-> (map :dt frames)
                (rest)           ; ignore first delay
                (concat [1000])) ; wait 1 second at end of loop

        ; convert delays to imagemagick unit (1/100 s)
        ; (modern browsers don't support gif delays below 0.02s)
        dt->delay #(max 2 (-> % (/ 10) js/Math.floor))
        delays (map dt->delay dts)]

    (println "Received " (count frames) "frames")

    ; Write frame files.
    (doseq [[img buffer] (map vector images buffers)]
      (.writeFileSync fs img buffer)
      (println "Wrote " img))

    (create-gif delays images)))
