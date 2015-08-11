(ns herark.adapters.snmp4j-test
  (:require [clojure.test :refer :all]
            [herark.adapters.snmp4j :refer :all]
            [taoensso.timbre :as log]
            [com.stuartsierra.component :as component]
            [herark.tools :refer :all]
            [herark.test-tools :refer :all])
  (:import (org.snmp4j.smi OID)
           (java.util.concurrent Executors)))

(defn make-testing-processor
  "Creates a trap processor listenting at a random port with the specified
  responder `f`."
  [f]
  (try-on-random-port
    (fn [p] (-> (component/system-map
                  :responder f
                  :processor (component/using
                               (snmp-v2c-trap-processor
                                 (str "Proc-at-" p)
                                 "localhost"
                                 p)
                               [:responder]))
                component/start))))

(deftest snmp-v2c-trap-received
  (testing "Given a function to be invoked when a trap is received"
    (let [flag (promise)
          process-trap (fn [e] (log/debug "I got hit!" e) (deliver flag true))]
      (testing "and a v2c trap processor with this function as responder"
        (let [tp (make-testing-processor process-trap)]
          (log/debug "Created testing processor: " (get-in tp [:processor]))
          (testing "When I send a v2c trap to this processor"
            (let [pdu (make-notification-pdu ".1.2.3.4")
                  _ (send-pdu (get-in tp [:processor :host]) (get-in tp [:processor :port]) "public" pdu)
                  invoked? (deref flag 2000 false)]
              (is invoked? "the function is invoked")))
          (component/stop tp))))))

(deftest snmp-v2c-trap-equals-sent
  (let [trap-oid-str ".1.3.6.1.6.3.1.1.5.3"]
    (testing (str "Given a trap OID:" trap-oid-str)
      (testing "And a v2c trap processor"
        (let [received-trap-oid (promise)
              f (fn [e]
                  (log/debug "I got hit!")
                  (try
                    (let [oid (get-in e [:varbinds 1 1 1])]
                      (log/debug "Received OID:" (str (OID. (int-array oid))))
                      (deliver received-trap-oid oid))
                    (catch Exception ex
                      (log/error "Exception when accessing event:" ex)
                      (deliver received-trap-oid []))))
              tp (make-testing-processor f)]
          (testing "when I send a notification with that OID"
            (let [pdu (make-notification-pdu trap-oid-str)
                  _ (send-pdu (get-in tp [:processor :host]) (get-in tp [:processor :port]) "public" pdu)]
              (is (= (OID. (int-array (deref received-trap-oid 2000 []))) (OID. trap-oid-str)) "The received trap has this OID"))
            (component/stop tp)))))))

(deftest snmp-v2c-trap-race-condition-prevention
  (let [trap-oid-str ".1.3.6.1.6.3.1.1.5.3"]
    (testing (str "Given a trap OID:" trap-oid-str)
      (testing "and a trap handler that increases the value of an atom and a v2c trap processor"
        (let [counter (atom 0)
              handler (fn [_] (swap! counter inc))
              tp (make-testing-processor handler)]
          (testing "and a ThreadPool of the same number of processors than cores sending a trap 10 times each"
            (let [avail-proc (. (Runtime/getRuntime) availableProcessors)
                  update-times 100
                  thread-pool (Executors/newFixedThreadPool avail-proc)
                  send-notif (fn [] (doseq [_ (range update-times)]
                                      (let [pdu (make-notification-pdu ".1.3.6.1.6.3.1.1.5.3")
                                            host (get-in tp [:processor :host])
                                            port (get-in tp [:processor :port])]
                                        (try
                                          (send-pdu host port "public" pdu)
                                          (catch Exception e
                                            (log/error "Exception catched while sending PDU" e))))))]
              (testing "When I execute all the threads and wait for the result"
                (doseq [th-status (.invokeAll
                                    thread-pool
                                    (repeat avail-proc send-notif))]
                  (log/debug "Joining thread: " (.get th-status)))
                ; Without the log statement above, the test fails frequently with update-times < desired one.
                ; Is it because the compiler removes-optimizes the call to .get so the thread-pool is shutdown
                ; before all threads are done?
                (.shutdown thread-pool)
                (is (= @counter (* update-times avail-proc)) "The counter value equals times * threads")))))))))
