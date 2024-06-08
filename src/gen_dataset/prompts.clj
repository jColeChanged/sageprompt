(ns gen-dataset.prompts 
  "Module for loading prompt content from disk with caching functionality."
  (:require [clojure.java.io :as io]))

(defn load-prompt-content-from-disk-non-caching
  "Loads a prompt's content from the disk.
   
   Args:
     filepath: The filepath of the prompt to load.
   
    Throws:
     FileNotFoundException: If the file does not exist.
     IOException: If an I/O error occurs.
     SecurityException: If a security manager denies read access.
     IllegalArgumentException: If an invalid argument is passed.
     OutOfMemoryError: If there is insufficient memory to read the file.
   
   Returns the file contents as a string."
  [filepath]
  (with-open [r (io/reader filepath)]
    (slurp r)))

(def load-prompt-content-from-disk-caching 
  "Loads a prompt's content from the either cache or disk.  Caches the prompt when loading.
   
    Args:
     filepath: The filepath of the prompt to load.
   
    Throws:
     FileNotFoundException: If the file does not exist.
     IOException: If an I/O error occurs.
     SecurityException: If a security manager denies read access.
     IllegalArgumentException: If an invalid argument is passed.
     OutOfMemoryError: If there is insufficient memory to read the file.
   
   Returns the file contents as a string."
  (memoize load-prompt-content-from-disk-non-caching))

(def load-prompt-content
  "Loads a prompt's content.
   
   Args:
     filename (str): The filename of the prompt to load.
   
   Throws:
     FileNotFoundException: If the file does not exist.
     IOException: If an I/O error occurs.
     SecurityException: If a security manager denies read access.
     IllegalArgumentException: If an invalid argument is passed.
     OutOfMemoryError: If there is insufficient memory to read the file.
   
   Returns the file contents as a string."
  load-prompt-content-from-disk-caching)

