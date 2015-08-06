(ns herark.smi
  (:import (org.snmp4j.smi BitString BitString Counter32 Counter64 Gauge32)
           (org.snmp4j.smi GenericAddress Integer32 IpAddress Null OctetString OID)
           (org.snmp4j.smi Opaque SshAddress TcpAddress TimeTicks TlsAddress)
           (org.snmp4j.smi UdpAddress UnsignedInteger32)
           (org.snmp4j.security TsmSecurityParameters)))

(defprotocol
  ITaggedSMI
  (as-tagged [this]))

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
    [:ip-address (.getInetAddress this)])

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
    [:ssh-address [(.getInetAddress this) (.getPort this) (.getUser this)]])

  TcpAddress
  (as-tagged [this]
    [:tcp-address [(.getInetAddress this) (.getPort this)]])

  TimeTicks
  (as-tagged [this]
    [:time-ticks (.getValue this)])

  TlsAddress
  (as-tagged [this]
    [:tls-address [(.getInetAddress this) (.getPort this)]])

  TsmSecurityParameters
  (as-tagged [this]
    [:tsm-security-params (vec (.getValue this))])

  UdpAddress
  (as-tagged [this]
    [:udp-address [(.getInetAddress this) (.getPort this)]])

  UnsignedInteger32
  (as-tagged [this]
    [:unsigned-int32 (.getValue this)]))

