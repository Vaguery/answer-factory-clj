(ns answer-factory.operator.select
  (:require [answer-factory.answer.push :as answer]))

;; Notes on the fundamental structure of the data store and how it affects function calls.
;;
;; The Answers table contains information about genomes and programs only.
;; The Rubrics table contain information about running and evaluating programs only.
;; The Scores table contain all info about scores obtained when applying a Rubric to an Answer
;;
;; Thus, for consistency all selection operators are built to take TWO arguments: a collection of Answer records, and a collection of Score records.


(defn uniform-selection
  "Returns a single element of the `answers` collection passed in, selected at random with uniform probability, disregarding the `scores` argument (which is still required)"
  [answers scores]
  [(rand-nth answers)])


(defn scores-for-answer
  "Takes a collection of Score hashmaps, and a single Answer record, and returns the subset of the scores which refer to that Answer by :id"
  [scores answer]
  (let [which (:id answer)]
    (filter #(= (:answer-id %) which) scores)))


(defn scores-for-rubric
  "Takes a collection of Score hashmaps, and a single Rubric record, and returns the subset of the scores which refer to that Rubric by :id"
  [scores rubric]
  (let [which (:id rubric)]
    (filter #(= (:rubric-id %) which) scores)))


(defn appears-on-list-of-ids?
  "Takes an answer and a collection of :id values, and returns true if the answer's :id appears on the list"
  [answer list-of-ids]
  (boolean (some #{(:id answer)} list-of-ids)))


(defn numeric-only
  "Takes a table of score records, and a single Rubric record; removes all records from the collection of records where the indicated Rubric does not have numeric value. Throws an exception if the resulting collection would be empty"
  [scores rubric]
  (let [result
    (remove #( (complement number?) (:score %)) (scores-for-rubric scores rubric))]
    (if (empty? result)
      (throw
        (Exception. (str "No valid scores for rubric :id " (:id rubric))))
      result)))


(defn simple-selection
  "Takes a collection of Answer records, a collection of Scores, and a single Rubric record. Returns all Answers which have the lowest score on the indicated rubric."
  [answers scores rubric]
  (let [useful-scores (numeric-only scores rubric)
        min-score     (apply min (map :score useful-scores))
        best-scores   (filter #(= (:score %) min-score) useful-scores)
        winning-ids   (map :answer-id best-scores)]
    (into [] (filter #(appears-on-list-of-ids? % winning-ids) answers))))


(defn lexicase-selection
  "Takes a collection of Answer records, a collection of Scores, and a collection of Rubric records. NOTE: returns all answers which filter through; does not sample at the end."
  [answers scores rubrics]
  (loop [survivors answers
         criteria (shuffle rubrics)]
    (cond (empty? criteria) survivors
          (= 1 (count survivors)) survivors
          :else
            (let [criterion (first criteria)]
              (recur (simple-selection survivors scores criterion)
                     (rest criteria))))))


;; multiobjective selection


(defn every-rubric
  "returns a set of rubric keywords, which is the union of all the :score keys in every answer in the argument collection"
  [answers]
  (reduce
    #(into %1 (keys (:scores %2)))
    #{}
    answers))


(defn dominated-by?
  "returns true if the second (answer) argument dominates the first; if a collection of rubrics is specified, that is used as the basis of comparison; otherwise, the union of the :scores keys of both answers are used; if any scores in either answer are nil, it returns false"
  [a1 a2 & [rubrics]]
  (let [k     (if rubrics
                (seq rubrics)
                (set (concat (keys (:scores a1)) (keys (:scores a2)))))
        s1    (map (:scores a1) k)
        s2    (map (:scores a2) k)
        delta (map compare s1 s2)]
    (and (not-any? nil? s1)
         (not-any? nil? s2)
         (and
           (boolean (some pos? delta))
           (not-any? neg? delta)))))


(defn remove-dominated
  "takes an answer and a collection of answers, and removes from the latter all answers dominated by the first argument; if an optional rubrics collection is passed in, that is used for the basis of comparison"
  [answer answers & [rubrics]]
  (remove #(dominated-by? % answer rubrics) answers))


(defn nondominated
  "takes a collection of answers; removes any that are dominated by any others; if _any_ answer in the entire collection has an extra score, or lacks a score the others have (or it has a nil value), then _all_ the answers are returned"
  [answers & [rubrics]]
  (let [universe    (every-rubric answers)
        consistent? (every? #(= (set (keys (:scores %))) universe) answers)
        covered?    (every? #(not-any? nil? (vals (:scores %))) answers)]
    (if (and consistent? covered?)
      (reduce
        (fn [keep check] (remove-dominated check keep rubrics))
        answers
        answers)
      answers)))


;; filtering a single VECTOR rubric

