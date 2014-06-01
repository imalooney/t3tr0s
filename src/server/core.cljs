(ns server.core
  (:require
    [clojure.string :refer [join]]
    [cljs.reader :refer [read-string]]))

(enable-console-print!)

(def port 1984)

(def player-count
  "The current number of players connected."
  (atom 0))

(defn gen-player-id!
  "Create a new unique player ID."
  []
  (swap! player-count inc)
  @player-count)

;;------------------------------------------------------------
;; Node libraries
;;------------------------------------------------------------

(def express (js/require "express"))
(def http    (js/require "http"))
(def socket  (js/require "socket.io"))
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
  "Make an animated gif from the given frames."
  [frames]
  (let [; current working directory
        cwd "gif"

        ; frame filename
        fname (fn [i] (str cwd "/" i ".png"))

        ; create file buffers
        buffers (map #(data-url->buffer (:data-url %)) frames)

        ; create the delays
        dts (-> (map :dt frames)
                (rest)           ; ignore first delay
                (concat [1000])) ; wait 1 second at end of loop

        ; convert delays to imagemagick unit (1/100 s)
        ; (modern browsers don't support gif delays below 0.02s)
        dt->delay #(max 2 (-> % (/ 10) js/Math.floor))
        delays (map dt->delay dts)

        ; create imagemagick command
        gif-file (str cwd "/anim.gif")
        cmd-file (str cwd "/anim.sh")
        cmd (join " " (concat
               ["convert"]
               (map-indexed #(str "-delay " %2 " " (fname %1)) delays)
               ["-layers OptimizeTransparency -loop 0" gif-file]))]

    (println "Received " (count frames) "frames")

    ; Write frame files.
    (doseq [[i buffer] (map-indexed vector buffers)]
      (.writeFileSync fs (fname i) buffer)
      (println "Wrote " (fname i)))

    ; Run imagemagick command for generating gif.
    (println cmd)
    (.writeFile fs cmd-file cmd)
    (exec cmd (fn [error stdout stderr]
                (println "stdout:\n" stdout)
                (println "stderr:\n" stderr)
                (if error
                  (println "ERROR:\n" error)
                  (println "SUCCESS.  Wrote " gif-file))))))

;;------------------------------------------------------------
;; Socket Setup
;;------------------------------------------------------------

(defn init-socket
  "Initialize the web socket."
  [socket]
  (aset socket "user-id" (gen-player-id!))

  ; Emit "refresh" whenever client file changes.
  (.watch fs "public/client.js" #(.emit socket "refresh"))

  ; Create gif whenever "create-gif" is emitted.
  (.on socket "create-gif" #(create-gif (read-string %)))

  ; When a board update comes in, send it to all other players.
  (.on socket "board-update" (fn [data]
                               (let [new-data (assoc (read-string data) :id (aget socket "user-id"))]
                                 (js/console.log "receiving data from user" (:id new-data))
                                 (.. socket -broadcast (emit "board-update" (pr-str new-data))))))

  (.on socket "disconnect" #(.. socket -broadcast (emit "board-delete" (aget socket "user-id"))))

  ;;----------------------------------------------------------
  ;;  Chat
  ;;----------------------------------------------------------
  (.on socket "chat-message" (fn [data]
                               (js/console.log "receiving data from user"  data)
                               (.. socket -broadcast (emit "new-message" data))
                               ))
  )

;;------------------------------------------------------------
;; Main
;;------------------------------------------------------------

(defn -main [& args]

  (let [app    (express)
        server (.createServer http app)
        io     (.listen socket server)]

    ; configure express app
    (doto app
      (.get "/" (fn [req res] (.sendfile res "public/index.html")))
      (.use (.static express (str js/__dirname "/public"))))

    ; start server
    (.listen server port)
    (println "listening on port" port "\n")

    ; configure sockets
    (.sockets.on io "connection" init-socket)))

(set! *main-cli-fn* -main)
