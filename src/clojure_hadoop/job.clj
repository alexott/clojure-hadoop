(ns clojure-hadoop.job
  (:require [clojure-hadoop.gen :as gen]
            [clojure-hadoop.imports :as imp]
            [clojure-hadoop.wrap :as wrap]
            [clojure-hadoop.config :as config]
            [clojure-hadoop.load :as load])
  (:import (org.apache.hadoop.util Tool))
  (:use [clojure.contrib.def :only (defvar-)]
        [clojure-hadoop.config :only (configuration)]
        [clojure-hadoop.context :only (with-context)]))

(imp/import-conf)
(imp/import-io)
(imp/import-io-compress)
(imp/import-fs)
(imp/import-mapreduce)
(imp/import-mapreduce-lib)

(def ^Configuration *config* nil)

(gen/gen-job-classes)

(defvar- method-fn-name
  {"map" "mapper-map"
   "reduce" "reducer-reduce"
   "combine" "combiner-reduce"})

(defvar- wrapper-fn
  {"map" wrap/wrap-map
   "reduce" wrap/wrap-reduce
   "combine" wrap/wrap-reduce})

(defvar- default-reader
  {"map" wrap/clojure-map-reader
   "reduce" wrap/clojure-reduce-reader
   "combine" wrap/clojure-reduce-reader})

(defn set-config [config]
  (alter-var-root (var *config*) (fn [_] config)))

(defn- configure-functions
  "Preps the mapper or reducer with a Clojure function read from the
  job configuration.  Called from Mapper.configure and
  Reducer.configure."
  [type ^Configuration configuration]
  (set-config configuration)
  (let [function (load/load-name (.get configuration (str "clojure-hadoop.job." type)))
        reader (if-let [v (.get configuration (str "clojure-hadoop.job." type ".reader"))]
                 (load/load-name v)
                 (default-reader type))
        writer (if-let [v (.get configuration (str "clojure-hadoop.job." type ".writer"))]
                 (load/load-name v)
                 wrap/clojure-writer)]
    (assert (fn? function))
    (alter-var-root (ns-resolve (the-ns 'clojure-hadoop.job)
                                (symbol (method-fn-name type)))
                    (fn [_] ((wrapper-fn type) function reader writer)))))

;;; CREATING AND CONFIGURING JOBS

(defn- parse-command-line [job args]
  (try
    (config/parse-command-line-args job args)
    (catch Exception e
      (prn e)
      (config/print-usage)
      (System/exit 1))))

;;; MAPPER METHODS

(defn mapper-cleanup [this context]
  (with-context context
    (let [configuration (.getConfiguration context)]
      (if-let [cleanup-fn-name (.get configuration config/map-cleanup)]
        ((load/load-name cleanup-fn-name) context)))))

(defn mapper-map [this wkey wvalue context]
  (throw (Exception. "Mapper function not defined.")))

(defn mapper-setup [this context]
  (with-context context
    (let [configuration (.getConfiguration context)]
      (configure-functions "map" configuration)
      (if-let [setup-fn-name (.get configuration config/map-setup)]
        ((load/load-name setup-fn-name) context)))))

;;; REDUCER METHODS

(defn reducer-cleanup [this context]
  (with-context context
    (let [configuration (.getConfiguration context)]
      (if-let [cleanup-fn-name (.get configuration config/reduce-cleanup)]
        ((load/load-name cleanup-fn-name) context)))))

(defn reducer-reduce [this wkey wvalues context]
  (throw (Exception. "Reducer function not defined.")))

(defn reducer-setup [this context]
  (with-context context
    (let [configuration (.getConfiguration context)]
      (configure-functions "reduce" configuration)
      (if-let [setup-fn-name (.get configuration config/reduce-setup)]
        ((load/load-name setup-fn-name) context)))))

;;; COMBINER METHODS

(defn combiner-cleanup [this context]
  (with-context context
    (let [configuration (.getConfiguration context)]
      (if-let [cleanup-fn-name (.get configuration config/combine-cleanup)]
        ((load/load-name cleanup-fn-name) context)))))

(defn combiner-reduce [this wkey wvalues context]
  (throw (Exception. "Combiner function not defined.")))

(defn combiner-setup [this context]
  (with-context context
    (let [configuration (.getConfiguration context)]
      (configure-functions "combine" configuration)
      (if-let [setup-fn-name (.get configuration config/combine-setup)]
        ((load/load-name setup-fn-name) context)))))

(defn- handle-replace-option [^Job job]
  (when (= "true" (.get (configuration job) "clojure-hadoop.job.replace"))
    (let [output (FileOutputFormat/getOutputPath job)
          fs (FileSystem/get (.toUri output) (configuration job))]
      (.delete fs output true))))

(defn- set-default-config [^Job job]
  (doto job
    (.setJobName "clojure_hadoop.job")
    (.setOutputKeyClass Text)
    (.setOutputValueClass Text)
    (.setMapperClass (Class/forName "clojure_hadoop.job_mapper"))
    (.setReducerClass (Class/forName "clojure_hadoop.job_reducer"))
    (.setInputFormatClass SequenceFileInputFormat)
    (.setOutputFormatClass SequenceFileOutputFormat)
    (FileOutputFormat/setCompressOutput true)
    (SequenceFileOutputFormat/setOutputCompressionType
     SequenceFile$CompressionType/BLOCK)))

(defn- run-hadoop-job
  "Run a hadoop job and wait for completion.
   Params are a hadoop Tool instance and a configuration function that should accept a single hadoop Job parameter.
   The config function will be called with the current job once the default params have been set."
  [tool job-config-fn]
  (let [config (.getConf tool)]
    (doto (Job. config)
      (.setJarByClass (.getClass tool))
      (set-default-config)
      (job-config-fn)
      (handle-replace-option)
      (.waitForCompletion true))))

(defn run
  "Runs a Hadoop job given the job configuration map/fn."
  ([job]
     (run (clojure_hadoop.job.) job))  		    
  ([tool job]
     (run-hadoop-job tool #(config/conf % :job job))))

;;; TOOL METHODS

(gen/gen-conf-methods)
(gen/gen-main-method)

(defn tool-run [^Tool this args]
  (run-hadoop-job this #(parse-command-line % args))
  0)
