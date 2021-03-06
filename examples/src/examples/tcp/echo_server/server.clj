(ns examples.tcp.echo-server.server
  (:require
    [clojure.core.async :as async]
    [clojure.java.io :as io]
    [examples.common :as common])
  (:import (java.net ServerSocket)))

(defn echo-service
  [in out]
  (async/go-loop []
    (let [msg (async/<! in)]
      (async/>! out msg)
      (recur))))

(defn line-reader
  [sock]
  (let [in (async/chan)
        reader (io/reader sock)]
    (async/go-loop []
      (let [msg (.readLine reader)]
        (async/>! in msg))
      (recur))
    in))

(defn echo-writer
  [sock]
  (let [out (async/chan)
        writer (io/writer sock)]
    (async/go-loop []
      (let [msg (async/<! out)]
        (.write writer (format "Echoing: %s\n" msg)))
      (.flush writer)
      (recur))
    out))

(defn -main
  "You can start the server like this:
  ```
  $ lein run -m examples.tcp.echo-server.server
  ```

  To connect to the server:
  ```
  $ nc localhost 15099
  ```

  Then type away, and enjoy the echo chamber ;-)"
  [& [port & args]]
  (println "Starting server ...")
  (let [server (new ServerSocket (common/get-port port))
        sock (.accept server)]
    (println (format "Listening on tcp://%s:%s ..."
                     (.getHostAddress (.getLocalAddress sock))
                     (.getLocalPort sock)))
    (async/go
      (echo-service
        (line-reader sock)
        (echo-writer sock)))
    (.join (Thread/currentThread))))
