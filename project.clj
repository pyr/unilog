(defproject org.spootnik/logconfig "0.7.3"
  :description "easy logging setup"
  :url "https://github.com/pyr/logconfig"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[codox "0.8.10"]]
  :codox {:defaults {:doc/format :markdown}}
  :dependencies [[org.clojure/clojure                 "1.6.0"]
                 [commons-logging/commons-logging     "1.2"]
                 [net.logstash.log4j/jsonevent-layout "1.7"]
                 [org.slf4j/slf4j-log4j12             "1.7.7"]
                 [log4j/apache-log4j-extras           "1.2.17"]
                 [log4j/log4j                         "1.2.17"
                  :exclusions [javax.mail/mail
                               javax.jms/jms
                               com.sun.jdmk/jmxtools
                               com.sun.jmx/jmxri]]])
