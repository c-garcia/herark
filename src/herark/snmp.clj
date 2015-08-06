(ns herark.snmp
  (:require [herark.smi :refer [as-tagged]])
  (:import (org.snmp4j CommandResponderEvent)))


(defprotocol ISnmpCommand
  (as-map [this]))

(extend-protocol ISnmpCommand
  CommandResponderEvent
  (as-map [this]
    {:peer-addr     (as-tagged (.getPeerAddress this))
     :security-name (vec (.getSecurityName this))
     :pdu           {:pdu-type (.. this getPDU getType)
                     :req-id (as-tagged (.. this getPDU getRequestID))
                     :error-status (.. this getPDU getErrorStatus)
                     :error-index (.. this getPDU getErrorIndex)
                     :var-binds (mapv (fn [x] [(as-tagged (.getOid x))
                                               (as-tagged (.getVariable x))])
                                      (.. this getPDU getVariableBindings))}}))

