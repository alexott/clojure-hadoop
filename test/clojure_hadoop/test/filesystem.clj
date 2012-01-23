(ns clojure-hadoop.test.filesystem
  (:use [clojure.java.io :only [writer reader]]
        [clojure-hadoop.imports :only [import-io-compress]]
        [clojure.test]
        [clojure-hadoop.filesystem])
  (:import [java.io BufferedReader BufferedWriter InputStreamReader OutputStreamWriter PrintWriter]
           [org.apache.hadoop.fs FSDataInputStream FSDataOutputStream FileSystem LocalFileSystem Path]
           [org.apache.hadoop.conf Configuration]
           [org.apache.hadoop.fs.s3native NativeS3FileSystem]))

(defn read-lines [f]
  (let [read-lines (fn [rdr]
		     (if-let [line (.readLine rdr)]
		       (conj (read-lines rdr) line)
		       (.close rdr)))]
    (read-lines (reader f))))

(defn write-lines [f lines]
  (with-open [writer (PrintWriter. (writer f))]
    (doseq [line lines]
      (.write writer (str line))
      (.println writer))))

(import-io-compress)

(deftest test-buffered-reader
  (let [filename (str (local-tmp-dir) "/test-buffered-reader")]
    (create-new-file filename)
    (with-open [reader (buffered-reader filename)]
      (is (isa? (class reader) BufferedReader)))))

(deftest test-buffered-writer
  (let [filename (str (local-tmp-dir) "/test-buffered-writer")]
    (with-open [reader (buffered-writer filename)]
      (is (isa? (class reader) BufferedWriter)))))

(deftest test-buffered-writer-with-compression
  (let [filename (str (local-tmp-dir) "/test-buffered-writer.gz")]
    (with-open [reader (buffered-writer filename)]
      (is (isa? (class reader) BufferedWriter)))))

(deftest test-default-filesystem
  (let [fs (default-filesystem)]
    (is (isa? (class fs) FileSystem))
    (is (= (str (.getUri fs)) "file:///"))))

(deftest test-filesystem
  (let [fs (filesystem (str "file:///"))]
    (is (isa? (class fs) LocalFileSystem))
    (is (= (str (.getUri fs)) "file:///")))
  ;; (let [fs (filesystem (str "s3n://mybucket"))]
  ;;   (is (isa? (class fs) NativeS3FileSystem))
  ;;   (is (= (str (.getUri fs)) "s3n://mybucket")))
  )

(deftest test-compression-codec
  (is (nil? (compression-codec "unsupported")))
  (are [filename codec]
    (is (isa? (class (compression-codec filename)) codec))
    "test.deflate" DefaultCodec
    "test.gz" GzipCodec
    "test.bz2" BZip2Codec))

(deftest test-local-tmp-dir
  (is (= (local-tmp-dir)
         (make-path (str "file:" (System/getProperty "java.io.tmpdir"))))))

(deftest test-hadoop-tmp-dir
  (is (= (hadoop-tmp-dir)
         (make-path (.get (Configuration.) "hadoop.tmp.dir")))))

(deftest test-exists?
  (let [filename (str (local-tmp-dir) "/test-exists?")]
    (create-new-file filename)
    (is (exists? filename))
    (delete filename)
    (is (not (exists? filename)))))

(deftest test-make-directory
  (let [directory (str (local-tmp-dir) "/test-make-directory")]
    (make-directory (make-path directory))
    (is (exists? directory))))

(deftest test-create-new-file
  (let [filename (str (local-tmp-dir) "/test-create-new-file")]
    (delete filename)
    (create-new-file filename)
    (is (exists? filename))))

(deftest test-make-path
  (let [path (make-path (str "file:///tmp"))]
    (is (isa? (class path) Path)))
  (let [path (make-path (Path. "file:///tmp"))]
    (is (isa? (class path) Path)))
  (let [path (make-path (java.net.URI. "file:///tmp"))]
    (is (isa? (class path) Path)))
  (let [path (make-path (java.net.URI. "s3n:///mybucket"))]
    (is (isa? (class path) Path))))

(deftest test-with-file-system
  (let [file-system (LocalFileSystem.)]
    (with-file-system file-system
      (is (= *file-system* file-system)))))

(deftest test-with-local-file-system
  (with-local-file-system
    (is (isa? (class *file-system*) LocalFileSystem))))

(deftest test-with-config-file-system
  (let [config (Configuration.)]
    (with-config-file-system config
      (is (= *file-system* (FileSystem/get config))))))

(deftest test-input-stream
  (let [filename (str (local-tmp-dir) "/test-input-stream")]
    (create-new-file filename)
    (with-open [stream (input-stream filename)]
      (is (isa? (class stream) FSDataInputStream)))))

(deftest test-output-stream
  (let [filename (str (local-tmp-dir) "/test-output-stream")]
    (with-open [stream (output-stream filename)]
      (is (isa? (class stream) FSDataOutputStream)))))

(deftest test-buffered-io-with-compression
  (let [filename (str (local-tmp-dir) "/test-buffered-reader.gz") content ["1" "2"]]
    (with-open [writer (buffered-writer filename)]
      (.write writer (pr-str content)))

    (with-open [rdr (reader filename)]
      (is (not (= content (line-seq rdr))))
      (is (= (pr-str content) (.readLine (buffered-reader filename)))))))

(deftest test-copy
  (let [source (str (local-tmp-dir) "/test-copy-source")
        destination (str (local-tmp-dir) "/test-copy-destination")]
    (try
      (spit source "TEST")
      (copy source destination)
      (is (= (slurp source) (slurp destination)))
      (finally
       (delete source)
       (delete destination)))))

(deftest test-copy-from-local-file
  (let [source (str (local-tmp-dir) "/test-copy-from-local-file-source")
        destination (str (local-tmp-dir) "/test-copy-from-local-file-destination")]
    (testing "copy"
      (try
        (create-new-file source)
        (copy-from-local-file source destination)
        (is (exists? destination))
        (finally
         (delete source)
         (delete destination))))
    (testing "copy and delete source"
      (try
        (create-new-file source)
        (copy-from-local-file source destination :delete true)
        (is (not (exists? source)))
        (is (exists? destination))
        (finally (delete destination))))))

(deftest test-qualified-path
  (is (= (qualified-path "/tmp") "file:/tmp")))

(deftest test-with-local-tmp-file
  (is (not (exists? (with-local-tmp-file [filename]
                      (spit (str filename) "TEST")
                      (exists? filename)
                      filename)))))
