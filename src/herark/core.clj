(ns herark.core
  (:require [com.stuartsierra.component :as component]
            [slingshot.slingshot :refer [try+ throw+]]
            [taoensso.timbre :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as tc])
  (:import (org.snmp4j.smi UdpAddress OctetString Integer32 VariableBinding OID)
           (org.snmp4j.transport DefaultUdpTransportMapping)
           (org.snmp4j.util ThreadPool MultiThreadedMessageDispatcher)
           (org.snmp4j MessageDispatcherImpl CommandResponder Snmp CommunityTarget PDU)
           (org.snmp4j.mp MPv2c SnmpConstants)
           (java.net InetAddress)
           (org.joda.time DateTime)))


(defrecord SNMPV2CTrapProcessor [proc-name host port community nd responder snmp-entity]

  component/Lifecycle

  (start [component]
    (let [snmp-entity (get component :snmp-entity)]
      (if-not snmp-entity
        (let [host (get component :host)
              port (get component :port)
              responder (get component :responder)
              nd (get component :nd)
              tm (DefaultUdpTransportMapping. (UdpAddress. host port))
              tp (ThreadPool/create (str "SnmpDispatcher." proc-name) nd)
              md (doto
                   (MultiThreadedMessageDispatcher. tp (MessageDispatcherImpl.))
                   (.addMessageProcessingModel (MPv2c.)))
              cr (reify CommandResponder
                   (processPdu [this evt]
                     (responder evt)))
              mgr (doto (Snmp. md tm)
                    (.addCommandResponder cr))]
          (log/info proc-name ": snmp entity created")
          (.listen mgr)
          (log/info proc-name ": snmp entity started")
          (assoc component :snmp-entity mgr))
        (do
          (log/info proc-name ": already started")
          component))))

  (stop [component]
    (let [snmp-entity (get component :snmp-entity)
          proc-name (get component :proc-name)]
      (if snmp-entity
        (do
          (log/info proc-name ": stopping trap processor")
          (.close snmp-entity)
          (assoc component :snmp-entity nil))
        (do
          (log/info proc-name ": already stopped"))))))

(defn- threads-suggestion
  "Calculates the number of cores and +2."
  []
  (+ 2 (.. (Runtime/getRuntime) availableProcessors)))

(defn snmp-v2c-trap-processor
  "Creates a SNMP V2C UPD trap processor with name `proc-name`,
  listening on `host`:`port`.

  Options:

  Name        Desc                            Default
  ------------------------------------------------------------
  :community  SNMP Community                  \"public\"
  :nd         Number of dispatchers           (+ cores 2)
  :responder  (fn [CommandResponderEvent])    No default"
  [proc-name host port & {:keys [community nd]
                          :or   {community "public" nd (threads-suggestion)}
                          :as   options}]
  (let [host (if (string? host) (InetAddress/getByName host) host)]
    (map->SNMPV2CTrapProcessor {:proc-name   proc-name
                                :host        host
                                :port        port
                                :community   community
                                :nd          nd
                                :responder   nil
                                :snmp-entity nil})))

(defn make-v2c-target
  "Creates a SNMP4J V2C UDP target on `host`:`port`."
  [host port community]
  (let [addr (UdpAddress. host port)
        tgt (doto (CommunityTarget.)
              (.setCommunity (OctetString. community))
              (.setVersion SnmpConstants/version2c)
              (.setAddress addr))]
    tgt))

(defn time-ticks
  "Gets time-ticks number of seconds after Jan 01, 2001 UTC
  from a Joda Time DateTime.
  If no parameter is passed, the current instant is assumed."
  ([^DateTime d]
   (-> d
       (tc/to-long)
       (- (tc/to-long (t/date-time 2000 01 01)))
       (/ 1000)
       int))
  ([]
   (time-ticks (t/now))))

(defn make-notification
  "Creates a SNMP V2C notification PDU without additional varbinds.
  If uptime is not passed, the current timestamp is assumed."
  [oid & {:keys [uptime]}]
  (let [uptime (Integer32. (if-not uptime (time-ticks) uptime))
        vb-uptime (VariableBinding. SnmpConstants/sysUpTime uptime)
        oid (OID. oid)
        vb-oid (VariableBinding. SnmpConstants/snmpTrapOID oid)
        ; address (IpAddress. origin)
        ; 1.3.6.1.6.3.18.1.3.0
        ; RFC2576-MIB. Trap forwarding
        ; vb-address (VariableBinding. SnmpConstants/snmpTrapAddress address)
        pdu (doto (PDU.)
              (.add vb-uptime)
              (.add vb-oid)
              ; (.add vb-address)
              (.setType PDU/NOTIFICATION))]
    pdu))

(defn send-pdu
  "Sends the `pdu` PDU to a V2C Manager listening on UDP `host`:`port` with `community`."
  [host port community pdu]
  (log/debug "Sending pdu to: " host ":" port)
  (let [tgt (make-v2c-target host port community)
        transport (doto (DefaultUdpTransportMapping. ))
                    ;(.listen))
        snmp (Snmp. transport)]
    (.send snmp pdu tgt)))
