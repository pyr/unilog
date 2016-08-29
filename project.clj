(defproject spootnik/unilog "0.7.16-SNAPSHOT"
  :description "logging should be easy!"
  :url "https://github.com/pyr/unilog"
  :license {:name "MIT License"
            :url "https://github.com/pyr/unilog/tree/master/LICENSE"}
  :plugins [[codox "0.9.6"]]
  :codox {:defaults {:doc/format :markdown}}
  :dependencies [[org.clojure/clojure                           "1.8.0"]
                 [net.logstash.logback/logstash-logback-encoder "4.7"]
                 [org.slf4j/slf4j-api                           "1.7.21"]
                 [org.slf4j/log4j-over-slf4j                    "1.7.21"]
                 [org.slf4j/jul-to-slf4j                        "1.7.21"]
                 [ch.qos.logback/logback-classic                "1.1.7"]
                 [ch.qos.logback/logback-core                   "1.1.7"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.logging "0.3.1"]]}})
