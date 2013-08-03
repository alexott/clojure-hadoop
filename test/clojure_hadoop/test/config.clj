(ns clojure-hadoop.test.config
  (:use clojure.test
        clojure-hadoop.config
        clojure-hadoop.imports
        clojure-hadoop.defjob))

(import-conf)
(import-fs)
(import-io)
(import-io-compress)
(import-mapreduce)
(import-mapreduce-lib)

(deftest test-configuration
  (is (isa? (class (configuration (Job.))) Configuration)))

(deftest test-conf-job
  (testing "with a full qualified function name"
    (let [job (Job.)]    
      (defn my-job [] {:name "My Job"})
      (conf job :job "clojure-hadoop.test.config/my-job")
      (is (= (.getJobName job) "My Job")))))

(deftest test-conf-name
  (let [job (Job.)]
    (conf job :name "My Job")
    (is (= (.getJobName job) "My Job"))))

(deftest test-conf-input
  (let [job (Job.)]
    (conf job :input "file:/tmp/input/part-all")
    (is (= (seq (FileInputFormat/getInputPaths job)) [(Path. "file:/tmp/input/part-all")]))))

(deftest test-conf-output
  (let [job (Job.)]
    (conf job :output "file:/tmp/output")
    (is (= (FileOutputFormat/getOutputPath job) (Path. "file:/tmp/output")))))

(deftest test-conf-replace
  (let [job (Job.)]
    (conf job :replace true)
    (is (= (.get (configuration job) "clojure-hadoop.job.replace") "true")))
  (let [job (Job.)]
    (conf job :replace false)
    (is (nil? (.get (configuration job) "clojure-hadoop.job.replace")))))

