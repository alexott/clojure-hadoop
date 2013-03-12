;; wordcount7 -- example using the distributed cache to cache
;;              files and archives
;;
;; This example wordcount program uses defjob like wordcount5, but it
;; includes demonstrating configuring and using the DistributedCache
;; to add a file of words to ignore in the map phase.
;;
;; In the defjob section below we'll configure two files that
;; will have a few words each to create a predicate during the
;; map setup phase, then use the predicate in the my-map
;; to filter counting specific words.
;;
;; The commandline example below shows how we can specify files
;; to load in the distributed cache via the commandline as well. 
;;
;; After compiling (see README.txt), run the example like this
;; (all on one line):
;;
;; java -cp examples.jar clojure_hadoop.job \
;;      -job clojure-hadoop.examples.wordcount7/job \
;;      -input README.txt -output out7 \
;;      -add-cache-file <ABSOLUTE-PATH>/test-resources/wc7-exclude-these-words.txt \
;;      -add-cache-file <ABSOLUTE-PATH>/test-resources/wc7-exclude-these-words-too.txt 
;;
;; The output is plain text, written to out7/part-00000
;;
;; All file specificaitons for the DistributedCache must be strings
;; that can function as java.net.URI's as that is what is required
;; to add a file to the DistributedCache. If the files are in HDFS/S3
;; the URI can be interpreted as a directory locaiton within HDFS/S3.
;; for example:
;;
;; hadoop jar examples.jar clojure_hadoop.job \
;;   -job clojure-hadoop.examples.wordcount7/job \
;;   -input hdfs://<HDFS-ADDRESS>/hdfs/fs/path/to/README.txt -output out7 \
;;   -add-cache-file hdfs://<HDFS-ADDRESS>/hdfs/fs/path/to/wc7-exclude-these-words.txt \
;;   -add-cache-file hdfs://<HDFS-ADDRESS>/hdfs/fs/path/to/wc7-exclude-these-words-too.txt
;;  
;; For S3 replace hdfs://<HDFS-ADDRESS> with s3://<S3-ADDRESS>
;;
;; If running hadoop local to where HDFS is located, you can omit
;; hdfs://<HDFS-ADDRESS>
;;
;; In the later section below, we have an example
;; of how to specify files to place in the distributed
;; cache below.

(ns clojure-hadoop.examples.wordcount7
  (:require [clojure-hadoop.wrap :as wrap]
            [clojure-hadoop.defjob :as defjob]
            [clojure-hadoop.imports :as imp])
  (:import (java.util StringTokenizer)
           (java.net URI))
  (:require [clojure.string :as string])
  (:use clojure.test clojure-hadoop.job)
  (:use [clojure.string :only [join]]))

(imp/import-io)
(imp/import-mapreduce)
(imp/import-filecache)


;; We're going to make a custom ignore-word? predicate
;; for each mapper. Read the following function descriptions
;; from `my-map-setup` upward.
(def ignore-word?)

(defn distributed-cache-files
  "Given the job configuration retrieve the list of file paths
   we've stored in the distributed cache. These will be different
   paths on each node, thus each mapper needs to ask for the
   location during the set up phase."
  [job-conf]  
  (DistributedCache/getLocalCacheFiles job-conf))


(defn file-path->lines
  "Given a Path, return the lines of the file as a realized
   sequence"
  [path]
  ;; we have to .toString the path to get the absolute path
  (let [file-name (str path)] 
    (with-open [rdr (clojure.java.io/reader file-name)]
      ;; force realization of the sequence so we can close the file
      (doall (line-seq rdr)))))

(defn make-ignore-word?
  "Given the job configuration, read out the list of files that 
   we've supplied to the distributed cache, and create a predicate  
   that will return false for any strings that are
   contained in our files."
  [job-conf]
  
  (let [local-file-paths (distributed-cache-files job-conf)
        string-set (set (mapcat file-path->lines local-file-paths))]
    ;; remember, sets in clojure can be used as predicates to test membership
    (fn [string] (string-set string))))

(defn my-map-setup
  "When we set up the mapper, let's now bind our ignore-word? var
   to a function that's read the files that contains the words to ignore
   from our cache."
  [^JobContext context]  
  (alter-var-root (var ignore-word?)
                  (fn [_]
                    (make-ignore-word? (.getConfiguration context)))))

(defn my-map
  "We'll emit a 1 for each word that we don't end up ignoring,
   along with that word."
  [key value]
  (map (fn [token] [token 1])
       (remove ignore-word? (enumeration-seq (StringTokenizer. value)))))

(defn my-reduce
  "Gather by each word and sum the values"
  [key values-fn]  
  [[key (reduce + (values-fn))]])

(defn string-long-writer [^TaskInputOutputContext context ^String key value]
  (.write context (Text. key) (LongWritable. value)))

(defn string-long-reduce-reader [^Text key wvalues]
  [(.toString key)
   (fn [] (map (fn [^LongWritable v] (.get v)) wvalues))])

(defjob/defjob job
  :map-setup my-map-setup
  :map my-map
  :map-reader wrap/int-string-map-reader
  :map-writer string-long-writer
  :reduce my-reduce
  :reduce-reader string-long-reduce-reader
  :reduce-writer string-long-writer
  :output-key Text
  :output-value LongWritable
  :input-format :text
  :output-format :text
  :compress-output false
  :input "README.txt"
  :output "tmp/out7"
  :replace true)

(deftest test-wordcount-7
  (is (run job)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Specifying files for the distibuted cache within your
;;; Map-Reduce job.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; NOTE: get-current-directory does not work
;; when you're running a job like this on hadoop
;; to reference files. This is purely for the
;; testing, and as an example.
(defn- get-current-directory []
  (. (java.io.File. ".") getCanonicalPath))

;; If you want to specify files within your code
;; that reside in HDFS or S3, you need to specify
;; the aboslute path to the files here,
;; as a vector. Again, get-current-directory
;; is purely for regression testing.
;;
;; In a "typical" case where we want to specify the path within
;; the code, this would look something like:
;;
;; (def file-for-distributed-cache "/absolute/path/to/my/file")
;;
;; -or-
;;
;; (def files-for-distributed-cache ["/absolute/path/to/file1"
;;                                   "/absolute/path/to/file2"
;;                                    ...])
(def files-for-distributed-cache
  (let [cwd (get-current-directory)]
    [(join  "/" [cwd "test-resources/wc7-exclude-these-words.txt"])
     (join  "/" [cwd "test-resources/wc7-exclude-these-words-too.txt"])]))
  
(defjob/defjob configured-job
  :map-setup my-map-setup
  :map my-map
  :map-reader wrap/int-string-map-reader
  :map-writer string-long-writer
  :reduce my-reduce
  :reduce-reader string-long-reduce-reader
  :reduce-writer string-long-writer
  :output-key Text
  :output-value LongWritable
  :input-format :text
  :output-format :text
  :compress-output false
  :input "README.txt"
  :output "tmp/out7"
  :replace true
  :add-cache-file files-for-distributed-cache)
  
(deftest test-wordcount-7-via-config
  "Testing adding files to the DistributedCache via
in-file configuration"
  (is (run configured-job)))
