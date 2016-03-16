(ns server.gif
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [clojure.string :refer [join]]
    [cljs.core.async :refer [close! chan <! timeout]]))

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

(defn images-and-delays
  "Create a sequence of image frame names and delays for the given frames."
  [frames]
  (let [; current working directory
        cwd "gif"

        ; frame filename
        fname (fn [i] (str cwd "/" i ".png"))

        ; Create a list of the image filenames.
        images (map fname (range (count frames)))

        ; create the delays
        dts (-> (map :dt frames)
                (rest)           ; ignore first delay
                (concat [1000])) ; wait 1 second at end of loop

        ; convert delays to imagemagick unit (1/100 s)
        ; (modern browsers don't support gif delays below 0.02s)
        dt->delay #(max 2 (-> % (/ 10) js/Math.floor))
        delays (map dt->delay dts)]

    [images delays]))


(defn resize-screenshots
  []
  (let [done-chan (chan)]

    (exec "sips --resampleWidth 674 gif/*.png"
          (fn [error stdout stderr]
            (if error
              (println "ERROR:\n" error)
              (do (println "SUCCESS. resized images")
                  (close! done-chan)))))

    done-chan))

(defn screenshot
  [imagename]

  (let [done-chan (chan)]
    ; Use the Mac command `screencapture` to take a screenshot of the browser.
    ; This of course means the server and client need to run on the same computer,
    ; and the browser must remain in view while the server is taking the screenshot.
    ; NOTE: the specified region below works on a full firefox window zoomed out 4x
    (exec (str "screencapture -R390,105,674,400 " imagename)
          (fn [error stdout stderr]
            (if error
              (println "ERROR:\n" error)
              (do (println "SUCCESS.  Wrote " imagename)
                  (close! done-chan)))))

    done-chan))

(defn screenshot-state
  "Take a screenshot of the given state on the given socket.  Returns a channel that will receive a value when done."
  [state imagename socket]

  (go

    ; Set the client's state to render the correct view.
    (.emit socket "set-state" (pr-str state))

    ; Allow some time for the state to be set
    (<! (timeout 100))

    ; Wait for screenshot to be taken.
    (<! (screenshot imagename))))

(defn create-html-gif
  "Create an animated gif from the given states using Mac's `screencapture` command.  (Slow)"
  [frames socket]
  (let [[images delays] (images-and-delays frames)]

    ; Continue in a go-block because we have to wait on
    ; asynchronous screenshot operations.
    (go

      ; Create all the screenshots.
      (let [done-chan (chan)]
        (doseq [[frame img] (map vector frames images)]
          (<! (screenshot-state (:state frame) img socket))))

      ; Downscale the screenshots.
      (<! (resize-screenshots))

      ; Create the gif.
      (create-gif delays images))))


(defn create-canvas-gif
  "Make an animated gif from the given frame images."
  [frames]
  (let [[images delays] (images-and-delays frames)
        buffers (map #(data-url->buffer (:data-url %)) frames)]

    (println "Received " (count frames) "frames")

    ; Write frame files.
    (doseq [[img buffer] (map vector images buffers)]
      (.writeFileSync fs img buffer)
      (println "Wrote " img))

    (create-gif delays images)))
