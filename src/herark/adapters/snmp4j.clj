(ns herark.adapters.snmp4j
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [schema.core :as s]
            [herark.core :as hk])
  (:import (org.snmp4j.smi BitString Counter32 Counter64 Gauge32)
           (org.snmp4j.smi GenericAddress Integer32 IpAddress Null OctetString OID)
           (org.snmp4j.smi Opaque SshAddress TcpAddress TimeTicks TlsAddress)
           (org.snmp4j.smi UdpAddress UnsignedInteger32)
           (org.snmp4j.security TsmSecurityParameters)
           (org.snmp4j.transport DefaultUdpTransportMapping)
           (org.snmp4j.util ThreadPool MultiThreadedMessageDispatcher)
           (org.snmp4j MessageDispatcherImpl Snmp CommandResponder CommandResponderEvent)
           (org.snmp4j.mp MPv2c)
           (java.net InetAddress)))

(defprotocol
  ITaggedSMI
  (as-tagged [this]))

;; TODO: Check which types are actually part of the SNMP Specification
(extend-protocol ITaggedSMI
  BitString
  (as-tagged [this]
    [:bit-string (vec (.getValue this))])

  Counter32
  (as-tagged [this]
    [:counter32 (.getValue this)])

  Counter64
  (as-tagged [this]
    [:counter64 (.getValue this)])

  Gauge32
  (as-tagged [this]
    [:gauge32 (.getValue this)])

  GenericAddress
  (as-tagged [this]
    [:generic-address (.toString this)])

  Integer32
  (as-tagged [this]
    [:int32 (.getValue this)])

  IpAddress
  (as-tagged [this]
    [:ip-address (.. this getInetAddress getHostAddress)])

  Null
  (as-tagged [this]
    [:null nil])

  OctetString
  (as-tagged [this]
    [:octet-string (vec (.getValue this))])

  OID
  (as-tagged [this]
    [:oid (vec (.getValue this))])

  Opaque
  (as-tagged [this]
    [:oid (vec (.getValue this))])

  SshAddress
  (as-tagged [this]
    [:ssh-address [(.. this getInetAddress getHostAddress) (.getPort this) (.getUser this)]])

  TcpAddress
  (as-tagged [this]
    [:tcp-address [(.. this getInetAddress getHostAddress) (.getPort this)]])

  TimeTicks
  (as-tagged [this]
    [:time-ticks (.getValue this)])

  TlsAddress
  (as-tagged [this]
    [:tls-address [(.. this getInetAddress getHostAddress) (.getPort this)]])

  TsmSecurityParameters
  (as-tagged [this]
    [:tsm-security-params (vec (.getValue this))])

  UdpAddress
  (as-tagged [this]
    [:udp-address [(.. this getInetAddress getHostAddress) (.getPort this)]])

  UnsignedInteger32
  (as-tagged [this]
    [:unsigned-int32 (.getValue this)]))

(defn- command-responder-event->trap-info
  "Turns `ev` into a map that conforms to SnmpV2CTrapInfo validator"
  [^CommandResponderEvent ev]
  {:post [(nil? (s/check hk/SnmpV2CTrapInfo %))]}
  (let [[t [addr port]] (as-tagged (.getPeerAddress ev))
        proto (case t
                :tcp-address :tcp
                :udp-address :udp
                (throw (IllegalArgumentException. (str "Unexpected address type in event:" ev))))
        community (vec (.getSecurityName ev))
        pdu (.getPDU ev)
        error-status (.getErrorStatus pdu)
        error-index (.getErrorIndex pdu)
        req-id (.. pdu getRequestID getValue)
        varbinds (mapv (fn [x] [(as-tagged (.getOid x))
                                (as-tagged (.getVariable x))])
                       (.getVariableBindings pdu))
        timestamp (System/currentTimeMillis)]
    {:version        :v2c
     :source-address addr
     :source-port    port
     :protocol       proto
     :community      community
     :request-id     req-id
     :error-status   error-status
     :error-index    error-index
     :varbinds       varbinds
     :timestamp      timestamp}))

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
                     (try
                       (-> evt
                           command-responder-event->trap-info
                           responder)
                       (catch Exception e
                         (log/error "Exception caught while dispatching event" e)
                         (log/debug evt)))))
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


