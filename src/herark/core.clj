;; ## Trap processing in clojure
(ns herark.core
  "Herark is a library for SNMP trap processing inspired by Ring.
  In this namespace, `herark.core`, we can find the definitions of
  the highest level data structures that the trap processors need
  to look after: `TrapEvent`.

  A TrapEvent is an associative structure that includes a, at a minimum:

  * `:timestamp`: a timestamp in JVM format (long).
  * `:message`: a SNMP trap message whose format will depend on the SNMP version the
     sending agent is using.

  Trap message formats are defined in smi namespaces."
  (:require [schema.core :as s]
            [herark.smi.misc :refer :all]))

(s/defrecord TrapEvent [timestamp :- long
                        message :- (s/pred #(contains? % :version))])

(defn make-trap-event
  "Creates a trap event from a map or receiving timestamp, message and
  other potential key-value pairs."
  ([options]
   (map->TrapEvent options))
  ([timestamp message & {:as other}]
   (let [options (assoc other :timestamp timestamp :message message)]
     (make-trap-event options))))
