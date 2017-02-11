(ns unilog.config-test
  (:require [unilog.config         :refer :all]
            [clojure.test          :refer :all]
            [clojure.tools.logging :refer [debug info warn error]]))

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
    (is (nil? (error "error"))))  )
