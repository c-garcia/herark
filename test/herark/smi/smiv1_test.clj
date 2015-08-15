(ns herark.smi.smiv1-test
  (:require [clojure.test :refer :all]
            [herark.smi.smiv1 :refer :all]
            [herark.smi.misc :refer :all]
            [schema.core :as s]))

(deftest oid-test
  (testing "Valid OIDs"
    (are [x] (not (s/check OID x))
             [:oid [1 2]]
             [:oid [1]]
             [:oid [1 232322]]
             [:oid (repeat 200 1)]))
  (testing "Invalid OIDs"
    (are [x] (s/check OID x)
             nil
             1
             'a
             :oid
             [:other]
             [:other [1 2]]
             [:other []]
             [:oid]
             [:oid []]
             [:oid [nil]]
             [:oid [1 -1]]
             [:oid [1 2 'a]])))

(deftest int-test
  (testing "Valid Ints"
    (are [x] (not (s/check Int x))
             [:int -1]
             [:int 0]
             [:int 1]))
  (testing "Invalid Ints"
    (are [x] (s/check Int x)
             nil
             1
             'a
             :int
             [:int]
             [:int 'a])))

(deftest counter-test
  (testing "Valid Counters"
    (are [x] (not (s/check Counter x))
             [:counter 0]
             [:counter TWO_32_MINUS_1]
             [:counter 128]))
  (testing "Invalid Counters"
    (are [x] (s/check Counter x)
             nil
             1
             'a
             :counter
             [:counter -1]
             [:counter (inc TWO_32_MINUS_1)])))

(deftest gauge-test
  (testing "Valid Gauges"
    (are [x] (not (s/check Gauge x))
             [:gauge 0]
             [:gauge TWO_32_MINUS_1]
             [:gauge 128]))
  (testing "Invalid Gauges"
    (are [x] (s/check Gauge x)
             nil
             1
             'a
             :gauge
             [:gauge -1]
             [:gauge (inc TWO_32_MINUS_1)])))


(deftest time-ticks-test
  (testing "Valid TimeTicks"
    (are [x] (not (s/check TimeTicks x))
             [:time-ticks 0]
             [:time-ticks TWO_32_MINUS_1]
             [:time-ticks 128]))
  (testing "Invalid TimeTicks"
    (are [x] (s/check TimeTicks x)
             nil
             1
             'a
             :time-ticks
             [:time-ticks -1])))

(deftest octet-string-test
  (testing "Valid OctetStrings"
    (are [x] (not (s/check OctetString x))
             [:octet-string [255]]
             [:octet-string []]
             [:octet-string [1 -127]]))
  (testing "Invalid OctetStrings"
    (are [x] (s/check OctetString x)
             nil
             1
             'a
             :octet-string
             [:octet-string [-200]]
             [:octet-string [256]])))

(deftest opaque-test
  (testing "Valid Opaques"
    (are [x] (not (s/check Opaque x))
             [:opaque [255]]
             [:opaque []]
             [:opaque [1 -127]]))
  (testing "Invalid Opaques"
    (are [x] (s/check Opaque x)
             nil
             1
             'a
             :opaque
             [:opaque [-200]]
             [:opaque [256]])))

(deftest ip-address-test
  (testing "Valid IP Addresses"
    (are [x] (not (s/check IPAddress x))
             [:ip-address "192.168.1.1"]
             [:ip-address "255.255.255.255"]
             [:ip-address "0.0.0.0"]))
  (testing "Invalid IP Addresses"
    (are [x] (s/check IPAddress x)
             nil
             1
             'a
             :ip-address
             [:ip-address 1]
             [:ip-address "300.1.2.3"]
             [:ip-address "1.2.3"])))
