(ns clojure-hadoop.gen
  "Class-generation helpers for writing Hadoop jobs in Clojure."
  (:use [clojure-hadoop.imports :only (import-conf)]))

(import-conf)

(defmacro gen-job-classes
  "Creates gen-class forms for Hadoop job classes from the current
  namespace. Now you only need to write three functions:

  (defn mapper-map [this key value mapper-context] ...)

  (defn reducer-reduce [this key values reducer-context] ...)

  (defn tool-run [& args] ...)

  The first two functions are the standard map/reduce functions in any
  Hadoop job.

  The third function, tool-run, will be called by the Hadoop framework
  to start your job, with the arguments from the command line.  It
  should set up the Job object and call JobClient/runJob, then
  return zero on success.

  You must also call gen-main-method to create the main method.

  After compiling your namespace, you can run it as a Hadoop job using
  the standard Hadoop command-line tools."
  []
  (let [the-name (.replace (str (ns-name *ns*)) \- \_)]
    `(do
       (gen-class
        :name ~the-name
        :extends "org.apache.hadoop.conf.Configured"
        :implements ["org.apache.hadoop.util.Tool"]
        :prefix "tool-"
        :main true)
       (gen-class
        :name ~(str the-name "_mapper")
        :extends "org.apache.hadoop.mapreduce.Mapper"
        :prefix "mapper-"
        :main false)
       (gen-class
        :name ~(str the-name "_reducer")
        :extends "org.apache.hadoop.mapreduce.Reducer"
        :prefix "reducer-"
        :main false)
       (gen-class
        :name ~(str the-name "_combiner")
        :extends "org.apache.hadoop.mapreduce.Reducer"
        :prefix "combiner-"
        :main false))))

(defn gen-main-method
  "Adds a standard main method, named tool-main, to the current
  namespace."
  []
  (let [the-name (.replace (str (ns-name *ns*)) \- \_)]
    (intern *ns* 'tool-main
            (fn [& args]
              (System/exit
               (org.apache.hadoop.util.ToolRunner/run
                (new org.apache.hadoop.conf.Configuration)
                (. (Class/forName the-name) newInstance)
                (into-array String args)))))))

(defmacro gen-conf-methods
  "Adds the tool-getConf and tool-setConf methods, to the current
  namespace."
  []
  `(do
     (defn ~'tool-getConf [~'_]
       (or ~'clojure-hadoop.job/*config* (Configuration.)))
     (defn ~'tool-setConf [~'_ ~'config]
       (clojure-hadoop.job/set-config ~'config))))

