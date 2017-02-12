(ns unilog.config-test
  (:require [unilog.config         :refer :all]
            [clojure.test          :refer :all]
            [unilog.context        :refer [with-context]]
            [cheshire.core         :refer [parse-string]]
            [clojure.java.io       :refer [reader]]
            [clojure.tools.logging :refer [debug info warn error]]
            [clj-time.format       :refer [parse formatters]]
            [clj-time.core         :refer [now within? minus seconds]]))

(defn- temp-file
  "Temp file which gets deleted when the JVM stops"
  [prefix suffix]
  (doto (java.io.File/createTempFile prefix suffix)
    (.deleteOnExit)))

(defn- parse-lines
  "Returns a seq of JSON records parsed from a file, line by line."
  [path]
  (for [line (line-seq (reader path))]
    (parse-string line true)))

(defn- check-interval
  [date-str]
  (let [dt (parse date-str)]
    (within? (minus (now) (seconds 5)) dt (now))))

(def ^:private get-version (keyword "@version"))
(def ^:private get-timestamp (keyword "@timestamp"))

(deftest logging

  (testing "Text logging to console"
    (start-logging! {:level :info})
    (is (nil? (debug "debug")))
    (is (nil? (info "info")))
    (is (nil? (warn "warn")))
    (is (nil? (error "error"))))

  (testing "JSON logging to console"
    (start-logging! {:level :info :appenders [{:appender :console :encoder :json}]})
    (is (nil? (debug "debug")))
    (is (nil? (info "info")))
    (is (nil? (warn "warn")))
    (is (nil? (error "error"))))

  (testing "JSON logging to file"
    (let [path (temp-file "unilog.config" "log")]
      (start-logging! {:level :info :appenders [{:appender :file
                                                 :file (str path)
                                                 :encoder :json}]})
      (debug "debug")
      (info "info")
      (warn "warn")
      (error "error")

      (let [records (parse-lines path)]
        (every? true? (map (comp check-interval get-timestamp) records))
        (is (= #{"unilog.config-test"} (reduce conj #{} (map :logger_name records))))
        (is (= #{"main"} (reduce conj #{} (map :thread_name records))))
        (is (= #{1} (reduce conj #{} (map get-version records))))
        (is (= ["info" "warn" "error"] (map :message records)))
        (is (= ["INFO" "WARN" "ERROR"] (map :level records)))
        )))

  (testing "JSON logging to file with MDC"
    (let [path (temp-file "unilog.config" "log")]
      (start-logging! {:level :info :appenders [{:appender :file
                                                 :file (str path)
                                                 :encoder :json}]})
      (with-context {:foo :bar}

        (debug "debug")
        (info "info")
        (warn "warn")
        (error "error")

        (let [records (parse-lines path)]
          (every? true? (map (comp check-interval get-timestamp) records))
          (is (= #{"unilog.config-test"} (reduce conj #{} (map :logger_name records))))
          (is (= #{"main"} (reduce conj #{} (map :thread_name records))))
          (is (= #{1} (reduce conj #{} (map get-version records))))
          (is (= ["info" "warn" "error"] (map :message records)))
          (is (= ["INFO" "WARN" "ERROR"] (map :level records)))
          (is (= #{":bar"} (reduce conj #{} (map :foo records)))))))))
