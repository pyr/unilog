(defproject spootnik/unilog "0.7.13"
  :description "logging should be easy!"
  :url "https://github.com/pyr/unilog"
  :license {:name "MIT License"
            :url "https://github.com/pyr/unilog/tree/master/LICENSE"}
  :plugins [[codox "0.8.10"]]
  :codox {:defaults {:doc/format :markdown}}
  :dependencies [[org.clojure/clojure                           "1.8.0"]
                 [net.logstash.logback/logstash-logback-encoder "4.2"]
                 [org.slf4j/slf4j-api                           "1.7.12"]
                 [org.slf4j/log4j-over-slf4j                    "1.7.12"]
                 [ch.qos.logback/logback-classic                "1.1.3"]
                 [ch.qos.logback/logback-core                   "1.1.3"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.logging "0.3.1"]]}})
