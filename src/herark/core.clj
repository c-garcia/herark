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

(def ^{:const true :doc "2**63-1"}
TWO-63-MINUS-1
  9223372036854775807)

(s/defschema EventTimeStamp
  "Timestamp for Trap Events"
  (s/both s/Int
          (s/pred #(> % 0))
          (s/pred #(< % TWO-63-MINUS-1))))

(s/defschema SNMPVersioned
  "Includes an SNMP Version"
  {(s/required-key :version) s/Keyword
   s/Keyword                 s/Any})

(s/defschema TrapEvent
  "Trap event is a map which will be passed to a responder function.
  Middleware functions are free to enrich this map with any key-value
  pair they feel like."
  {(s/required-key :timestamp) EventTimeStamp
   (s/required-key :message)   SNMPVersioned
   s/Keyword                   s/Any})

(s/defn make-trap-event :- TrapEvent
  "Creates a trap event from a timestamp, message and
  other potential key-value pairs.

  `other-map-elements`: will be passed to `apply hash-map`"
  [timestamp :- EventTimeStamp message :- SNMPVersioned & other-map-elements]
  (let [other (apply hash-map other-map-elements)
        res (assoc other :timestamp timestamp :message message)]
    res))
