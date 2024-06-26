(ns tacktech.llm.schemas
  (:require
   [malli.core :as m]
   [malli.generator :as mg]
   [clojure.string :as string]
   [wkok.openai-clojure.api :as api]))


;; Model Naming Convention and Provider Support
;;
;; In our system, we use a naming convention for models that includes both
;; the provider and the specific model name. This allows us to namespace
;; models by their providers while maintaining compatibility with external
;; API conventions.
;;
;; The model field in incoming queries follows this format:
;;   <provider-name>/<model-name>
;;
;; For example:
;;   "openai/gpt-4" or "anthropic/claude-2"

;; We support the following providers:
(def supported-providers #{"openai" "anthropic" "lmstudio"})

;; Internal representation
;; Internally, we separate the provider and model name for easier processing
(def provider-schema [:enum "openai" "anthropic" "lmstudio"])
(def internal-model-schema [:string {:min 1}])

;; External representation
;; Externally, we maintain the combined format for consistency with other APIs
(defn valid-model-format? [s]
  (let [[provider model] (string/split s #"/" 2)]
    (and (m/validate (m/schema provider-schema) provider)
         (m/validate (m/schema internal-model-schema) model))))

(def external-model-schema
  [:and
   [:string]
   [:fn
    {:error/message "Invalid model format. Expected 'provider/model'"} valid-model-format?]])

;; Function to split external model into provider and model name
(defn split-external-model
  "Splits an external model string into provider and model name.
   Returns a map with :provider and :model keys."
  [external-model]
  (let [[provider model] (string/split external-model #"/" 2)]
    {:provider provider
     :model model}))

;; When processing queries, we split the external model field into its
;; constituent parts (provider and model name). This allows us to:
;; 1. Validate the provider against our supported list
;; 2. Perform provider-specific operations or routing
;; 3. Handle the model name appropriately for each provider
;;
;; This approach enables dynamic dispatch based on the provider while
;; preserving a consistent external interface.

;; Usage:
;; To validate an external model:
;;   (m/validate external-model-schema "openai/gpt-4")
;;
;; To split and use an external model:
;;   (let [{:keys [provider model]} (split-external-model "openai/gpt-4")]
;;     (when (and (m/validate provider-schema provider)
;;                (m/validate internal-model-schema model))
;;       (process-query provider model)))
;;
;; To generate a valid external model (useful for testing):
;;   (mg/generate external-model-schema)

;; Common fields for both external and internal queries
(def supported-roles #{"system" "user" "assistant"})

(def message-schema
  [:map
   [:role [:enum "system" "user" "assistant"]]
   [:content :string]])

(def messages-schema
  [:vector message-schema])

;; External query schema
(def external-query-schema
  [:map
   [:model external-model-schema]
   [:messages messages-schema]])

;; Internal query schema
(def internal-query-schema
  [:map
   [:provider provider-schema]
   [:model internal-model-schema]
   [:messages messages-schema]])

;; Function to convert external query to internal query
(defn external->internal-query
  "Converts an external query to an internal query by splitting the model field."
  [external-query]
  (let [{:keys [model] :as query} external-query
        {:keys [provider model]} (split-external-model model)]
    (assoc (dissoc query :model) :provider provider :model model)))

;; Usage examples:
;; To validate an external query:
;;   (m/validate external-query-schema external-query)
;;
;; To validate an internal query:
;;   (m/validate internal-query-schema internal-query)
;;
;; To convert and validate:
;;   (let [internal-query (external->internal-query external-query)]
;;     (m/validate internal-query-schema internal-query))
;;
;; To generate a valid external query (useful for testing):
;;   (mg/generate external-query-schema)