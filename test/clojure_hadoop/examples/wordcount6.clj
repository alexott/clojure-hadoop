;; wordcount6 -- example customized defjob
;;
;; This example wordcount program uses defjob like wordcount5, but it
;; includes an example of specifying additional parameters at the
;; commandline to set values in the JobConf Configuration
;; via -X<configuration-key> <configuration value>.
;;
;; It also includes an example of how to read the values during
;; the map and reduce setup phases and shows how they can be referenced
;; during the map and reduce execution phases. 
;;
;; 
;;
;; After compiling (see README.txt), run the example like this
;; (all on one line):
;;
;; java -cp examples.jar clojure_hadoop.job \
;;      -job clojure-hadoop.examples.wordcount6/job \
;;      -input README.txt -output out6 \
;;      -Xmy.custom.repeat.string hello \
;;      -Xmy.custom.multiplier 11 
;;        
;;
;; The output is plain text, written to out5/part-00000
;;
;; During the map phase we'll not only output a 1 for each
;; string we read in our input file, but also output
;; an additional 1 for whatever string we supply as
;; our my.custom.repeat.string parameter.
;;
;; During the reduce phase, we'll mulitply the count
;; of each word by the multiplier specified by
;; my.custom.multiplier value
;;
;;
;; Note: Parameters expressed at the commandline
;; will overwrite values expressed in the defjob
;; mappings expressed below.

(ns clojure-hadoop.examples.wordcount6
  (:require [clojure-hadoop.wrap :as wrap]
            [clojure-hadoop.defjob :as defjob]
            [clojure-hadoop.imports :as imp])
  (:import (java.util StringTokenizer))
  (:use clojure.test clojure-hadoop.job))

(imp/import-io)
(imp/import-mapreduce)


(def ^:dynamic *bonus-word*)

(defn my-map-setup [^JobContext context]
  (alter-var-root
   (var *bonus-word*)
   (fn [_]
     (.get (.getConfiguration context) "my.custom.repeat.string"))))

(defn my-map [key value]
  (mapcat
   (fn [token] [[token 1] [*bonus-word* 1]])
   (enumeration-seq (StringTokenizer. value))))

(def ^:dynamic *multiplyer*)

(defn my-reduce-setup [^JobContext context]
  (alter-var-root
   (var *multiplyer*)
   (fn [_]
     (read-string (.get (.getConfiguration context) "my.custom.multiplier")))))


(defn my-reduce [key values-fn]
  [[key (* *multiplyer* (reduce + (values-fn)))]])

(defn string-long-writer [^TaskInputOutputContext context ^String key value]
  (.write context (Text. key) (LongWritable. value)))

(defn string-long-reduce-reader [^Text key wvalues]
  [(.toString key)
   (fn [] (map (fn [^LongWritable v] (.get v)) wvalues))])

;; Specifying the same parameters to use in a unit-test context
;; when we're not passing in any commandline args.
(def my-config-params
  {"my.custom.repeat.string" "unit-test"
   "my.custom.multiplier" "7"})

(defjob/defjob job
  :map-setup my-map-setup
  :map my-map
  :map-reader wrap/int-string-map-reader
  :map-writer string-long-writer
  :reduce-setup my-reduce-setup
  :reduce my-reduce
  :reduce-reader string-long-reduce-reader
  :reduce-writer string-long-writer
  :output-key Text
  :output-value LongWritable
  :input-format :text
  :output-format :text
  :compress-output false
  :input "README.txt"
  :output "tmp/out6"
  :replace true
  ;; If we want to set parameters in the job configuration
  ;; as part of the program, we can do so with the
  ;; params field, then reference them above;
  ;; the same way as happens in the commandline case.
  ;;
  ;; This is a great way to set defaults if commandline params
  ;; are omitted, because these params will be processed first.
  ;; then the commandline args will be layered over.
  :params my-config-params)

(deftest test-wordcount-6
  (is (run job)))
