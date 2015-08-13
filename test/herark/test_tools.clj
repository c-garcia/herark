(ns herark.test-tools
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [herark.core :refer :all])
  (:import [java.net BindException]))

(defn- get-random-port
  "Generates a random sequence of integers between `min_port` (inclusive) ad `max_port`
  (exclusive).
  If no parametes are passed, assumes `min_port` to be 1025 and `max_port` 60000."
  ([min_port max_port]
   (let [rnd_max (- max_port min_port)]
     (+ min_port (rand-int rnd_max))))
  ([]
   (get-random-port 1025 60000)))

(defn try-on-random-port
  "Tries to invoke a function of a single parameter (the port) on a random port. If
  the function throws BindException or nil, retries. If not, returns the result returned
   by the function"
  [f]
  (letfn [(object-or-nil [p]
                         (log/debug "Trying on port: " p)
                         (try
                           (f p)
                           (catch BindException e
                             (log/debug "Already bound!"))))]
    (->> (repeatedly get-random-port)
         (map object-or-nil)
         (filter identity)
         first)))

