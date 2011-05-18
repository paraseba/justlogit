(ns justlogit-test
  (:import java.util.regex.Pattern)
  (:use [justlogit] :reload)
  (:use [clojure.test]))

(deftest enabling-test
  (testing "default log level"
    (is (not (enabled? :trace)))
    (is (not (enabled? :debug)))
    (is (not (enabled? :info)))
    (is (enabled? :warn))
    (is (enabled? :error))
    (is (enabled? :fatal)))

  (testing "valid log level"
    (with-log-level :info
      (is (enabled? :info))
      (is (enabled? :warn))
      (is (enabled? :error))
      (is (enabled? :fatal))
      (is (not (enabled? :debug)))
      (is (not (enabled? :trace)))
      (is (enabled? :foobar))
      (is (enabled? nil))))

  (testing "invalid log level"
    (with-log-level :foobar
      (is (enabled? :trace))
      (is (enabled? :debug))
      (is (enabled? :info))
      (is (enabled? :warn))
      (is (enabled? :error))
      (is (enabled? :fatal)))))

(defmacro with-str-logger [& body]
  `(let [s# (java.io.StringWriter.)
         pw# (java.io.PrintWriter. s#)]
     (with-log-writer pw#
       ~@body
       (str s#))))

(defmacro no-logs [& body]
  `(let [s# (with-str-logger ~@body)]
     (is (= "" s#))))

(defmacro logs-message [level msg & body]
  `(let [s# (with-str-logger ~@body)
         patt# (str "\\s*" (.toUpperCase (name ~level))
                   " \\d{4}-\\d{2}-\\d{2}T\\d{2}-\\d{2}-\\d{2}.\\d{3} "
                   "\\[justlogit-test\\]: " ~msg "\\n")
         patt# (java.util.regex.Pattern/compile patt#)]
     (is (re-matches patt# s#))))

(defmacro logs-message-t [level msg throwable & body]
  `(let [s# (with-str-logger ~@body)
         patt# (str "\\s*" (.toUpperCase (name ~level))
                   " \\d{4}-\\d{2}-\\d{2}T\\d{2}-\\d{2}-\\d{2}.\\d{3} "
                   "\\[justlogit-test\\]: " ~msg "\\n"
                   (.getName (class ~throwable))
                   ": "
                   (.getMessage ~throwable)
                    ".*")
         patt# (Pattern/compile patt# Pattern/DOTALL)]
     (is (re-matches patt# s#))))

(deftest default-logging
  (logs-message :fatal "hello" (fatal "hello"))
  (logs-message :error "hello" (error "hello"))
  (logs-message :warn "hello" (warn "hello"))
  (no-logs :info "" (info "hello"))
  (no-logs :debug "" (debug "hello"))
  (no-logs :trace "" (trace "hello")))

(deftest change-level-logging
  (with-log-level :debug
    (logs-message :fatal "hello" (fatal "hello"))
    (logs-message :error "hello" (error "hello"))
    (logs-message :warn "hello" (warn "hello"))
    (logs-message :info "hello" (info "hello"))
    (logs-message :debug "hello" (debug "hello"))
    (no-logs (trace "hello"))))

(deftest dont-evaluate-bellow-level
  (trace (throw (Exception.))))

(deftest global-level
  (set-log-level :debug)
  (logs-message :fatal "hello" (fatal "hello"))
  (logs-message :error "hello" (error "hello"))
  (logs-message :warn "hello" (warn "hello"))
  (logs-message :info "hello" (info "hello"))
  (logs-message :debug "hello" (debug "hello"))
  (no-logs (trace "hello"))
  (set-log-level :warn))

(deftest arguments
  (logs-message :fatal "100% broken!" (fatal "100% broken!"))
  (is (thrown? java.util.FormatFlagsConversionMismatchException (fatal "%s is 100% broken!" "foo")))
  (logs-message :fatal "Hi foo" (fatal "Hi %s" "foo"))
  (logs-message :fatal "Hi 1 foo" (fatal "Hi %d %s" 1 (reify Object (toString [_] "foo")))))

(deftest with-throwable
  (let [e (IllegalArgumentException. "foo")]
    (with-log-level :trace
      (logs-message-t :fatal "Hi foo" e (fatal-t e "Hi %s" "foo"))
      (logs-message-t :error "Hi foo" e (error-t e "Hi %s" "foo"))
      (logs-message-t :warn "Hi foo" e (warn-t e "Hi %s" "foo"))
      (logs-message-t :info "Hi foo" e (info-t e "Hi %s" "foo"))
      (logs-message-t :debug "Hi foo" e (debug-t e "Hi %s" "foo"))
      (logs-message-t :trace "Hi foo" e (trace-t e "Hi %s" "foo")))))

(deftest custom-formatter
  (with-log-formatter #(str "hello " (:message %))
    (is (= "hello cruel world" (with-str-logger (fatal "cruel world")))))

  (set-log-formatter #(str "hello " (:message %)))
  (is (= "hello cruel world" (with-str-logger (fatal "cruel world"))))
  (set-log-formatter default-formatter))

