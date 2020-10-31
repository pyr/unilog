(ns unilog.config-test
  (:require [unilog.config         :refer [start-logging!]]
            [clojure.test          :refer [deftest is testing]]
            [clojure.instant       :as instant]
            [unilog.context        :refer [with-context]]
            [jsonista.core         :as j]
            [clojure.java.io       :as io]
            [clojure.tools.logging :refer [debug info warn error]]))

(defn- temp-file
  "Temp file which gets deleted when the JVM stops"
  [prefix suffix]
  (doto (java.io.File/createTempFile prefix suffix)
    (.deleteOnExit)))

(defn- parse-lines
  "Returns a seq of JSON records parsed from a file, line by line."
  [path]
  (for [line (line-seq (io/reader path))]
    (j/read-value line j/keyword-keys-object-mapper)))

(defn- minus-seconds
  [^java.util.Date d ^Long seconds]
  (java.util.Date/from
   ^java.time.Instant
   (-> d .toInstant (.minusSeconds ^Long seconds))))

(defn- check-interval
  [date-str]
  (let [dt ^java.util.Date (instant/read-instant-date date-str)
        ceiling ^java.util.Date (java.util.Date.)
        floor   ^java.util.Date (minus-seconds ceiling 5)]
    (and (.after dt floor) (.before dt ceiling))))

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
    (start-logging! {:level     :info
                     :appenders [{:appender :console :encoder :json}]})
    (is (nil? (debug "debug")))
    (is (nil? (info "info")))
    (is (nil? (warn "warn")))
    (is (nil? (error "error"))))

  (testing "JSON logging to file"
    (let [path (temp-file "unilog.config" "log")]
      (start-logging! {:level :info :appenders [{:appender :file
                                                 :file     (str path)
                                                 :encoder  :json}]})
      (debug "debug")
      (info "info")
      (warn "warn")
      (error "error")

      (let [records (parse-lines path)]
        (is (every? true? (map (comp check-interval get-timestamp) records)))
        (is (= #{"unilog.config-test"} (reduce conj #{} (map :logger_name records))))
        (is (= #{"main"} (reduce conj #{} (map :thread_name records))))
        (is (= #{"1"} (reduce conj #{} (map get-version records))))
        (is (= ["info" "warn" "error"] (map :message records)))
        (is (= ["INFO" "WARN" "ERROR"] (map :level records)))
        )))

  (testing "JSON logging to file with MDC"
    (let [path (temp-file "unilog.config" "log")]
      (start-logging! {:level :info :appenders [{:appender :file
                                                 :file     (str path)
                                                 :encoder  :json}]})
      (with-context {:foo :bar}

        (debug "debug")
        (info "info")
        (warn "warn")
        (error "error")

        (let [records (parse-lines path)]
          (is (every? true? (map (comp check-interval get-timestamp) records)))
          (is (= #{"unilog.config-test"} (reduce conj #{} (map :logger_name records))))
          (is (= #{"main"} (reduce conj #{} (map :thread_name records))))
          (is (= #{"1"} (reduce conj #{} (map get-version records))))
          (is (= ["info" "warn" "error"] (map :message records)))
          (is (= ["INFO" "WARN" "ERROR"] (map :level records)))
          (is (= #{":bar"} (reduce conj #{} (map :foo records)))))))))
