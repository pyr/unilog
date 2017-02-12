(ns unilog.config-test
  (:require [unilog.config         :refer :all]
            [clojure.test          :refer :all]
            [unilog.context        :refer [with-context]]
            [cheshire.core         :refer [parse-string]]
            [clojure.java.io       :refer [reader]]
            [clojure.tools.logging :refer [debug info warn error]]))

(defn temp-file
  [prefix suffix]
  (doto (java.io.File/createTempFile prefix suffix)
    (.deleteOnExit)))

(defn parse-lines
  [path]
  (for [line (line-seq (reader path))]
    (parse-string line true)))

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
        (is (= #{"unilog.config-test"} (reduce conj #{} (map :logger_name records))))
        (is (= #{"main"} (reduce conj #{} (map :thread_name records))))
        (is (= #{1} (reduce conj #{} (map (keyword "@version") records))))
        (is (= ["info" "warn" "error"] (map :message records)))
        (is (= ["INFO" "WARN" "ERROR"] (map :level records))))))

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
          (is (= #{"unilog.config-test"} (reduce conj #{} (map :logger_name records))))
          (is (= #{"main"} (reduce conj #{} (map :thread_name records))))
          (is (= #{1} (reduce conj #{} (map (keyword "@version") records))))
          (is (= ["info" "warn" "error"] (map :message records)))
          (is (= ["INFO" "WARN" "ERROR"] (map :level records)))
          (is (= #{":bar"} (reduce conj #{} (map :foo records)))))))))
