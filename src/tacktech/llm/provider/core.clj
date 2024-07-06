(ns tacktech.llm.provider.core)

;; Define a record to represent an API provider with a name and endpoint.
;; This will help in structuring the provider details in a clear and consistent way.
(defrecord Provider [name endpoint api-key])

;; Create instances of the Provider record for each supported provider.
;; Each provider has a unique name and endpoint where API requests will be sent.
(def anthropic (->Provider "anthropic" "https://api.anthropic.com/v1" (System/getenv "ANTHROPIC_API_KEY")))
(def lmstudio (->Provider "lmstudio" "http://localhost:1234/v1" "lmstduio"))
(def openai (->Provider "openai" "https://api.openai.com/v1" (System/getenv "OPENAI_API_KEY")))

;; Collect all provider instances into a single list.
;; This list will be used to create other data structures for easy access and lookup.
(def providers [anthropic lmstudio openai])

;; Extract the names of the providers into a list.
;; This helps in quickly referencing provider names without accessing the whole provider objects.
(def ^:private supported-providers-list (mapv :name providers))

;; Create a map that associates provider names with their respective provider instances.
;; This allows for efficient retrieval of provider details based on their name.
(def name->provider (zipmap supported-providers-list providers))

;; Define a function to retrieve a provider by name.
;; This function checks if a provider exists for the given name and returns it,
;; otherwise, it throws an exception with a meaningful error message.
(defn get-provider 
  ^Provider [^String provider-name]
  (if-let [provider (name->provider provider-name)]
    provider
    (throw (ex-info "Provider with the given name not found." {:provider-name provider-name}))))