(ns clojure-hadoop.flow
  (:require [clojure-hadoop.gen :as gen]
            [clojure-hadoop.imports :as imp]
            [clojure-hadoop.wrap :as wrap]
            [clojure-hadoop.config :as config]
            [clojure-hadoop.load :as load]
	    [clojure-hadoop.job :as job]
	    [clojure.stacktrace]
	    [clojure.string]))

(gen-class
 :name "clojure-hadoop.flow"
 :main true)

(imp/import-io)
(imp/import-mapreduce-lib-output)

;;
;; Flow registry
;;

(defonce components (atom {}))

(defn get-component
  "Get a component to apply when step is evaluated.
   Components are :source, :sink, and :shuffle"
  ([type name]
     (get-in @components [type name]))
  ([type name errorp]
     (if-let [comp (get-component type name)]
       comp
       (if errorp
	 (throw (Error. (str "Component " name " not found for type " type)))
	 nil))))

(defn put-component
  "Register a component for acquisition in later macro expansions"
  [type name comp]
  (when (get-component type name)
    (println "Redefining " type " named '" name "'"))
  (swap! components #(assoc-in %1 [type name] comp)))

;;
;; Define components
;;

(defn- full-name
  "Returns the fully-qualified name for a symbol s, either a class or
  a var, resolved in the current namespace."
  [s]
  (if-let [v (resolve s)]
    (cond (var? v) (let [m (meta v)]
                     (str (ns-name (:ns m)) \/
                          (name (:name m))))
          (class? v) (.getName ^Class v))
    (throw (Exception. (str "Symbol not found: " s)))))

(defn- kvs->map [kvs]
  (reduce (fn [m [k v]] (assoc m k v))
	  {} (partition 2 kvs)))

(defn- kvs-as-map [arglist kvs]
  (reduce
   (fn [m [k v]]
     (assoc m k (cond (keyword? v) (name v)
		      (number? v) (str v)
		      (string? v) v
		      (symbol? v) (if ((set arglist) v)
				    v
				    (full-name v))
		      (instance? Boolean v) (str v)
		      (and (sequential? v)
			   (#{:configure :params :post-hook} k)) v
		      :else (throw (Exception. (str "Invalid argument to component expression " k ", must be strings, symbols, keywords or bound variable references"))))))
   {} (apply hash-map kvs)))

(defn- make-component-expr [type name arglist keyvals]
  `(put-component ~type ~name
		  (fn [~@arglist]
		    ~(kvs-as-map arglist keyvals))))

(defmacro define-source
  "A source is a named function which takes arguments and returns
   a map of parameter settings to be merged in with the main job
   definition.  It is meant to be supplied to the step argument
   :source as a keyword name, or as a keyword/symbol in a simply
   application expression such as :source :text or
   :source (:dbfmt <jdbc-spec>)"
  [name [& args] & kvs]
  (make-component-expr :source name args kvs))

(defmacro define-sink
  "A sink is a function stored in the component registry which takes
   zero or more arguments and returns a map.  Any keys which are valid
   for defjob and define-step can be included here.  Any keys included
   in the including step will override the default key-value pairs in
   define-sink.  Sinks are defined using the :sink key to define-step
   expressions."
  [name [& arglist] & kvs]
  (make-component-expr :sink name arglist kvs))
  
(defmacro define-shuffle
  "Shuffle simplifies flows by specifying the map-writer and corresponding
   reduce-reader as well as any input-output key types necessary to specify.
   It can be used in define-step using the :shuffle key."
  [name [& args] & keyvals]
  (make-component-expr :shuffle name args keyvals))

(def component-keys [:source :sink :shuffle])

(defn- get-component-expr
  "Get the map for a step component"
  [type expr]
  (assert (keyword? type))
  (if (sequential? expr)
    (let [[comp & args] expr]
      `(apply (get-component ~type (keyword ~comp) true) (list ~@args)))
    `(apply (get-component ~type (keyword ~expr) true) [])))

(defn- get-component-exprs
  "Get source, sink and shuffle definitions and return a list of maps"
  [comps]
  (map #(let [[k vs] %]
	  (get-component-expr k vs))
       comps))

(defn cleanup-step-merge
  "If we override source/sink key-value pairs in a step, we'll
   get a list back so here we take the last element to act like
   a normal merge.  We keep the lists for :config and :params"
  [spec]
  (let [cfg (:configure spec)
	params (:params spec)
	rest (dissoc spec :configure :params)
	rest_keys (keys spec)
	rest_vals (vals spec)
	cleaned (zipmap rest_keys
			(map #(if (sequential? %) (last %) %) rest_vals))
	cleaned1 (if params
		   (assoc cleaned :params params)
		   cleaned)
	cleaned2 (if cfg
		   (assoc cleaned1 :configure cfg)
		   cleaned1)]
    cleaned2))


(defmacro define-step
  "A step is a function that composes key-value pair
   definitions from sources, sinks and shuffles.
   It is otherwise identical to a job."
  [name [& args] & keyvals]
  (let [kvmap (kvs->map keyvals)
	comps (select-keys kvmap component-keys)
	params (apply dissoc kvmap component-keys)]
    `(defn ~(with-meta name {:hadoop {:type :step}})       
       [~@args]
       (cleanup-step-merge
	(merge-with 
	 (comp vec flatten list)
	 ~@(get-component-exprs comps)
	 ~(kvs-as-map args (apply concat (seq params))))))))

(defn step-configuration [step & args]
  (apply step args))

(defn- job-counter [job group counter]
  (.findCounter (.getCounters job) group counter))

(defn job-counter-value
  "Utility function to help work with counters stored in
   completed jobs"
  [job group-name counter-name]
  (.getValue (job-counter job group-name counter-name)))

(defn is-step? [step]
  (and (fn? step)
       (= (:type (:hadoop (meta step))) :step)))

(defn do-step [step & args]
  (let [job-config (apply step args)
	job (clojure-hadoop.job/run (dissoc job-config :post-hook))]
    (loop []
	(if (.isComplete job)
	  true
	  (do (Thread/sleep 5000)
	      (recur))))
    (if-let [hook-name (:post-hook job-config)]
      ((load/load-name hook-name) job)
      (.isSuccessful job))))

(defn is-flow? [flow]
  (and (fn? flow)
       (= (:type (:hadoop (meta flow))) :flow)))

(defmacro define-flow
  "A flow is a function which dispatches a set of
   jobs dictated by branching and other logic embedded
   around dispatch-job statements.  It should return
   a boolean value indicating success."
  [name [& args] & body]
  `(defn ~(with-meta name {:hadoop {:type :flow}})
     [~@args]
     ~@body))
     
  
;; ================================
;; Some default components
;; ================================

;; Sources

(define-source :text [input]
  :input-format :text
  :output input
  :map-reader wrap/string-map-reader)

(define-source :int-text [input]
  :input-format :text
  :input input
  :map-reader wrap/int-string-map-reader)

;; Sinks

(define-sink :clojure [output]
  :reduce-writer wrap/clojure-writer
  :output-format :text
  :output-key Text
  :output-value Text
  :output output)

(define-sink :text [output]
  :reduce-writer wrap/clojure-writer ;; TODO: Fix this
  :output-format :text
  :output-key Text
  :output-value Text
  :output output)

(define-sink :null []
  :reduce-writer wrap/clojure-writer
  :output-format NullOutputFormat
  :output-key Text
  :output-value Text)

;; Shuffle

(define-shuffle :clojure []
  :map-writer wrap/clojure-writer
  :reduce-reader wrap/clojure-reduce-reader)


;;
;; Flow and Step top-level interface
;;

(defn run-flow
  "Run a flow.  We'll put some seatbelts in here later, for now
   any uncaught exceptions result in a failed flow."
  [flow args]
  (try
    (apply flow args)
    (catch java.lang.Throwable e
      (println e)
      false)))

;;
;; Entry point
;;

(defn- parse-arg [arg]
  (case (first arg)
	\( (read-string arg)
	\{ (read-string arg)
	\[ (read-string arg)
	\: (keyword (.substring arg 1))
	\' (symbol (.substring arg 1))
	arg))

(defn- parse-args [args]
  (map parse-arg args))

(defn- var-for-name [string]
  (let [[ns-name fn-name] (.split string "/")]
    (when-not (find-ns (symbol ns-name))
      (require (symbol ns-name)))
    (assert (find-ns (symbol ns-name)))
    (find-var (symbol ns-name fn-name))))

(defn- valid-operation? [type name]
  (= type (:type (:hadoop (meta (var-for-name name))))))

(defn- load-task [type opname]
  (if (valid-operation? type opname)
    (try
      (load/load-name opname)
      (catch java.lang.Error e
	(throw (java.lang.Error. (format "Unable to load %s %s" (name type) opname)))))
    (throw (java.lang.Error. (format "%s is not a hadoop %s operation" opname (name type))))))
      

(defn -main [run-type name & args]
  (assert (and (string? run-type) (string? name) (= (first run-type) \-)))
  (case run-type
	  "-job" (job/run-hadoop-job (clojure_hadoop.job.) #(job/parse-command-line % args))
	  "-step" (println (apply do-step (load-task :step name) (parse-args args)))
	  "-flow" (println (run-flow (load-task :flow name) (parse-args args)))))
	 
    