(deftest test-conf-map
  (let [job (Job.)]
    (conf job :map 'identity)
    (is (= (.getMapperClass job) Mapper)))
  (let [job (Job.)]
    (conf job :map "user/mapper-map")
    (is (= (.get (configuration job) "clojure-hadoop.job.map") "user/mapper-map")))
  (let [job (Job.)]
    (conf job :map InverseMapper)
    (is (= (.getMapperClass job) InverseMapper))))

(deftest test-conf-map-cleanup
  (let [job (Job.)]
    (conf job :map-cleanup "user/mapper-cleanup")
    (is (= (.get (configuration job) map-cleanup) "user/mapper-cleanup"))))

(deftest test-conf-map-setup
  (let [job (Job.)]
    (conf job :map-setup "user/mapper-setup")
    (is (= (.get (configuration job) map-setup) "user/mapper-setup"))))

(deftest test-conf-reduce
  (let [job (Job.)]
    (conf job :reduce 'identity)
    (is (= (.getReducerClass job) Reducer)))
  (let [job (Job.)]
    (conf job :reduce "user/reducer-reduce")
    (is (= (.get (configuration job) "clojure-hadoop.job.reduce") "user/reducer-reduce")))
  (let [job (Job.)]
    (conf job :reduce IntSumReducer)
    (is (= (.getReducerClass job) IntSumReducer))))

(deftest test-conf-reduce-cleanup
  (let [job (Job.)]
    (conf job :reduce-cleanup "user/reduce-cleanup")
    (is (= (.get (configuration job) reduce-cleanup) "user/reduce-cleanup"))))

(deftest test-conf-reduce-setup
  (let [job (Job.)]
    (conf job :reduce-setup "user/reduce-setup")
    (is (= (.get (configuration job) reduce-setup) "user/reduce-setup"))))

(deftest test-conf-reduce-tasks
  (let [job (Job.)]
    (conf job :reduce-tasks 1)
    (is (= (.getNumReduceTasks job) 1)))
  (let [job (Job.)]
    (conf job :reduce-tasks "2")
    (is (= (.getNumReduceTasks job) 2)))
  (is (thrown-with-msg? IllegalArgumentException
        #"The reduce-tasks option must be an integer."
        (conf (Job.) :reduce-tasks "boom"))))

(deftest test-conf-combine
  (let [job (Job.)]
    (conf job :combine "user/combiner-combine")
    (is (= (.get (configuration job) "clojure-hadoop.job.combine") "user/combiner-combine")))
  (let [job (Job.)]
    (conf job :combine IntSumReducer)
    (is (= (.getCombinerClass job) IntSumReducer))))

(deftest test-conf-combine-cleanup
  (let [job (Job.)]
    (conf job :combine-cleanup "user/combine-cleanup")
    (is (= (.get (configuration job) combine-cleanup) "user/combine-cleanup"))))

(deftest test-conf-combine-setup
  (let [job (Job.)]
    (conf job :combine-setup "user/combine-setup")
    (is (= (.get (configuration job) combine-setup) "user/combine-setup"))))

(deftest test-conf-map-reader
  (let [job (Job.)]
    (conf job :map-reader "clojure-hadoop.wrap/clojure-map-reader")
    (is (= (.get (configuration job) "clojure-hadoop.job.map.reader")
           "clojure-hadoop.wrap/clojure-map-reader"))))

(deftest test-conf-map-writer
  (let [job (Job.)]
    (conf job :map-writer "clojure-hadoop.wrap/clojure-map-writer")
    (is (= (.get (configuration job) "clojure-hadoop.job.map.writer")
           "clojure-hadoop.wrap/clojure-map-writer"))
    (is (= (.get (configuration job) "clojure-hadoop.job.combine.writer")
           "clojure-hadoop.wrap/clojure-map-writer"))))

(deftest test-conf-map-output-key
  (let [job (Job.)]
    (conf job :map-output-key "org.apache.hadoop.io.IntWritable")
    (is (= (.getMapOutputKeyClass job) IntWritable))))

(deftest test-conf-map-output-value
  (let [job (Job.)]
    (conf job :map-output-value "org.apache.hadoop.io.Text")
    (is (= (.getMapOutputValueClass job) Text))))

(deftest test-conf-output-key
  (let [job (Job.)]
    (conf job :output-key "org.apache.hadoop.io.IntWritable")
    (is (= (.getOutputKeyClass job) IntWritable))))

(deftest test-conf-output-value
  (let [job (Job.)]
    (conf job :output-value "org.apache.hadoop.io.Text")
    (is (= (.getOutputValueClass job) Text))))

(deftest test-conf-reduce-reader
  (let [job (Job.)]
    (conf job :reduce-reader "clojure-hadoop.wrap/clojure-map-reader")
    (is (= (.get (configuration job) "clojure-hadoop.job.reduce.reader")
           "clojure-hadoop.wrap/clojure-map-reader"))
    (is (= (.get (configuration job) "clojure-hadoop.job.combine.reader")
           "clojure-hadoop.wrap/clojure-map-reader"))))

(deftest test-conf-reduce-writer
  (let [job (Job.)]
    (conf job :reduce-writer "clojure-hadoop.wrap/clojure-writer")
    (is (= (.get (configuration job) "clojure-hadoop.job.reduce.writer")
           "clojure-hadoop.wrap/clojure-writer"))))

(deftest test-conf-input-format
  (let [job (Job.)]
    (conf job :input-format 'text)
    (is (= (.getInputFormatClass job) TextInputFormat)))
  (let [job (Job.)]
    (conf job :input-format 'seq)
    (is (= (.getInputFormatClass job) SequenceFileInputFormat)))
  (let [job (Job.)]
    (conf job :input-format "org.apache.hadoop.mapreduce.lib.input.TextInputFormat")
    (is (= (.getInputFormatClass job) TextInputFormat))))

(deftest test-conf-output-format
  (let [job (Job.)]
    (conf job :output-format 'text)
    (is (= (.getOutputFormatClass job) TextOutputFormat)))
  (let [job (Job.)]
    (conf job :output-format 'seq)
    (is (= (.getOutputFormatClass job) SequenceFileOutputFormat)))
  (let [job (Job.)]
    (conf job :output-format "org.apache.hadoop.mapreduce.lib.output.TextOutputFormat")
    (is (= (.getOutputFormatClass job) TextOutputFormat))))

(deftest test-conf-compress-output
  (let [job (Job.)]
    (conf job :compress-output "true")
    (is (FileOutputFormat/getCompressOutput job)))
  (let [job (Job.)]
    (conf job :compress-output "false")
    (is (not (FileOutputFormat/getCompressOutput job)))))

(deftest test-conf-output-compressor
  (let [job (Job.)]
    (conf job :output-compressor "default")
    (is (= (FileOutputFormat/getOutputCompressorClass job DefaultCodec) DefaultCodec)))
  (let [job (Job.)]
    (conf job :output-compressor "gzip")
    (is (= (FileOutputFormat/getOutputCompressorClass job DefaultCodec) GzipCodec)))
  (let [job (Job.)]
    (conf job :output-compressor "bzip2")
    (is (= (FileOutputFormat/getOutputCompressorClass job DefaultCodec) BZip2Codec)))
  (let [job (Job.)]
    (conf job :output-compressor "org.apache.hadoop.io.compress.BZip2Codec")
    (is (= (FileOutputFormat/getOutputCompressorClass job DefaultCodec) BZip2Codec))))

(deftest test-conf-compression-type
  (let [job (Job.)]
    (conf job :compression-type "block")
    (is (= (SequenceFileOutputFormat/getOutputCompressionType job) SequenceFile$CompressionType/BLOCK)))
  (let [job (Job.)]
    (conf job :compression-type "none")
    (is (= (SequenceFileOutputFormat/getOutputCompressionType job) SequenceFile$CompressionType/NONE)))
  (let [job (Job.)]
    (conf job :compression-type "record")
    (is (= (SequenceFileOutputFormat/getOutputCompressionType job) SequenceFile$CompressionType/RECORD))))

(deftest test-parse-command-line-args
  (is (thrown-with-msg? IllegalArgumentException #"Missing required options"
        (parse-command-line-args (Job.) [])))
  (is (thrown-with-msg? IllegalArgumentException #"Number of options must be even"
        (parse-command-line-args (Job.) ["replace"])))
  (let [job (Job.)]
    (parse-command-line-args job ["replace" "true"])
    (is (= (.get (configuration job) "clojure-hadoop.job.replace") "true"))))

(deftest test-print-usage
  (let [out (with-out-str (print-usage))]
    (is (re-find #"Usage.*" out))))

(deftest test-defjob-creates-constant-configs
  (defjob my-job
    :timestamp (System/currentTimeMillis))
  (let [job-conf (my-job)]
    (Thread/sleep 20)
    (is (= job-conf (my-job)))))
