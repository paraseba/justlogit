# justlogit

Minimal logging library for Clojure

I don't like clojure.contrib.logging. I like even less log4j and the rest of Java
logging paraphernalia. I just want to log.

I like to log to standard error output
([read this](http://adam.heroku.com/past/2011/4/1/logs_are_streams_not_files/)
to get an idea of why) so that's the default for justlogit.

## Usage

There are six log levels: `fatal`, `error`, `warn`, `info`, `debug` and `trace`. For each one
there are two macros named `level` and `level-t`. To the first style macros you pass
arguments as in `clojure.core/format`. To the second class of macros, you pass a
`Throwable` as first argument.

## Samples

    (fatal "A bad thing happened: %s, aborting" (get-bad-thing))

will output something like

    FATAL 2011-04-05T19-55-06.859 [your-ns]: A bad thing happened: bad thing, aborting

If you want to log a `Throwable` instance, use:

    (try
      (danger)
      (catch Exception e
        (warn e "So, what now")))

Which will produce something like

    WARN 2011-04-05T19-55-06.859 [your-ns]: So, what now
    java.lang.Exception: transaction rolled back: foooooo
     at clojure.contrib.sql.internal$throw_rollback.invoke (internal.clj:142)
        clojure.contrib.sql.internal$transaction_STAR_.invoke (internal.clj:169)
    .....
    .....

## Advanced use

If you don't want the defaults you can customize the behaviour using both context
macros and global setters:

    (set-log-level :warn)
    ; warn is set as minimum log level globally.
    ; calls of lower priority won't be logged (nor evaluated)

    (with-log-level :warn
      ;inside this context the minimum log level is set to :warn
      (do-your-thing))
      ;and everything is back to normal now


    (set-log-writer my-writer) ;sets the global OutputStreamWriter to use for logging

    (with-log-writer my-writer
      (do-your-thing))  ;scoped writer


You can also customize the log format, using a function that returns the logged string.
This function takes a map with keys: `:level`: (Keyword), `:timestamp` (java.util.Date),
`:namespace` (String), `:message` (string), `:throwable` (optional Throwable instance)

    ;set a new formatter globally
    (set-log-formatter #(str "Broken: " (:message %)))

    ;scoped formatter
    (with-log-formatter #(str "Broken: " (:message %))
      (debug "I'm here"))

## Contributors

* Sebastián Galkin ([@paraseba](https://twitter.com/paraseba))
* Brendan Ribera ([@abscondment](https://twitter.com/abscondment))

## License

Copyright (C) 2011 Sebastián Galkin

Distributed under the Eclipse Public License, the same as Clojure.
