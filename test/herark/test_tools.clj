(ns herark.test-tools
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [herark.core :refer :all])
  (:import [java.net BindException]))

(defn- get-candidate-port
  "Generates a random sequence of integers between `min_port` (inclusive) ad `max_port`
  (exclusive).
  If no parametes are passed, assumes `min_port` to be 1025 and `max_port` 60000."
  ([min_port max_port]
   (let [rnd_max (- max_port min_port)]
     (+ min_port (rand-int rnd_max))))
  ([]
   (get-candidate-port 1025 60000)))

(defn make-testing-processor
  "Creates a testing processor for SNMP `version` and bounds the processing function `f`  to it.
  `f` is a function receiving a org.snmp4j.CommandResponderEvent. At the moment,
  `version` can be only :v2c."
  [version f]
  (letfn [(processor-or-nil [port]
                            (log/debug "Trying port: " port)
                            (try
                              (->
                                (component/system-map
                                  :responder f
                                  :processor (component/using
                                               (snmp-v2c-trap-processor (str "processor-at-" port) "localhost" port)
                                               [:responder]))
                                component/start)
                              (catch BindException e
                                (log/debug "Already bound!")
                                nil)))]
    (case version
      :v2c (->>
             (repeatedly get-candidate-port)
             (map processor-or-nil)
             (filter identity)
             first)
      nil)))

