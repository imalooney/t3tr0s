(ns server.core
  (:require
    [cljs.reader :refer [read-string]]
    [server.gif :refer [create-gif]]))

(enable-console-print!)

(def port 1984)

;;------------------------------------------------------------
;; Node libraries
;;------------------------------------------------------------

(def express (js/require "express"))
(def http    (js/require "http"))
(def socket  (js/require "socket.io"))

;;------------------------------------------------------------
;; Player IDs
;;------------------------------------------------------------

(def player-count
  "The current number of players connected."
  (atom 0))

(defn gen-player-id!
  "Create a new unique player ID."
  []
  (swap! player-count inc)
  @player-count)

;;------------------------------------------------------------
;; Socket Setup
;;------------------------------------------------------------

(defn init-socket
  "Initialize the web socket."
  [socket]
  (aset socket "user-id" (gen-player-id!))

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
      (.get "/" (fn [req res] (.send res (html/page-shell))))
      (.use (.static express (str js/__dirname "/public"))))

    ; start server
    (.listen server port)
    (println "listening on port" port "\n")

    ; configure sockets
    (.sockets.on io "connection" init-socket)))

(set! *main-cli-fn* -main)
