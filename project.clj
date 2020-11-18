(defproject spootnik/unilog "0.7.27"
  :description "logging should be easy!"
  :url "https://github.com/pyr/unilog"
  :license {:name "MIT License"
            :url  "https://github.com/pyr/unilog/tree/master/LICENSE"}
  :plugins [[lein-ancient "0.6.15"]]
  :pedantic? :abort
  :dependencies [[org.clojure/clojure                           "1.10.1"]
                 [net.logstash.logback/logstash-logback-encoder "6.4"]
                 [org.slf4j/slf4j-api                           "1.7.30"]
                 [org.slf4j/log4j-over-slf4j                    "1.7.30"]
                 [org.slf4j/jul-to-slf4j                        "1.7.30"]
                 [ch.qos.logback/logback-classic                "1.2.3"]
                 [ch.qos.logback/logback-core                   "1.2.3"]]
  :deploy-repositories [["releases" :clojars] ["snapshots" :clojars]]
  :profiles {:dev  {:dependencies [[org.clojure/tools.logging "1.1.0"]
                                   [metosin/jsonista          "0.2.7"
                                    :exclusions [com.fasterxml.jackson.core/*]]]
                    :pedantic?    :ignore
                    :plugins      [[lein-ancient "0.6.15"]]
                    :global-vars  {*warn-on-reflection* true}}
             :test {:dependencies [[org.clojure/tools.logging "1.1.0"]
                                   [metosin/jsonista          "0.2.7"
                                    :exclusions [com.fasterxml.jackson.core/*]]]
                    :plugins      [[lein-difftest "2.0.0"]
                                   [lein-cljfmt "0.6.7"]]
                    :pedantic?    :abort}})
