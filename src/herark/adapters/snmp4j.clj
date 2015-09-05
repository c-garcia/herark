(ns herark.adapters.snmp4j
  "Herark adapter implemented using, as foundations:

  * The SNMP4J toolkit
  * The Stuart Sierra's component library.

  ### Interesting functions

  * `make-snmp-v2c-processor`: creates an SNMPv2c trap processor implementing
      the Component protocol."
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [schema.core :as s]
            [herark.core :as hk]
            [herark.smi.smiv1 :as smiv1]
            [herark.smi.smiv2 :as smiv2])
  (:import (org.snmp4j.smi BitString Counter32 Counter64 Gauge32)
           (org.snmp4j.smi GenericAddress Integer32 IpAddress Null OctetString OID)
           (org.snmp4j.smi Opaque SshAddress TcpAddress TimeTicks TlsAddress)
           (org.snmp4j.smi UdpAddress UnsignedInteger32 VariableBinding)
           (org.snmp4j.security TsmSecurityParameters)
           (org.snmp4j.transport DefaultUdpTransportMapping DefaultTcpTransportMapping)
           (org.snmp4j.util ThreadPool MultiThreadedMessageDispatcher)
           (org.snmp4j MessageDispatcherImpl Snmp CommandResponder CommandResponderEvent PDU PDUv1)
           (org.snmp4j.mp MPv2c MessageProcessingModel)
           (java.net InetAddress)))

(defmulti as-tagged
          "Transforms an object into a tagged value. It will
          be used to transform SNMP Libraries object types into
          `herark.smi` representations."
          (fn [x] (class x)))

(defmethod as-tagged BitString [x]
  [:bit-string (vec (.getValue x))])

(defmethod as-tagged Counter32 [x]
  [::smiv2/counter32 (.getValue x)])

(defmethod as-tagged Counter64 [x]
  [::smiv2/counter64 (.getValue x)])

(defmethod as-tagged Gauge32 [x]
  [::smiv2/gauge32 (.getValue x)])

(defmethod as-tagged Integer32 [x]
  [::smiv2/int32 (.getValue x)])

(defmethod as-tagged IpAddress [x]
  [::smiv2/ip-address (.. x getInetAddress getHostAddress)])

;; this is not an SMIv2 type
(defmethod as-tagged Null [_]
  [::null nil])

(defmethod as-tagged OctetString [x]
  [::smiv2/octet-string (vec (.getValue x))])

(defmethod as-tagged OID [x]
  [::smiv2/oid (vec (.getValue x))])

(defmethod as-tagged Opaque [x]
  [::smiv2/opaque (vec (.getValue x))])

(defmethod as-tagged SshAddress [x]
  [::ssh-address [(.. x getInetAddress getHostAddress) (.getPort x) (.getUser x)]])

(defmethod as-tagged TcpAddress [x]
  [::tcp-address [(.. x getInetAddress getHostAddress) (.getPort x)]])

(defmethod as-tagged TimeTicks [x]
  [::smiv2/time-ticks (.getValue x)])

(defmethod as-tagged TlsAddress [x]
  [::tls-address [(.. x getInetAddress getHostAddress) (.getPort x)]])

(defmethod as-tagged TsmSecurityParameters [x]
  [::tsm-security-params (vec (.getValue x))])

(defmethod as-tagged UdpAddress [x]
  [::udp-address [(.. x getInetAddress getHostAddress) (.getPort x)]])

(defmethod as-tagged UnsignedInteger32 [x]
  [::smiv2/uint32 (.getValue x)])

(defmulti as-pdu
          "Transforms a SNMP4J PDU into a PDU record as defined by herark.smi."
          (fn [p] (class p)))

(defmethod as-pdu PDU [p]
  (let [error-index [::smiv2/int32 (.getErrorIndex p)]
        error-status [::smiv2/int32 (.getErrorStatus p)]
        req-id [::smiv2/int32 (.getRequestID p)]
        varbinds (mapv (fn [^VariableBinding x] [(as-tagged (.getOid x))
                                                 (as-tagged (.getVariable x))])
                       (.getVariableBindings p))]
    (smiv2/make-v2-trap-pdu req-id error-status error-index varbinds)))

