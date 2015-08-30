;; ## Trap processing in clojure
;; * One
;; * Two
(ns herark.core
  "Library for SNMP trap processing inspired by ring."
  (:require [schema.core :as s]
            [herark.smi.misc :refer :all]))

(s/defrecord TrapEvent [timestamp :- long
                        message :- (s/pred #(contains? % :version))])

(defn make-trap-event
  ([options]
   (map->TrapEvent options))
  ([timestamp message & {:as other}]
   (let [options (assoc other :timestamp timestamp :message message)]
     (make-trap-event options))))
