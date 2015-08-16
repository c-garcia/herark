(ns herark.core
  (:require [schema.core :as s]
            [herark.smi.smiv1 :as smiv1]
            [herark.smi.smiv2 :as smiv2]
            [herark.smi.misc :refer :all]))



#_(def SnmpV2CTrapInfo {(s/required-key :version)        (s/eq :v2c)
                        (s/required-key :source-address) s/Str
                        (s/required-key :source-port)    (s/pred is_port?)
                        (s/required-key :protocol)       (s/enum :tcp :udp)
                        (s/required-key :community)      [s/Int]
                        (s/required-key :request-id)     s/Int
                        (s/required-key :error-status)   s/Int
                        (s/required-key :error-index)    s/Int
                        (s/required-key :varbinds)       [SMIv2VarBind]
                        (s/required-key :timestamp)      long
                        s/Keyword                        s/Any})


(s/defrecord TrapEvent [timestamp :- long
                        message :- (s/pred #(contains? % :version))])

(defn make-trap-event
  ([options]
   (map->TrapEvent options))
  ([timestamp message & {:as other}]
   (let [options (assoc other :timestamp timestamp :message message)]
     (make-trap-event options))))
