(defproject clojure-hadoop "1.4.3-SNAPSHOT"
  :description "Library to aid writing Hadoop jobs in Clojure."
  :url "http://github.com/alexott/clojure-hadoop"
  :license {:name "Eclipse Public License 1.0"
            :url "http://opensource.org/licenses/eclipse-1.0.php"
            :distribution "repo"
            :comments "Same license as Clojure"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.apache.hadoop/hadoop-core "1.1.2"]
                 ;; commons-io was omitted in 1.0.3/1.0.4
                 ;; needs to be explicitly included
                 ;; until this is resolved.                 
                 ;; see http://goo.gl/Trx7A
                 [commons-io "2.4"] 
                 [log4j/log4j "1.2.16" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jdmk/jmxtools
                                                    com.sun.jmx/jmxri]]]
  :source-paths ["src" "test"]
  :dev-dependencies [[swank-clojure "1.4.2"]]
  :aot [clojure-hadoop.config
        clojure-hadoop.defjob
        clojure-hadoop.gen
        clojure-hadoop.imports
        clojure-hadoop.job
        clojure-hadoop.load
        clojure-hadoop.wrap
        clojure-hadoop.flow
        ;; TODO: Remove them? Only needed for the tests.
        clojure-hadoop.examples.wordcount1
        clojure-hadoop.examples.wordcount2])
