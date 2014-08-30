(ns client.state
  (:require
    [client.util :as util]))

;; NOTE: this module contains "global state" information

;; create a new UUID everytime the page loads
(def client-id (util/uuid))

;;------------------------------------------------------------------------------
;; Global State Atoms
;;------------------------------------------------------------------------------

;; vector containing chat messages
(def chat (atom []))

;; current state of the "game loop"
;; ie: in the lobby waiting, playing a round, how much time is left, etc
(def round-state (atom nil))

;; a map of all the players currently connected and their states
(def players (atom {}))

;; current player's username
(def username (atom nil))