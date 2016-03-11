(ns fixme.status-bot.core
  (:require [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [dgellow.bottle.bot
             :refer [defaction
                     make-bot
                     make-envelope
                     stop-all! start-all!
                     say-to]]
            [clojure.java.shell :refer [sh]])
  (:import [dgellow.bottle.bot SlackAdapter])
  (:gen-class))

;; Bot entity
(def bot
  (atom nil))

;; Actions
(defaction hello-action
  "Greets the user"
  #"hello"
  (fn [_ _]
    (make-envelope
     {:message "Hello, I'm the status bot."})))

(defaction ping-foo-action
  "Ping foo.fixme.ch"
  #"ping foo"
  (fn [_ _]
    (let [{:keys [exit out err]} (sh "ping" "-c" "3" "foo.fixme.ch")]
      (make-envelope
       {:message
        (format "\n[Exit code] %s\n[Output]\n%s\n[Error]\n%s"
                exit out err)}))))

(defaction ping-router-action
  "Ping router (62.220.131.170)"
  #"ping router"
  (fn [_ _]
    (let [{:keys [exit out err]} (sh "ping" "-c" "3" "62.220.131.170")]
      (make-envelope
       {:message
        (format "\n[Exit code] %s\n[Output]\n%s\n[Error]\n%s"
                exit out err)}))))

(defrecord Service [label command])

(defn make-pingservice [label domain-or-ip]
  (Service. label (list "ping" "-c" "3" domain-or-ip)))

(defn make-webservice [url]
  (Service. url (list "curl" url)))

(def foo-pingservices (map (fn [x] (apply make-pingservice x))
                    (partition 2 ["ping foo.fixme.ch" "foo.fixme.ch"])))

(def foo-webservices (map make-webservice
                        ["http://foo.fixme.ch"
                         "https://foo.fixme.ch"
                         "https://git.fixme.ch"
                         "https://pad.fixme.ch"
                         "https://wiki.fixme.ch"
                         "https://trigger.fixme.ch"
                         "https://mpd.fixme.ch"]))

(def foo-services (concat foo-pingservices foo-webservices))

(defn check-services [services]
  (map (fn [x] (assoc x :ok? (= 0 (:exit (apply sh (:command x))))))
    foo-services))

(defn format-status-foo [checked-services]
  (apply str "Services status:\n"
    (map #(format "%s\t%s\n"
                  (if (:ok? %) ":white_check_mark:" ":x:")
                  (:label %))
      checked-services)))

(defaction status-foo-action
  "Gives status of services on foo.fixme.ch"
  #"status"
  (fn [_ _]
    (make-envelope
     {:message (format-status-foo (check-services foo-services))})))

(defaction help-action
  "Display help"
  #"help"
  (fn [_ {:keys [bot]}]
    (make-envelope
     {:message
      (format "\n%s\n"
              (clojure.string/join
               "\n"
               (map #(format "- \"%s\": %s" (:pattern %)
                             (:doc (meta %))) (:actions bot))))})))

;; Watches
(defn set-interval [callback ms]
  (future (while true (do (Thread/sleep ms) (callback)))))

(def watch-foo-status
  (set-interval
   (fn []
     (let [result (check-services foo-services)]
       (when (not-every? :ok? result)
         (doseq [[_ adapter] (:adapters @bot)]
           (say-to adapter
                   {:room "10_monitoring"
                    :message (format-status-foo result)})))))
   ;; run every 5 minutes
   (* 5 60 1000)))


;; Bot definition
(def bot-spec
  (let [slack-token (env :slack-api-token)]
    (assert slack-token "Missing environment variable SLACK_API_TOKEN")
    (make-bot {:name "status_bot"
               :actions [hello-action
                         help-action
                         ping-foo-action
                         ping-router-action
                         status-foo-action]
               :adapters [(SlackAdapter. slack-token)]})))

;; Entry points
(defn stop-bot! []
  (timbre/info "Stopping bot...")
  (future-cancel watch-foo-status)
  (stop-all! @bot)
  (reset! bot nil))
(defn start-bot! []
  (when @bot
    (stop-bot!))
  (timbre/info "Starting bot...")
  (reset! bot bot-spec)
  (when @bot
    (start-all! @bot)))

(defn -main [& args]
  (start-bot!))
