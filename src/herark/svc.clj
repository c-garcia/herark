(ns herark.svc
  (:require [herark.adapters.snmp4j :refer [snmp-v2c-trap-processor]]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [taoensso.timbre :as log])
  (:import [org.apache.commons.daemon Daemon DaemonContext]
           (org.snmp4j CommandResponderEvent))
  (:gen-class
    :implements [org.apache.commons.daemon.Daemon]))

(def state (atom nil))

(defn process-event!
  [ev]
  (log/debug "Received event" ev))

(defn init [args]
  (let [proc-name (env :name "v2c-proc")
        host (env :host "localhost")
        port-s (env :port "11162")
        port (Integer/parseInt port-s)]
    (log/info "Starting SNMP trap processor:" proc-name)
    (log/debug "host:" host)
    (log/debug "port:" port)
    (->> (component/system-map
           :responder process-event!
           :app (component/using
                  (snmp-v2c-trap-processor proc-name host port)
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
