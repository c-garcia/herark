(ns herark.smi.smiv1-test
  (:require [clojure.test :refer :all]
            [herark.smi.smiv1 :refer :all]
            [herark.smi.smiv1 :as smiv1]
            [herark.smi.misc :refer :all]
            [schema.core :as s]))

(deftest oid-test
  (testing "Valid OIDs"
    (are [x] (not (s/check OID x))
             [::smiv1/oid [1 2]]
             [::smiv1/oid [1]]
             [::smiv1/oid [1 232322]]
             [::smiv1/oid (repeat 200 1)]))
  (testing "Invalid OIDs"
    (are [x] (s/check OID x)
             nil
             1
             'a
             ::smiv1/oid
             [:other]
             [:other [1 2]]
             [:other []]
             [::smiv1/oid]
             [::smiv1/oid []]
             [::smiv1/oid [nil]]
             [::smiv1/oid [1 -1]]
             [::smiv1/oid [1 2 'a]])))

(deftest int-test
  (testing "Valid Ints"
    (are [x] (not (s/check Int x))
             [::smiv1/int -1]
             [::smiv1/int 0]
             [::smiv1/int 1]))
  (testing "Invalid Ints"
    (are [x] (s/check Int x)
             nil
             1
             'a
             ::smiv1/int
             [::smiv1/int]
             [::smiv1/int 'a])))

(deftest counter-test
  (testing "Valid Counters"
    (are [x] (not (s/check Counter x))
             [::smiv1/counter 0]
             [::smiv1/counter TWO-32-MINUS-1]
             [::smiv1/counter 128]))
  (testing "Invalid Counters"
    (are [x] (s/check Counter x)
             nil
             1
             'a
             ::smiv1/counter
             [::smiv1/counter -1]
             [::smiv1/counter (inc TWO-32-MINUS-1)])))

(deftest gauge-test
  (testing "Valid Gauges"
    (are [x] (not (s/check Gauge x))
             [::smiv1/gauge 0]
             [::smiv1/gauge TWO-32-MINUS-1]
             [::smiv1/gauge 128]))
  (testing "Invalid Gauges"
    (are [x] (s/check Gauge x)
             nil
             1
             'a
             ::smiv1/gauge
             [::smiv1/gauge -1]
             [::smiv1/gauge (inc TWO-32-MINUS-1)])))


(deftest time-ticks-test
  (testing "Valid TimeTicks"
    (are [x] (not (s/check TimeTicks x))
             [::smiv1/time-ticks 0]
             [::smiv1/time-ticks TWO-32-MINUS-1]
             [::smiv1/time-ticks 128]))
  (testing "Invalid TimeTicks"
    (are [x] (s/check TimeTicks x)
             nil
             1
             'a
             ::smiv1/time-ticks
             [::smiv1/time-ticks -1])))

(deftest octet-string-test
  (testing "Valid OctetStrings"
    (are [x] (not (s/check OctetString x))
             [::smiv1/octet-string [255]]
             [::smiv1/octet-string []]
             [::smiv1/octet-string [1 -127]]))
  (testing "Invalid OctetStrings"
    (are [x] (s/check OctetString x)
             nil
             1
             'a
             ::smiv1/octet-string
             [::smiv1/octet-string [-200]]
             [::smiv1/octet-string [256]])))

(deftest opaque-test
  (testing "Valid Opaques"
    (are [x] (not (s/check Opaque x))
             [::smiv1/opaque [255]]
             [::smiv1/opaque []]
             [::smiv1/opaque [1 -127]]))
  (testing "Invalid Opaques"
    (are [x] (s/check Opaque x)
             nil
             1
             'a
             ::smiv1/opaque
             [::smiv1/opaque [-200]]
             [::smiv1/opaque [256]])))

(deftest ip-address-test
  (testing "Valid IP Addresses"
    (are [x] (not (s/check IPAddress x))
             [::smiv1/ip-address "192.168.1.1"]
             [::smiv1/ip-address "255.255.255.255"]
             [::smiv1/ip-address "0.0.0.0"]))
  (testing "Invalid IP Addresses"
    (are [x] (s/check IPAddress x)
             nil
             1
             'a
             ::smiv1/ip-address
             [::smiv1/ip-address 1]
             [::smiv1/ip-address "300.1.2.3"]
             [::smiv1/ip-address "1.2.3"])))
