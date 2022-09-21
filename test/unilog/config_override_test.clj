(ns unilog.config-override-test
  (:require [unilog.config :refer [start-logging!]]
            [clojure.test :refer [deftest is testing]]
            [clojure.tools.logging :refer [debug info warn error]]
            [unilog.testns :as testns]
            [clojure.string :as str]))

(defn- temp-file
  "Temp file which gets deleted when the JVM stops"
  [prefix suffix]
  (doto (java.io.File/createTempFile prefix suffix)
    (.deleteOnExit)))

(defn- to-lines [path]
  (->> (str/split (slurp path) #"\n")
       (filter #(not (zero? (count %))))))

(deftest logging
  (testing "Appender ref and additivity"
    (let [path (temp-file "unilog.config" "log")]
      (start-logging! {:level     :info
                       :console   false
                       :appenders [{:appender     :file
                                    :file         (str path)
                                    :encoder      :multipattern
                                    :multipattern {:unilog (str "BASE_OVERRIDE " unilog.config/default-pattern)
                                                   :unilog.config-override-test "OVERRIDE_MARKER %c - %m%n%rEx{full,clojure}"}}]
                       :overrides {:unilog.config-override-test :error}})

      (testing "logging from other ns"
        (testns/info "info")
        (is (not (str/includes? (slurp path ) "OVERRIDE_MARKER")))
        (is (str/includes? (slurp path) "BASE_OVERRIDE")))

      (testing "overrides still work"
        (info "info")
        (is (not (str/includes? (slurp path ) "OVERRIDE_MARKER"))))

      (testing "custom logger pattern"
        (error "error")
        (is (str/includes? (slurp path) "OVERRIDE_MARKER"))

        (testing "exception filtering"
          (error (RuntimeException.) "ex")
          (let [lines (to-lines path)]
            (is (every? #(not (str/includes? % "clojure.")) lines))

            (testing "some parts of the stack are still preserved"
              (is (some #(str/includes? % "unilog.config-override-test") lines)))))))))
