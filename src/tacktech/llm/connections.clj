(ns tacktech.llm.connections
  "Module for interfacing with Large Language Models (LLMs).
   This module provides functions to connect to LLMs using a standardized
   connection specification."
  (:require
   [clojure.spec.alpha :as s]))

(defrecord ConnectionSpec [type connection-string api-key user model])

(s/def ::non-empty-string (s/and string? (complement empty?)))
(s/def ::type ::non-empty-string)
(s/def ::connection-string ::non-empty-string)
(s/def ::api-key ::non-empty-string)
(s/def ::user ::non-empty-string)
(s/def ::model ::non-empty-string)

(s/def ::connection-spec
  (s/keys :req-un [::type ::connection-string ::api-key ::user ::model]))

