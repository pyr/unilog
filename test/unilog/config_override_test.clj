(ns unilog.config-override-test
  (:require [unilog.config :refer [start-logging!]]
            [clojure.test :refer [deftest is testing]]
            [clojure.tools.logging :refer [debug info warn error]]
            [unilog.testns :as testns]
            [unilog.testns2 :as testns2]
            [nsexample :as nsexample]
            [clojure.string :as str])
  (:import [java.io File]
           [java.nio.file Files StandardOpenOption]))

(defn- temp-file
  "Temp file which gets deleted when the JVM stops"
  [prefix suffix]
  (doto (java.io.File/createTempFile prefix suffix)
    (.deleteOnExit)))

(defn- to-lines [path]
  (->> (str/split (slurp path) #"\n")
       (filter #(not (zero? (count %))))))

(defn truncate [^File f]
  (Files/write (.toPath f) (byte-array 0) (into-array StandardOpenOption [StandardOpenOption/TRUNCATE_EXISTING])))

(deftest logging
  (testing "Appender ref and additivity"
    (let [tempfile (temp-file "unilog.config" "log")]
      (start-logging! {:level     :info
                       :console   false
                       :appenders [{:appender     :file
                                    :file         (str tempfile)
                                    :encoder      :multipattern
                                    :multipattern {:unilog                      (str "ROOT" unilog.config/default-pattern)
                                                   :unilog.testns               (str "TESTNS " unilog.config/default-pattern)
                                                   :unilog.config-override-test "OVERRIDE_MARKER %c - %m%n%rEx{full,clojure}"}}]
                       :overrides {:unilog.config-override-test :error}})

      (testing "logging from nsexample uses default logger"
        (truncate tempfile)
        (nsexample/info "info")
        (is (not (str/includes? (slurp tempfile) "ROOT")))
        (is (not (str/includes? (slurp tempfile) "TESTNS")))
        (is (not (str/includes? (slurp tempfile) "OVERRIDE_MARKER"))))

      (testing "logging from a configured ns"
        (truncate tempfile)
        (testns/info "info")
        (is (not (str/includes? (slurp tempfile) "OVERRIDE_MARKER")))
        (is (str/includes? (slurp tempfile) "TESTNS")))

      (testing "loggers prefixes are matched at the dot, substrings dont count"
        ;; testns2 should log via root :unilog logger
        (truncate tempfile)
        (testns2/info "info")
        (is (not (str/includes? (slurp tempfile) "TESTNS")))
        (is (not (str/includes? (slurp tempfile) "OVERRIDE_MARKER")))
        (is (str/includes? (slurp tempfile) "ROOT")))

      (testing "overrides still work"
        (truncate tempfile)
        (info "info")
        (is (not (str/includes? (slurp tempfile) "OVERRIDE_MARKER"))))

      (testing "custom logger pattern"
        (truncate tempfile)
        (error "error")
        (is (str/includes? (slurp tempfile) "OVERRIDE_MARKER"))

        (testing "exception filtering"
          (truncate tempfile)
          (error (RuntimeException.) "ex")
          (let [lines (to-lines tempfile)]
            (is (every? #(not (str/includes? % "clojure.")) lines))

            (testing "some parts of the stack are still preserved"
              (is (some #(str/includes? % "unilog.config-override-test") lines)))))))))
