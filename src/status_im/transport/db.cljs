(ns ^{:doc "DB spec and utils for the transport layer"}
 status-im.transport.db
  (:require [cljs.spec.alpha :as spec]
            [clojure.string :as s]
            status-im.contact.db
            [status-im.utils.clocks :as utils.clocks]))

;; required
(spec/def ::ack (spec/coll-of string? :kind vector?))
(spec/def ::seen (spec/coll-of string? :kind vector?))
(spec/def ::pending-ack (spec/coll-of string? :kind vector?))
(spec/def ::pending-send (spec/coll-of string? :kind vector?))
(spec/def ::resend? (spec/nilable #{"contact-request" "contact-request-confirmation" "contact-update"}))

;; optional
(spec/def ::topic (spec/nilable string?))
(spec/def ::topics (spec/coll-of ::topic :min-count 1))
(spec/def ::sym-key-id (spec/nilable string?))
;;TODO (yenda) remove once go implements persistence
(spec/def ::sym-key (spec/nilable string?))
(spec/def :transport/filter-id (spec/or :keyword keyword?
                                        :chat-id :global/not-empty-string))
(spec/def :transport/filter any?)

(spec/def :pairing/installation-id :global/not-empty-string)
(spec/def :pairing/device-type :global/not-empty-string)

(spec/def :transport/chat (spec/keys :req-un [::ack ::seen ::pending-ack ::pending-send ::topic]
                                     :opt-un [::sym-key-id ::sym-key ::resend?]))
(spec/def :transport/chats (spec/map-of :global/not-empty-string :transport/chat))
(spec/def :transport/filters (spec/map-of :transport/filter-id :transport/filter))

(defn create-chat
  "Initialize datastructure for chat representation at the transport level
  Currently only :topic is actually used"
  [{:keys [topic resend? now]}]
  {:ack                   []
   :seen                  []
   :pending-ack           []
   :pending-send          []
   :resend?               resend?
   :topic                 topic})

(spec/def ::profile-image :contact/photo-path)

(spec/def :chat/name (spec/nilable string?))

(spec/def :group-chat/admin :global/public-key)
(spec/def :group-chat/signature :global/not-empty-string)
(spec/def :group-chat/chat-id :global/not-empty-string)
(spec/def :group-chat/type :global/not-empty-string)
(spec/def :group-chat/member :global/not-empty-string)
(spec/def :group-chat/name :global/not-empty-string)

(spec/def :group-chat/event   (spec/keys :req-un [::clock-value :group-chat/type] :opt-un [:group-chat/member :group-chat/name]))
(spec/def :group-chat/events  (spec/coll-of :group-chat/event))
(spec/def :group-chat/membership-updates (spec/coll-of (spec/keys :req-un [:group-chat/signature :group-chat/events])))

(spec/def :message.content/text (spec/and string? (complement s/blank?)))
(spec/def :message.content/response-to string?)
(spec/def :message.content/response-to-new string?)
(spec/def :message.content/command-path (spec/tuple string? (spec/coll-of (spec/or :scope keyword? :chat-id string?) :kind set? :min-count 1)))
(spec/def :message.content/params (spec/map-of keyword? any?))

(spec/def ::content-type #{"text/plain" "command" "command-request"})
(spec/def ::message-type #{:group-user-message :public-group-user-message :user-message})
(spec/def ::clock-value (spec/and pos-int?
                                  utils.clocks/safe-timestamp?))
(spec/def ::timestamp (spec/nilable pos-int?))

(spec/def :message/id string?)
(spec/def :message/ids (spec/coll-of :message/id :kind set?))

(spec/def ::message (spec/or :message/contact-request :message/contact-request
                             :message/contact-update :message/contact-update
                             :message/contact-request-confirmed :message/contact-request-confirmed
                             :message/message :message/message
                             :message/message-seen :message/message-seen
                             :message/group-membership-update :message/group-membership-update))

(spec/def :message/contact-request (spec/keys :req-un [:contact/name ::profile-image :contact/address :contact/fcm-token]))
(spec/def :message/contact-update (spec/keys :req-un [:contact/name ::profile-image :contact/address :contact/fcm-token]))
(spec/def :message/contact-request-confirmed (spec/keys :req-un [:contact/name ::profile-image :contact/address :contact/fcm-token]))
(spec/def :message/new-contact-key (spec/keys :req-un [::sym-key ::topic ::message]))

(spec/def :message/message-seen (spec/keys :req-un [:message/ids]))

(spec/def :message/group-membership-update (spec/keys :req-un [:group-chat/membership-updates :group-chat/chat-id]))
(spec/def :message/sync-installation (spec/keys :req-un [:contacts/contacts]))
(spec/def :message/pair-installation (spec/keys :req-un [:pairing/installation-id
                                                         :pairing/device-type]))

(spec/def :message/message-common (spec/keys :req-un [::content-type ::message-type ::clock-value ::timestamp]))
(spec/def :message.text/content (spec/keys :req-un [:message.content/text]
                                           :req-opt [:message.content/response-to]))
(spec/def :message.command/content (spec/keys :req-un [:message.content/command-path :message.content/params]))

(defmulti content-type :content-type)

(defmethod content-type "command" [_]
  (spec/merge :message/message-common
              (spec/keys :req-un [:message.command/content])))

(defmethod content-type "command-request" [_]
  (spec/merge :message/message-common
              (spec/keys :req-un [:message.command/content])))

(defmethod content-type :default [_]
  (spec/merge :message/message-common
              (spec/keys :req-un [:message.text/content])))

(spec/def :message/message (spec/multi-spec content-type :content-type))

(defn all-filters-added?
  [{:keys [db]}]
  (let [filters (set (keys (get db :transport/filters)))
        chats (into #{:discovery-topic}
                    (keys (filter #(:topic (val %)) (get db :transport/chats))))]
    (= chats filters)))