(defmethod as-pdu PDUv1 [p]
  (let [enterprise (vec (.getEnterprise p))
        [_ source-address] (as-tagged (.getAgentAddress p))
        source-address [::smiv1/ip-address source-address]
        generic-trap [::smiv1/int (.getGenericTrap p)]
        specific-trap [::smiv1/int (.getSpecificTrap p)]
        timestamp [::smiv1/time-ticks (.getTimestamp p)]
        varbinds (mapv (fn [^VariableBinding x] [(as-tagged (.getOid x))
                                                 (as-tagged (.getVariable x))])
                       (.getVariableBindings p))]
    (smiv1/make-v1-trap-pdu
      enterprise
      source-address
      generic-trap
      specific-trap
      timestamp
      varbinds)))

(defmethod as-pdu :default [p]
  (throw (IllegalArgumentException. (str "Illegal PDU received: " (.getType p)))))

(defmulti as-message
          "Transforms a SNMP4J CommandResponderEvent into a Message record as defined by herark.smi."
          (fn [^CommandResponderEvent e] (.. e getMessageProcessingModel)))

(defmethod as-message MessageProcessingModel/MPv2c [e]
  (if (not= PDU/TRAP (.. e getPDU getType))
    (throw (IllegalArgumentException. (str "While in MPv2c, received a trap type: " (.. e getPDU getType)))))
  (let [[_ [source-address _]] (as-tagged (.getPeerAddress e))
        community (vec (.getSecurityName e))
        pdu (as-pdu (.getPDU e))]
    (smiv2/make-v2-trap-message source-address community pdu)))

(defmethod as-message MessageProcessingModel/MPv1 [e]
  (if (not= PDU/V1TRAP (.. e getPDU getType))
    (throw (IllegalArgumentException. (str "While in MPv1, received a trap type: " (.. e getPDU getType)))))
  (let [[_ [source-address _]] (as-tagged (.getPeerAddress e))
        community (vec (.getSecurityName e))
        pdu (as-pdu (.getPDU e))]
    (smiv1/make-v1-trap-message source-address community pdu)))

(defmethod as-message :default [e]
  (throw (IllegalArgumentException. (str "Illegal message processing model: " e))))

(defn- command-responder-event->trap-event
  "For every CommandResponderEvent, it generates an event map with a timestamp and
  the message as created by as-message."
  [^CommandResponderEvent ev]
  (let [now (System/currentTimeMillis)
        message (as-message ev)]
    (hk/make-trap-event now message)))

(defrecord SNMPV2CTrapProcessor [proc-name host port proto nd responder snmp-entity]

  component/Lifecycle

  (start [component]
    (let [snmp-entity (get component :snmp-entity)]
      (if-not snmp-entity
        (let [host (get component :host)
              port (get component :port)
              proto (get component :proto)
              responder (get component :responder)
              nd (get component :nd)
              tm (if (= proto :udp)
                   (DefaultUdpTransportMapping. (UdpAddress. host port))
                   (DefaultTcpTransportMapping. (TcpAddress. host port)))
              tp (ThreadPool/create (str "SnmpDispatcher." proc-name) nd)
              md (doto
                   (MultiThreadedMessageDispatcher. tp (MessageDispatcherImpl.))
                   (.addMessageProcessingModel (MPv2c.)))
              cr (reify CommandResponder
                   (processPdu [this evt]
                     (try
                       (-> evt
                           command-responder-event->trap-event
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


(defn make-snmp-v2c-trap-processor
  "Creates a SNMP V2C UPD trap processor with name `proc-name`,
  listening on `host`:`port`.

  Options:

  Name        Desc                            Default
  ------------------------------------------------------------
  :nd         Number of dispatchers           (+ cores 2)
  :responder  (fn [CommandResponderEvent])    No default
  :proto      Protocol: :tcp / :udp           :udp"
  [proc-name host port & {:keys [nd proto]
                          :or   {nd (threads-suggestion) proto :udp}
                          :as   options}]
  (let [host (if (string? host) (InetAddress/getByName host) host)]
    (map->SNMPV2CTrapProcessor {:proc-name   proc-name
                                :host        host
                                :port        port
                                :proto       proto
                                :nd          nd
                                :responder   nil
                                :snmp-entity nil})))


