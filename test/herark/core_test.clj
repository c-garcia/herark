(ns herark.core-test
  (:require [clojure.test :refer :all]
            [herark.test-tools :refer :all]
            [herark.core :refer :all]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:import (org.snmp4j CommandResponderEvent)
           (org.snmp4j.smi OID)))

(deftest snmp-v2c-trap-received
  (testing "Given a function to be invoked when a trap is received"
    (let [flag (promise)
          process-trap (fn [_] (log/debug "I got hit!") (deliver flag true))]
      (testing "and a  v2c trap processor with this function as responder"
        (let [tp (make-testing-processor :v2c process-trap)]
          (log/debug "Created testing processor: " (get-in tp [:processor]))
          (testing "When I send a v2c trap to this processor"
            (let [pdu (make-notification ".1.2.3.4")
                  _ (send-pdu (get-in tp [:processor :host]) (get-in tp [:processor :port]) "public" pdu)
                  invoked? (deref flag 3000 false)]
              (is invoked? "the function is invoked")))
          (component/stop tp))))))

(deftest snmp-v2c-trap-equals-sent
  (let [trap-oid-str ".1.3.6.1.6.3.1.1.5.3"]
    (testing (str "Given a trap with OID:" trap-oid-str)
      (testing "And a v2c trap processor"
        (let [received-trap-oid (promise)
              f (fn [^CommandResponderEvent e]
                  (log/debug "I got hit!")
                  (try
                    (let [oid (.. e getPDU (get 1) getVariable getValue)]
                      (log/debug "Received OID:" (OID. oid))
                      (deliver received-trap-oid oid))
                    (catch Exception ex
                      (log/error "Exception when accessing event:" ex)
                      (deliver received-trap-oid "EXCEPTION"))))
              tp (make-testing-processor :v2c f)]
          (testing "when I send a notification with that OID"
            (let [pdu (make-notification trap-oid-str)
                  _ (send-pdu (get-in tp [:processor :host]) (get-in tp [:processor :port]) "public" pdu)]
              (is (= (OID. (deref received-trap-oid 2000 "TIMEOUT")) (OID. trap-oid-str)) "The received trap has this OID")
              (component/stop tp))))))))
