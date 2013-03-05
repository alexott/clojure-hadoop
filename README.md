A library to assist in writing Hadoop MapReduce jobs in Clojure.

Originally written by Stuart Sierra ([http://stuartsierra.com/](http://stuartsierra.com)).

Extended by Roman Scherer, Christopher Miles, Ian Eslick, Dave Lambert, Alex Ott, and other.

Stable releases are available via [http://clojars.org](http://clojars.org)

##Resources

* [Stuart's presentation on clojure-hadoop](http://vimeo.com/7669741)
* [Introduction to clojure-hadoop](http://alexott.net/en/clojure/ClojureHadoop.html)
* [Hadoop](http://hadoop.apache.org/)
* [Clojure](http://clojure.org/)

##Using the Library

This library provides different layers of abstraction away from the raw Hadoop API.

###Layer 1: clojure-hadoop.imports

Provides convenience functions for importing the many classes and interfaces in the Hadoop API.


###Layer 2: clojure-hadoop.gen

Provides gen-class macros to generate the multiple classes needed  for a MapReduce job.  See the example file "wordcount1.clj" for a demonstration of these macros.


###Layer 3: clojure-hadoop.wrap

Provides wrapper functions that automatically convert between Hadoop Text objects and Clojure data structures. See the example file "wordcount2.clj" for a demonstration of these wrappers.


###Layer 4: clojure-hadoop.job

Provides a complete implementation of a Hadoop MapReduce job that can be dynamically configured to use any Clojure functions in the map and reduce phases.  See the example file "wordcount3.clj" for a demonstration of this usage.


###Layer 5: clojure-hadoop.defjob

A convenient macro to configure MapReduce jobs with Clojure code. See the example files "wordcount4.clj" and "wordcount5.clj" for demonstrations of this macro.

##Requiring

You can either require this library through leiningen dependencies or maven2. It is important to notice that you must include the desired version of hadoop-core as well. 

Currently, versions 0.20.2 and 1.0.3 are tested and working.

###Leiningen

```
[clojure-hadoop "1.4.1"]
[org.apache.hadoop/hadoop-core "1.0.3"]
```

###Maven2

```
    <dependencies>
      ...

      <dependency>
        <groupId>clojure-hadoop</groupId>
        <artifactId>clojure-hadoop</artifactId>
        <version>1.4.1</version>
      </dependency>
      
      <dependency>
        <groupId>org.apache.hadoop</groupId>
        <artifactId>hadoop-core</artifactId>
        <version>1.0.3</version>
      </dependency>

      ...
    </dependencies>
    ...
    <repositories>
      ...

      <repository>
        <id>clojars</id>
        <url> http://clojars.org/repo </url>
      </repository>

      ...
    </repositories>
```


##Building from source

In the top-level directory of this project, run:

```
lein jar
```

This will compile and build the JAR file.


###Dependencies

* [Java 6 JDK](http://java.sun.com/)
* [Hadoop core]()
* [Leiningen](http://github.com/technomancy/leiningen)


##Running the Examples and Tests

With hadoop 0.20.2:

```
lein with-profile 0.20.2 test
```

With hadoop 1.0.3:

```
lein with-profile 1.0.3 test
```


##License

Copyright (c) Stuart Sierra, 2009. All rights reserved.  The use and distribution terms for this software are covered by the Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can be found in the file LICENSE.html at the root of this distribution. By using this software in any fashion, you are agreeing to be bound by the terms of this license. You must not remove this notice, or any other, from this software.