clojure-hadoop

A library to assist in writing Hadoop MapReduce jobs in Clojure.

Originally written by Stuart Sierra (http://stuartsierra.com/).
Extended by Roman Scherer, Christopher Miles, Ian Eslick, 
Dave Lambert, Alex Ott, and other.

Stable releases are available via http://clojars.org

For more information
on Clojure, http://clojure.org/
on Hadoop, http://hadoop.apache.org/

Also see Stuart's presentation about this library at
http://vimeo.com/7669741

Introduction to work with library is available at
http://alexott.net/en/clojure/ClojureHadoop.html

Copyright (c) Stuart Sierra, 2009. All rights reserved.  The use and
distribution terms for this software are covered by the Eclipse Public
License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
be found in the file LICENSE.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.  You must not remove this notice, or any
other, from this software.


DEPENDENCIES

This library requires Java 6 JDK, http://java.sun.com/

Building from source requires Leiningen, http://github.com/technomancy/leiningen


BUILDING

If you downloaded the library distribution as a .zip or .tar file,
everything is pre-built and there is nothing you need to do.

If you downloaded the sources from Git, then you need to run the build
with Leiningen. In the top-level directory of this project, run:

    lein jar

This compiles and builds the JAR file.


RUNNING THE EXAMPLES & TESTS

After building, copy the file from

    clojure-hadoop-${VERSION}.jar

to something short, like "examples.jar".  Each of the *.clj files in
the test/clojure_hadoop/examples directory contains instructions for
running that example.

The wordcount examples can also be run via the "lein test" command.


USING THE LIBRARY IN HADOOP

After building, include the "clojure-hadoop-${VERSION}.jar" file
in the lib/ directory of the JAR you submit as your Hadoop job.


DEPENDING ON THE LIBRARY WITH MAVEN

You can depend on clojure-hadoop in your Maven 2 projects by adding
the following lines to your pom.xml:

    <dependencies>
      ...

      <dependency>
        <groupId>clojure-hadoop</groupId>
        <artifactId>clojure-hadoop</artifactId>
        <version>${VERSION}</version>
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


USING THE LIBRARY

This library provides different layers of abstraction away from the
raw Hadoop API.

Layer 1: clojure-hadoop.imports

    Provides convenience functions for importing the many classes and
    interfaces in the Hadoop API.

Layer 2: clojure-hadoop.gen

    Provides gen-class macros to generate the multiple classes needed
    for a MapReduce job.  See the example file "wordcount1.clj" for a
    demonstration of these macros.

Layer 3: clojure-hadoop.wrap

    clojure-hadoop.wrap: provides wrapper functions that automatically
    convert between Hadoop Text objects and Clojure data structures.
    See the example file "wordcount2.clj" for a demonstration of these
    wrappers.

Layer 4: clojure-hadoop.job

    Provides a complete implementation of a Hadoop MapReduce job that
    can be dynamically configured to use any Clojure functions in the
    map and reduce phases.  See the example file "wordcount3.clj" for
    a demonstration of this usage.

Layer 5: clojure-hadoop.defjob

    A convenient macro to configure MapReduce jobs with Clojure code.
    See the example files "wordcount4.clj" and "wordcount5.clj" for
    demonstrations of this macro.

 Layer 7: clojure-hadoop.config - Adding files and archives 
to the DistributedCache.
    
    Example file "wordcount7.clj" demonstrates how to specify files
    and archives for distribution to across nodes via the 
    DistributedCache, as well as how to access the files 
    during the mapper-setup or reducer-setup phases.


NOTES

* README.txt changed to reflect the Leiningen build process (Roman Scherer).
