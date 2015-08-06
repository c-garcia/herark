(ns herark.main
  (:require [herark.core :refer [snmp-v2c-trap-processor]]
            [herark.smi :refer :all]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [taoensso.timbre :as log])
  (:import [org.apache.commons.daemon Daemon DaemonContext]
           (org.snmp4j CommandResponderEvent))
  (:gen-class
    :init init
    :implements [Daemon]))

(def state (atom nil))

(defn process-event
  [^CommandResponderEvent ev]
  (try
    (let [pdu (.getPDU ev)
          vbs (.getVariableBindings pdu)]
      (doseq [vb vbs]
        (println (str (as-tagged (.getOid vb))) "=" (str (as-tagged (.getVariable vb))))))
    (catch Exception e
      (log/error "Exception caught" e))))

(defn init [args]
  (let [proc-name (env :name "v2c-proc")
        host (env :host "localhost")
        port-s (env :port "11162")
        port (Integer/parseInt port-s)
        community (env :community "public")]
    (log/info "Starting SNMP trap processor:" proc-name)
    (log/debug "host:" host)
    (log/debug "port:" port)
    (log/debug "community:" community)
    (->> (component/system-map
           :responder process-event
           :app (component/using
                  (snmp-v2c-trap-processor proc-name host port :community community)
                  [:responder]))
         (reset! state))
    (log/debug "system state created")))

(defn start []
  (log/debug "starting processor")
  (swap! state component/start)
  (log/debug "started processor"))

(defn stop []
  (log/debug "stopping processor")
  (swap! state component/stop)
  (log/debug "stopped processor"))

(defn -init [this ^DaemonContext context]
  (init (.getArguments context)))

(defn -start [this]
  (future start))

(defn -stop [this]
  (stop))

(defn -main [& args]
  (init args)
  (start))
