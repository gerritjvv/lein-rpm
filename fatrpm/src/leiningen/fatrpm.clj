(ns leiningen.fatrpm)

(ns leiningen.fatrpm
  (:refer-clojure :exclude [replace])
  (:use [clojure.java.shell :only [sh]]
        [clojure.java.io :only [file delete-file writer copy]]
        [leiningen.core.eval :refer [eval-in-project]]
        [clojure.string :only [join capitalize trim-newline replace]]
        [leiningen.uberjar :only [uberjar]])
  (:import java.util.Date
           java.text.SimpleDateFormat
           (org.codehaus.mojo.rpm RPMMojo
                                  AbstractRPMMojo
                                  Mapping Source
                                  SoftlinkSource
                                  Scriptlet)
           (org.apache.maven.project MavenProject)
           (org.apache.maven.shared.filtering DefaultMavenFileFilter)
           (org.codehaus.plexus.logging.console ConsoleLogger)))

(defn write
  "Write string to file, plus newline"
  [file string]
  (with-open [w (writer file)]
    (.write w (str (trim-newline string) "\n"))))

(defn workarea
  [project]
  (file (:root project) "target" "rpm"))

(defn cleanup
  [project]
  (sh "rm" "-rf" (str (workarea project))))

(defn reset
  [project]
  (cleanup project)
  (sh "rm" (str (:root project) "/target/*.rpm")))

(defn get-version
  [project]
  (let [df   (SimpleDateFormat. ".yyyyMMdd.HHmmss")]
    (replace (:version project) #"-SNAPSHOT" (.format df (Date.)))))

(defn set-mojo!
  "Set a field on an AbstractRPMMojo object."
  [object name value]
  (let [field (.getDeclaredField AbstractRPMMojo name)]
    (.setAccessible field true)
    (.set field object value))
  object)

(defn array-list
  [list]
  (let [list (java.util.ArrayList.)]
    (doseq [item list] (.add list item))
    list))

(defn scriptlet
  "Creates a scriptlet backed by a file"
  [filename]
  (doto (Scriptlet.)
    (.setScriptFile (file filename))))

(defn source
  "Create a source with a local location and a destination."
  ([] (Source.))
  ([location]
   (doto (Source.)
     (.setLocation (str location))))
  ([location destination]
   (doto (Source.)
     (.setLocation (str location))
     (.setDestination (str destination)))))

(defn mapping
  [m]
  (doto (Mapping.)
    (.setArtifact           (:artifact m))
    (.setConfiguration      (case (:configuration m)
                              true  "true"
                              false "false"
                              nil   "false"
                              (:configuration m)))
    (.setDependency         (:dependency m))
    (.setDirectory          (:directory m))
    (.setDirectoryIncluded  (boolean (:directory-included? m)))
    (.setDocumentation      (boolean (:documentation? m)))
    (.setFilemode           (:filemode m))
    (.setGroupname          (:groupname m))
    (.setRecurseDirectories (boolean (:recurse-directories? m)))
    (.setSources            (:sources m))
    (.setUsername           (:username m))))

(def ^:dynamic project)

(defn eval2 [project s]
  (binding [project project]
    (eval s)))

(defn as-source-instance [project path]
  (binding [project project]
    (if (coll? path)
      (let [[src dest] (eval path)]
        (source (file (:root project) src) dest))
      (source (file (:root project) (eval path))))))

(defn update-sources
  "Create java source instances from source entries"
  [project {:keys [sources] :as source}]
  (if (not-empty sources)
    (assoc source :sources (doall (map (partial as-source-instance project) sources)))
    source))

(defn mappings
  [project]
  (map (comp mapping
             (partial merge {:username (get-in project [:rpm :username])
                             :groupname (get-in project [:rpm :groupname])}))

       (map (partial update-sources project) (get-in project [:rpm :mappings]))))

(defn blank-rpm
  "Create a new RPM file"
  []
  (let [mojo (RPMMojo.)
        fileFilter (DefaultMavenFileFilter.)]
    (set-mojo! mojo "project" (MavenProject.))
    (.enableLogging fileFilter (ConsoleLogger. 0 "Logger"))
    (set-mojo! mojo "mavenFileFilter" fileFilter)))

(defn create-dependency
  [rs]
  (let [hs (java.util.LinkedHashSet.)]
    (doseq [r rs] (.add hs r))
    hs))

(defn make-rpm
  "Create and execute a Mojo RPM."
  [project]
  (doto (blank-rpm)
    (set-mojo! "projversion" (get-version project))
    (set-mojo! "name" (:name project))
    (set-mojo! "summary" (:description project))
    (set-mojo! "copyright" (get-in project [:license :name]))

    (set-mojo! "workarea" (workarea project))
    (set-mojo! "mappings" (mappings project))

    (comment
      (set-mojo! "requires" (create-dependency ["jre >= 1.6.0"
                                                "daemonize >= 1.7.3"])))
    (.execute)))

(defn extract-rpm
  "Snags the RPM file out of its little mouse-hole and brings it up to target/,
  then generates an md5"
  [project]
  (let [dir (file (workarea project)
                  (:name project)
                  "RPMS"
                  "noarch")
        rpms (remove #(.isDirectory %) (.listFiles dir))]
    (doseq [rpm rpms]
      (let [dest (file (:root project) "target" (.getName rpm))]
        ; Move
        (.renameTo rpm dest)))))

(defn fatrpm
  ([project] (fatrpm project true))
  ([project uberjar?]
   (reset project)
   (when uberjar? (uberjar project))
   (make-rpm project)
   (extract-rpm project)
   (cleanup project)))
