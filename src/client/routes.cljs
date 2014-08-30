(ns client.routes
  (:require
    client.chat
    client.menu
    client.mc
    client.play
    client.spectate
    [client.dom :as dom]
    [client.util :as util]))

;;------------------------------------------------------------------------------
;; Routes
;;------------------------------------------------------------------------------

(def default-route "/menu")

(def routes {
  ;; initial page + login
  "/menu"  [client.menu/init]
  "/login" [client.login/init]

  ;; solo play screen
  "/play-solo" [client.play/init-solo client.play/cleanup]

  ;; the "game loop" screens
  "/lobby" [client.chat/init client.chat/cleanup]
  "/play"  [client.play/init-battle client.play/cleanup]
  "/spectate" [client.spectate/init client.spectate/cleanup]

  ;; MC / admin
  "/mc" [client.mc/init client.mc/cleanup]

  ;; experimental - work in progress
  "/play2" [client.play/init-battle2 client.play/cleanup]

  ;; redirects - where is my HTTP 301? :)
  "/dashboard" [#(aset js/document "location" "hash" "/spectate")]
  })

(def previous-page-cleanup-fn (atom nil))

(defn- on-hash-change []
  (let [new-route (.replace (aget js/document "location" "hash") #"^#" "")
        page (get routes new-route)
        init-fn (first page)
        cleanup-fn (second page)]
    (if-not (or page init-fn)
      (aset js/document "location" "hash" default-route)
      (do
        ;; run cleanup function from last page
        (if (and @previous-page-cleanup-fn
                 (js/goog.isFunction @previous-page-cleanup-fn))
          (@previous-page-cleanup-fn))

        ;; run init function for the new page
        (init-fn)

        ;; store cleanup function for next hash change
        (reset! previous-page-cleanup-fn cleanup-fn)))))

;;------------------------------------------------------------------------------
;; Routes Init
;;------------------------------------------------------------------------------

(def initialized? (atom false))

;; NOTE: this "run-once" function should be called on global app init
(defn init! []
  (when-not @initialized?
    ;; add the event handler
    (aset js/window "onhashchange" on-hash-change)

    ;; kick off the initial page
    (on-hash-change)

    ;; toggle initialized flag
    (reset! initialized? true)))