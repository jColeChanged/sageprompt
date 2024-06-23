(ns gen-dataset.core
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
(def provider-schema [:enum supported-providers])
(def internal-model-schema [:string {:min 1}])

;; External representation
;; Externally, we maintain the combined format for consistency with other APIs
(def external-model-schema
  [:and
   string?
   [:fn
    {:error/message "Invalid model format. Expected 'provider/model'"}
    (fn [s]
      (let [[provider model] (string/split s #"/")]
        (and (contains? supported-providers provider)
             (string? model)
             (not (string/blank? model)))))]
   [:re #"^[a-zA-Z0-9-]+/[a-zA-Z0-9-]+$"]])

;; Function to split external model into provider and model name
(defn split-external-model
  "Splits an external model string into provider and model name.
   Returns a map with :provider and :model keys."
  [external-model]
  (let [[provider model] (string/split external-model #"/")]
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


(defn split-model 
  [model]
  (let [parts (string/split model #"/")]
    {:provider (first parts)
     :model (second parts)}))


(defn create-chat-completion
  [connection-spec session query]
  (api/create-chat-completion
   {:model    (:model connection-spec)
    :messages (:messages query)}
   {:api-key (:api-key connection-spec)
    :api-endpoint (:api-endpoint connection-spec)}))
  
