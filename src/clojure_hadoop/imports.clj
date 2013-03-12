(ns clojure-hadoop.imports
  "Functions to import entire packages under org.apache.hadoop.")

(defn import-conf
  "Imports all classes/interfaces/exceptions from the package
  org.apache.hadoop.conf into the current namespace."
  []
  (import
   '(org.apache.hadoop.conf
     Configurable Configuration Configuration$IntegerRanges Configured)))

(defn import-io
  "Imports all classes/interfaces/exceptions from the package
  org.apache.hadoop.io into the current namespace."
  []
  (import
   '(org.apache.hadoop.io
     RawComparator SequenceFile$Sorter$RawKeyValueIterator SequenceFile$ValueBytes
     Stringifier Writable WritableComparable WritableFactory
     AbstractMapWritable ArrayFile ArrayFile$Reader ArrayFile$Writer
     ArrayWritable BooleanWritable BooleanWritable$Comparator BytesWritable
     BytesWritable$Comparator ByteWritable ByteWritable$Comparator
     CompressedWritable DataInputBuffer DataOutputBuffer DefaultStringifier
     DoubleWritable DoubleWritable$Comparator FloatWritable
     FloatWritable$Comparator GenericWritable InputBuffer IntWritable LongWritable
     LongWritable$Comparator IOUtils IOUtils$NullOutputStream LongWritable
     LongWritable$Comparator LongWritable$DecreasingComparator MapFile
     MapFile$Reader MapFile$Writer MapWritable MD5Hash MD5Hash$Comparator
     NullWritable NullWritable$Comparator ObjectWritable OutputBuffer
     SequenceFile SequenceFile$Metadata SequenceFile$Reader
     SequenceFile$Sorter SequenceFile$Writer SetFile SetFile$Reader
     SetFile$Writer SortedMapWritable Text Text$Comparator
     TwoDArrayWritable UTF8 UTF8$Comparator VersionedWritable VLongWritable
     VLongWritable WritableComparator WritableFactories WritableName
     WritableUtils SequenceFile$CompressionType MultipleIOException
     VersionMismatchException)))

(defn import-io-compress
  "Imports all classes/interfaces/exceptions from the package
  org.apache.hadoop.io.compress into the current namespace."
  []
  (import
   '(org.apache.hadoop.io.compress
     CompressionCodec Compressor Decompressor BlockCompressorStream
     BlockDecompressorStream BZip2Codec CodecPool CompressionCodecFactory
     CompressionInputStream CompressionOutputStream CompressorStream
     DecompressorStream DefaultCodec GzipCodec GzipCodec$GzipOutputStream)))

(defn import-fs
  "Imports all classes/interfaces/exceptions from the package
  org.apache.hadoop.fs into the current namespace."
  []
  (import
   '(org.apache.hadoop.fs
     PathFilter PositionedReadable Seekable Syncable BlockLocation
     BufferedFSInputStream ChecksumFileSystem ContentSummary DF DU
     FileStatus FileSystem FileSystem$Statistics FileUtil HardLink
     FilterFileSystem FSDataInputStream FSDataOutputStream FSInputChecker
     FSInputStream FSOutputSummer FsShell FsUrlStreamHandlerFactory HarFileSystem
     InMemoryFileSystem LocalDirAllocator LocalFileSystem Path RawLocalFileSystem
     Trash ChecksumException FSError)))

(defn import-mapreduce
  "Imports all classes/interfaces/exceptions from the package
  org.apache.hadoop.mapreduce into the current namespace."
  []
  (import
   '(org.apache.hadoop.mapreduce
     Counter CounterGroup Counters ID InputFormat InputSplit Job
     Job$JobState JobContext JobID MapContext Mapper OutputCommitter
     OutputFormat Partitioner RecordReader RecordWriter ReduceContext
     Reducer StatusReporter TaskAttemptContext TaskAttemptID TaskID
     TaskInputOutputContext)))

(defn import-filecache
  "Imports all classes/interfaces/exceptions from the package
  org.apache.hadoop.filecache into the current namespace."
  []
  (import
   '(org.apache.hadoop.filecache
     DistributedCache TaskDistributedCacheManager
     TrackerDistributedCacheManager)))

(defn import-mapreduce-lib-input
  "Imports all classes/interfaces/exceptions from the package
  org.apache.hadoop.mapreduce.lib.input into the current namespace."
  []
  (import
   '(org.apache.hadoop.mapreduce.lib.input
     FileInputFormat FileSplit LineRecordReader SequenceFileInputFormat
     SequenceFileRecordReader TextInputFormat InvalidInputException)))

(defn import-mapreduce-lib-map
  "Imports all classes/interfaces/exceptions from the package
  org.apache.hadoop.mapreduce.lib.map into the current namespace."
  []
  (import
   '(org.apache.hadoop.mapreduce.lib.map
     InverseMapper MultithreadedMapper TokenCounterMapper)))

(defn import-mapreduce-lib-output
  "Imports all classes/interfaces/exceptions from the package
  org.apache.hadoop.mapreduce.lib.output into the current namespace."
  []
  (import
   '(org.apache.hadoop.mapreduce.lib.output
     FileOutputCommitter FileOutputFormat NullOutputFormat
     SequenceFileOutputFormat TextOutputFormat TextOutputFormat$LineRecordWriter)))

(defn import-mapreduce-lib-partition
  "Imports all classes/interfaces/exceptions from the package
  org.apache.hadoop.mapreduce.lib.partition into the current namespace."
  []
  (import
   '(org.apache.hadoop.mapreduce.lib.partition HashPartitioner)))

(defn import-mapreduce-lib-reduce
  "Imports all classes/interfaces/exceptions from the package
  org.apache.hadoop.mapreduce.lib.reduce into the current namespace."
  []
  (import
   '(org.apache.hadoop.mapreduce.lib.reduce IntSumReducer LongSumReducer)))

(defn import-mapreduce-lib
  "Imports all classes/interfaces/exceptions from the package
  org.apache.hadoop.mapreduce.lib into the current namespace."
  []
  (import-mapreduce-lib-input)
  (import-mapreduce-lib-map)
  (import-mapreduce-lib-output)
  (import-mapreduce-lib-partition)
  (import-mapreduce-lib-reduce))
