(ns clojurewerkz.langohr.examples.direct-routing
  (:gen-class)
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.exchange  :as le]
            [langohr.queue     :as lq]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]))


(defn start-consumer
  "Starts a consumer in a separate thread"
  [ch queue-name]
  (let [handler (fn [ch metadata ^bytes payload]
                  (println (format "[consumer] %s received a message: %s"
                                   queue-name
                                   (String. payload "UTF-8"))))
        thread  (Thread. (fn []
                           (lc/subscribe ch queue-name handler :auto-ack true)))]
    (.start thread)))

(defn -main
  [& args]
  (let [conn  (rmq/connect)
        ch    (lch/open conn)
        ename "langohr.examples.direct"]
    (le/declare ch ename "direct")
    (let [q (.getQueue (lq/declare ch "" :exclusive false :auto-delete true))]
      (lq/bind ch q ename :routing-key "pings")
      (start-consumer ch q))
    (lb/publish ch ename "pings" "Ping" :content-type "text/plain")
    (Thread/sleep 2000)
    (println "[main] Disconnecting...")
    (rmq/close ch)
    (rmq/close conn)))
