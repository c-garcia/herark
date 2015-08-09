(ns herark.tools
  (:require [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [taoensso.timbre :as log])
  (:import (org.snmp4j.smi UdpAddress OctetString Integer32 VariableBinding OID)
           (org.snmp4j CommunityTarget PDU Snmp)
           (org.snmp4j.mp SnmpConstants)
           (org.snmp4j.transport DefaultUdpTransportMapping)))

(defn make-v2c-target
  "Creates a SNMP4J V2C UDP target on `host`:`port` and `community`.
  `community` can be specificed as a byte-array or String."
  [^String host ^String port community]
  (let [^bytes community-b (if (string? community) (.getBytes ^String community) community)
        addr (UdpAddress. host port)
        tgt (doto (CommunityTarget.)
              (.setCommunity (OctetString. community-b))
              (.setVersion SnmpConstants/version2c)
              (.setAddress addr))]
    tgt))

(defn time-ticks
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

(defn make-notification-pdu
  "Creates a SNMP V2C notification PDU without additional varbinds from an OID in dotted string form: `oid`.
  If uptime is not passed, the current timestamp is assumed."
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

(defn send-pdu
  "Sends the `pdu` PDU to a V2C Manager listening on UDP `host`:`port` with `community`."
  [host port community pdu]
  (log/debug "Sending pdu to: " host ":" port)
  (let [tgt (make-v2c-target host port community)
        transport (doto (DefaultUdpTransportMapping.))
        ;(.listen))
        snmp (Snmp. transport)]
    (.send snmp pdu tgt)))
