(ns unilog.testns2
  "Test ns to check different loggers"
  (:require [clojure.tools.logging :as log]))

(defn info
  ([msg] (log/info msg))
  ([e msg] (log/info e msg)))
(defn warn
  ([msg] (log/warn msg))
  ([e msg] (log/warn e msg)))
(defn error
  ([msg] (log/error msg))
  ([e msg] (log/error e msg)))
