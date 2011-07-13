(ns justlogit
  (:use [clojure.stacktrace :only [print-cause-trace]]))

(defn- indexed [s]
  (map vector (iterate inc 0) s))

(def ^{:private true} levels (indexed [:trace :debug :info :warn :error :fatal]))
(def ^{:dynamic true} *log-level* (atom :warn))
(def ^{:dynamic true} *log-writer* (atom (java.io.OutputStreamWriter. System/err)))

(defn set-log-level [level]
  (reset! *log-level* level))

(defn set-log-writer [writer]
  (reset! *log-writer* writer))

(defmacro with-log-level [level & body]
  `(binding [*log-level* (atom ~level)] ~@body))

(defmacro with-log-writer [writer & body]
  `(binding [*log-writer* (atom ~writer)] ~@body))

(defn- format-throwable [tr]
  (format "%s%n" (with-out-str (print-cause-trace tr))))

(defn default-formatter [log]
  (let [{:keys [level timestamp namespace message throwable]} log
        format-str "%5S %2$tY-%2$tm-%2$tdT%2$tH-%2$tM-%2$tS.%2$tL [%3$s]: %4$s%n"
        head (format format-str level timestamp namespace message)]
    (if throwable
      (format "%s%s%n" head (format-throwable throwable))
      head)))

(def ^{:dynamic true} *log-formatter* (atom default-formatter))

(defn set-log-formatter [formatter]
  (reset! *log-formatter* formatter))

(defmacro with-log-formatter [formatter & body]
  `(binding [*log-formatter* (atom ~formatter)] ~@body))

(defn- int-level [level]
  (ffirst (filter #(= level (second %)) levels)))

(defn enabled? [level]
  (>= (or (int-level level) Integer/MAX_VALUE)
      (or (int-level @*log-level*) Integer/MIN_VALUE)))

(defn format-if-needed [& args]
  (apply
   (if (> (count args) 1) format str)
   args))

(defmacro log [level throwable & args]
  `(when (enabled? ~level)
     (let [s# (@*log-formatter*
                 {:timestamp (java.util.Date.)
                  :namespace (.name ~*ns*)
                  :level (name ~level)
                  :throwable ~throwable
                  :message (format-if-needed ~@args)})]
       (io!
         (locking @*log-writer*
           (.write @*log-writer* s#)
           (.flush @*log-writer*))))))

(defn- def-level-macros [level]
  (let [args (gensym "args")
        throwable (gensym "throwable")]
    `(do
       (defmacro ~(symbol (name level)) [& ~args]
         `(log ~~level nil ~@~args))
       (defmacro ~(symbol (str (name level) "-t")) [~throwable & ~args]
         `(log ~~level ~~throwable ~@~args)))))

(defmacro ^{:private true} level-macro [& levels]
  (map def-level-macros levels))

(level-macro :trace :debug :info :warn :error :fatal)
