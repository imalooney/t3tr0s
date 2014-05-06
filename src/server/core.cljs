(ns server.core)

(enable-console-print!)

(def express (js/require "express"))
(def http    (js/require "http"))
(def socket  (js/require "socket.io"))
(def fs      (js/require "fs"))

(def port 1984)

(defn init-socket [socket]
  (.watch fs "public/client.js" #(.emit socket "refresh")))

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
    (.set io "log level" 1)
    (.sockets.on io "connection" init-socket)))

(set! *main-cli-fn* -main)
