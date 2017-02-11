(ns unilog.config-test
  (:require [unilog.config         :refer :all]
            [clojure.test          :refer :all]
            [clojure.tools.logging :refer [debug info warn error]]))

(deftest logging

  (testing "Text logging to console"
    (start-logging! {:level :info})
    (is (nil? (debug "default config")))
    (is (nil? (info "default config")))
    (is (nil? (warn "default config")))
    (is (nil? (error "default config"))))

  (testing "Text logging to console, through `appenders` key"
    (start-logging! {:level     :info
                     :console   false
                     :appenders [{:appender :console}]})
    (is (nil? (debug "text config")))
    (is (nil? (info "text config")))
    (is (nil? (warn "text config")))
    (is (nil? (error "text config"))))

  (testing "JSON logging to console"
    (start-logging! {:level     :info
                     :console   false
                     :appenders [{:appender :console
                                  :encoder  :json}]})
    (is (nil? (debug "JSON config")))
    (is (nil? (info "JSON config")))
    (is (nil? (warn "JSON config")))
    (is (nil? (error "JSON config"))))  )
