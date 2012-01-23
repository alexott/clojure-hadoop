(ns clojure-hadoop.context
  (:use [clojure-hadoop.imports :only (import-mapreduce)]))

(import-mapreduce)

(def ^TaskInputOutputContext ^:dynamic *context* nil)

(defn get-counter
  "Returns a counter by group and name."
  [group-name counter-name]
  (if *context* (.getCounter *context* group-name counter-name)))

(defn increment-counter
  "Increment the counter by the given amount or 1."
  [group-name counter-name & [amount]]
  (if *context* (.increment (get-counter group-name counter-name) (or amount 1))))

(defn set-status
  "Set the current status of the task to the given message."
  [message] (if *context* (.setStatus *context* message)))

(defmacro with-context [context & body]
  `(binding [clojure-hadoop.context/*context* ~context]
     ~@body))
