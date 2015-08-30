(ns herark.adapters.tools
  "Tools to help implementing or testing adapters when sending actual PDU over the wire
  is required. The need of these functions to be close to the lowest layer, makes them
  to to use the abstractions provided in the `herark.smi` namespaces.
  The functions here are built using the SNMP4J stack."
  (:require [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [taoensso.timbre :as log])
  (:import (org.snmp4j.smi UdpAddress OctetString Integer32 VariableBinding OID TcpAddress)
           (org.snmp4j CommunityTarget PDU Snmp)
           (org.snmp4j.mp SnmpConstants)
           (org.snmp4j.transport DefaultUdpTransportMapping DefaultTcpTransportMapping)))

(defn- make-v2c-target
  "Creates a SNMP4J V2C UDP target on `host`:`port` and `community`.
  `community` can be specificed as a byte-array or String."
  [^String host ^String port community & {:keys [proto] :or {proto :udp}}]
  (let [^bytes community-b (if (string? community) (.getBytes ^String community) community)
        addr (if (= :udp proto)
               (UdpAddress. host port)
               (TcpAddress. host port))
        tgt (doto (CommunityTarget.)
              (.setCommunity (OctetString. community-b))
              (.setVersion SnmpConstants/version2c)
              (.setAddress addr))]
    tgt))

(defn- time-ticks
  "As per the SNMP standard, gets the hundredths of seconds between a date, `s`  and now.
  If no parameter is passed, the current day at midnight (UTC) is assumed to be the starting point."
  ([s]
   (-> (t/now)
       (tc/to-long)
       (- (tc/to-long s))
       (/ 100)
       int))
  ([]
   (time-ticks (t/today-at-midnight))))

(defn make-notification-v2c-pdu
  "Creates a specific implementation version of the SNMP V2C notification PDU
  *only intented to interact with the functions in this namespace*.

  * `oid`:    (string) oid in 1.2.4.5 format.
  * `uptime`: (integer) uptime. Default, centi-seconds since midnight."
  [^String oid & {:keys [uptime]}]
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

(defn send-v2c-pdu
  "Sends a SNMP v2c PDU to a manager:

  * `host`:       (string) the hostname of the manager.
  * `port`:       (integer) the port
  * `community`:  (string or byte array) the community.
  * `proto`:      (keyword) :tcp or :udp
  * `pdu`:        (pdu) pdu as created by `make-notification-v2c-pdu`."
  [host port community pdu & {:keys [proto] :or {proto :udp}}]
  (log/debug "Sending pdu to: " host ":" port)
  (let [tgt (make-v2c-target host port community :proto proto)
        tm (if (= :udp proto)
             (DefaultUdpTransportMapping.)
             (DefaultTcpTransportMapping.))
        transport (doto tm)
        ;(.listen))
        snmp (Snmp. transport)]
    (.send snmp pdu tgt)))
