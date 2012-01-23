(ns clojure-hadoop.filesystem
  (:import [java.io BufferedReader BufferedWriter InputStreamReader OutputStreamWriter])
  (:use [clojure-hadoop.imports]))

(import-conf)
(import-fs)
(import-io-compress)

(def ^:dynamic *buffer-size*
  "The default buffer size."
  1024)

(def ^:dynamic *file-system*
  "The filesystem."
  (FileSystem/get (Configuration.)))

(defn make-path [path]
  (if (isa? (class path) Path)
    path (Path. (str path))))

(defn filesystem [path & [configuration]]
  (FileSystem/get (.toUri (make-path path)) (or configuration (Configuration.))))

(defn compression-codec [path]
  (.getCodec (CompressionCodecFactory. (Configuration.)) (make-path path)))

(defn input-stream
  "Open path and return a FSDataInputStream."
  [path]
  (let [input-stream (.open (filesystem path) (make-path path))]
    (if-let [codec (compression-codec path)]
      (.createInputStream codec input-stream)
      input-stream)))

(defn output-stream
  "Open path and return a FSDataOutputStream."
  [path]
  (let [output-stream (.create (filesystem path) (make-path path))]
    (if-let [codec (compression-codec path)]
      (.createOutputStream codec output-stream)
      output-stream)))

(defn buffered-reader [path]
  (BufferedReader. (InputStreamReader. (input-stream path))))

(defn buffered-writer [path]
  (BufferedWriter. (OutputStreamWriter. (output-stream path))))

(defn default-filesystem []
  (filesystem (. (Configuration.) get "fs.default.name")))

(defn delete [path]
  (.delete (filesystem path) (make-path path)))

(defn exists?
  "Returns true if the path exists, otherwise false."
  [path] (.exists (filesystem path) (make-path path)))

(defn make-directory
  "Make the given path and all non-existent parents into directories."
  [path] (.mkdirs (filesystem path) (make-path path)))

(defn local-tmp-dir
  "Returns the path of the system's temporary directory."
  [] (make-path (str "file:" (System/getProperty "java.io.tmpdir"))))

(defn hadoop-tmp-dir
  "Returns the path of the system's temporary directory."
  [] (make-path (.get (Configuration.) "hadoop.tmp.dir")))

(defn create-new-file
  "Creates the given path as a brand-new zero-length file."
  [path] (.createNewFile (filesystem path) (make-path path)))

(defn copy
  "Copy the file from source to destination."
  [source destination]
  (with-open [input (input-stream source) output (output-stream destination)]
    (let [buffer (byte-array *buffer-size*)]
      (loop [offset 0 length (.read input buffer)]
        (when (> length 0)
          (.write output buffer 0 length)
          (recur (+ offset length) (.read input buffer)))))))

(defn copy-from-local-file
  "Copy the local file from source to destination."
  [source destination & {:keys [delete overwrite]}]
  (let [destination (make-path destination)]
    (.copyFromLocalFile (filesystem destination) (or delete false) (make-path source) destination)
    destination))

(defn rename [source destination]
  (.rename (filesystem destination) (make-path source) (make-path destination)) )

(defn qualified-path [path]
  (let [path (make-path path)]
    (str (.makeQualified path (filesystem path)))))

(defmacro with-file-system [file-system & body]
  `(binding [*file-system* ~file-system]
     ~@body))

(defmacro with-config-file-system [config & body]
  `(with-file-system (FileSystem/get ~config)
     ~@body))

(defmacro with-local-file-system [& body]
  `(with-file-system (filesystem "file:/")
     ~@body))

(defmacro with-local-tmp-file [[symbol prefix] & body]
  (let [symbol# symbol]
    `(let [~symbol# (make-path (str (local-tmp-dir) "/" ~(or prefix "") (rand 100000)))
           result# (do ~@body)]
       (delete ~symbol#)
       result#)))
