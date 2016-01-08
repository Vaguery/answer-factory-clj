(ns answer-factory.genome-test
  (:use midje.sweet)
  (:require [clojure.zip :as zip])
  (:use [answer-factory.genomes])
  (:use clojure.pprint))


;; fixtures


(def test-zipper (zip/seq-zip '(1 2 3 (4 5 (6)))))
(def empty-zipper (zip/seq-zip '()))
(def simple-zipper (zip/seq-zip '(:foo :bar :baz)))
(def stubby-zipper (zip/seq-zip '(() () (()))))


;; helpers

(fact "empty-zipper? returns true if the zipper argument is an empty seq"
  (empty-zipper? test-zipper) => false
  (empty-zipper? empty-zipper) => true)


;; movers

(fact "rewind moves the cursor of a zipper to its head (not its root!)"
  (zip/node (rewind test-zipper)) => 1

  (zip/node (rewind empty-zipper)) => nil
  (zip/end? (rewind empty-zipper)) => false

  (zip/node (rewind simple-zipper)) => :foo)


(fact "fast-forward moves the cursor to the tail (not the end!)"
  (zip/node (fast-forward test-zipper)) => 6

  (zip/node (fast-forward empty-zipper)) => nil
  (zip/end? (fast-forward empty-zipper)) => false

  (zip/node (fast-forward simple-zipper)) => :baz)


(fact "wrap-left moves the cursor one step left"
  (zip/node (wrap-left (zip/down test-zipper))) => '(4 5 (6))
  (zip/node (wrap-left (zip/next (zip/next test-zipper)))) => 1
  (zip/node (wrap-left test-zipper)) => '(4 5 (6))

  (zip/node (wrap-left empty-zipper)) => nil

  (zip/node (wrap-left (zip/down simple-zipper))) => :baz)

(fact "wrap-left stays inside a sublist"
  (let [in-subtree
    (-> test-zipper zip/next zip/next zip/next zip/next zip/next zip/next)]
    (zip/node in-subtree) => 5
    (zip/node (wrap-left in-subtree)) => 4
    (zip/node (wrap-left (wrap-left in-subtree))) => '(6)
    (zip/node (wrap-left (wrap-left (wrap-left in-subtree)))) => 5))



;; head moves


(fact "a tuple with :head as its move leaves the cursor at the head of the scratch zipper"
  (zip/node (edit-with {:from :head :put :L :item 99} test-zipper)) => 1
  (zip/node (edit-with {:from :head :put :L :item 99} simple-zipper)) => :foo)



(fact ":head tuples"
  (zip/root (edit-with {:from :head :put :L :item 99} test-zipper)) => 
    '(99 1 2 3 (4 5 (6)))
  (zip/root (edit-with {:from :head :put :R :item 99} test-zipper)) => 
    '(1 99 2 3 (4 5 (6)))

  (zip/root (edit-with {:from :head :put :L :item 99} empty-zipper)) => 
    '(99)
  (zip/root (edit-with {:from :head :put :R :item 99} empty-zipper)) => 
    '(99)

  (zip/root (edit-with {:from :head :put :L :item 99} simple-zipper)) => 
    '(99 :foo :bar :baz)
  (zip/root (edit-with {:from :head :put :R :item 99} simple-zipper)) => 
    '(:foo 99 :bar :baz))



(fact ":head nil tuples"
  (zip/root (edit-with {:from :head :put :L :item nil} test-zipper)) => 
    '(1 2 3 (4 5 (6)))
  (zip/root (edit-with {:from :head :put :R} test-zipper)) => 
    '(1 2 3 (4 5 (6)))

  (zip/root (edit-with {:from :head :put :L :item nil} empty-zipper)) => 
    '()
  (zip/root (edit-with {:from :head :put :R} empty-zipper)) => 
    '()

  (zip/root (edit-with {:from :head :put :L :item nil} simple-zipper)) => 
    '(:foo :bar :baz)
  (zip/root (edit-with {:from :head :put :R} simple-zipper)) => 
    '(:foo :bar :baz))



;; tail moves


(fact "a tuple with :tail as its move leaves the cursor at the tail of the zipper"
  (zip/node (edit-with {:from :tail :put :L :item 99} test-zipper)) => 6
  (zip/node (edit-with {:from :tail :put :L :item 99} simple-zipper)) => :baz)



(fact ":tail tuples"
  (zip/root (edit-with {:from :tail :put :L :item 99} test-zipper)) => 
    '(1 2 3 (4 5 (99 6)))
  (zip/root (edit-with {:from :tail :put :R :item 99} test-zipper)) => 
    '(1 2 3 (4 5 (6 99)))

  (zip/root (edit-with {:from :tail :put :L :item 99} empty-zipper)) => 
    '(99)
  (zip/root (edit-with {:from :tail :put :R :item 99} empty-zipper)) => 
    '(99)

  (zip/root (edit-with {:from :tail :put :L :item 99} simple-zipper)) => 
    '(:foo :bar 99 :baz)
  (zip/root (edit-with {:from :tail :put :R :item 99} simple-zipper)) => 
    '(:foo :bar :baz 99))


(fact ":tail nil tuples"
  (zip/root (edit-with {:from :tail :put :L :item nil} test-zipper)) => 
    '(1 2 3 (4 5 (6)))
  (zip/root (edit-with {:from :tail :put :R} test-zipper)) => 
    '(1 2 3 (4 5 (6)))
  (zip/node (edit-with {:from :tail :put :R} test-zipper)) => 6

  (zip/root (edit-with {:from :tail :put :L :item nil} empty-zipper)) => 
    '()
  (zip/root (edit-with {:from :tail :put :R} empty-zipper)) => 
    '()
  (zip/node (edit-with {:from :tail :put :R} empty-zipper)) => '()  

  (zip/root (edit-with {:from :tail :put :L :item nil} simple-zipper)) => 
    '(:foo :bar :baz)
  (zip/root (edit-with {:from :tail :put :R} simple-zipper)) => 
    '(:foo :bar :baz)
  (zip/node (edit-with {:from :tail :put :R} simple-zipper)) => 
    :baz  )


;; subhead moves


(fact "a tuple with :subhead as its move leaves the cursor at the leftmost in its level"
  (let [in-subtree
    (-> test-zipper zip/next zip/next zip/next zip/next zip/next zip/next)]
    (zip/node in-subtree) => 5
    (zip/node (edit-with {:from :subhead :put :L :item 99} in-subtree)) => 4
    (zip/node (edit-with {:from :subhead :put :R :item 99} in-subtree)) => 4))


(fact ":subtree tuples"
  (let [in-subtree
    (-> test-zipper zip/next zip/next zip/next zip/next zip/next zip/next)]

    (zip/root (edit-with {:from :subhead :put :L :item 99} in-subtree)) => 
      '(1 2 3 (99 4 5 (6)))
    (zip/root (edit-with {:from :subhead :put :R :item 99} in-subtree)) => 
      '(1 2 3 (4 99 5 (6)))

    (zip/root (edit-with {:from :subhead :put :L :item 99} empty-zipper)) => 
      '(99)
    (zip/root (edit-with {:from :subhead :put :R :item 99} empty-zipper)) => 
      '(99)

    (zip/root (edit-with {:from :subhead :put :L :item 99} simple-zipper)) => 
      '(99 :foo :bar :baz)
    (zip/root (edit-with {:from :subhead :put :R :item 99} simple-zipper)) => 
      '(:foo 99 :bar :baz)))


(fact ":subhead nil tuples"
  (let [in-subtree
    (-> test-zipper zip/next zip/next zip/next zip/next zip/next zip/next)]

    (zip/root (edit-with {:from :subhead :put :L :item nil} in-subtree)) => 
      '(1 2 3 (4 5 (6)))
    (zip/root (edit-with {:from :subhead :put :R} in-subtree)) => 
      '(1 2 3 (4 5 (6)))
    (zip/node (edit-with {:from :subhead :put :R} in-subtree)) => 4

    (zip/root (edit-with {:from :subhead :put :L :item nil} empty-zipper)) => 
      '()
    (zip/root (edit-with {:from :subhead :put :R} empty-zipper)) => 
      '()
    (zip/node (edit-with {:from :subhead :put :R} empty-zipper)) => '()  

    (zip/root (edit-with {:from :subhead :put :L :item nil} simple-zipper)) => 
      '(:foo :bar :baz)
    (zip/root (edit-with {:from :subhead :put :R} simple-zipper)) => 
      '(:foo :bar :baz)
    (zip/node (edit-with {:from :subhead :put :R} simple-zipper)) => 
      :foo  ))


;; left moves


(fact "a tuple with :left as its move leaves the cursor in the right place"
  (let [in-subtree
    (-> test-zipper zip/next zip/next zip/next zip/next zip/next zip/next)]
    (zip/node in-subtree) => 5
    (zip/node (edit-with {:from :left :put :L :item 99} in-subtree)) => 4
    (zip/node (edit-with {:from :left :put :R :item 99} in-subtree)) => 4))


(fact ":left tuples"
  (let [in-subtree
    (-> test-zipper zip/next zip/next zip/next zip/next zip/next zip/next)]

    (zip/root (edit-with {:from :left :put :L :item 99} in-subtree)) => 
      '(1 2 3 (99 4 5 (6)))
    (zip/root (edit-with {:from :left :put :R :item 99} in-subtree)) => 
      '(1 2 3 (4 99 5 (6)))

    (zip/root (edit-with {:from :left :put :L :item 99} empty-zipper)) => 
      '(99)
    (zip/root (edit-with {:from :left :put :R :item 99} empty-zipper)) => 
      '(99)

    (zip/root (edit-with {:from :left :put :L :item 99} simple-zipper)) => 
      '(:foo :bar 99 :baz)
    (zip/root (edit-with {:from :left :put :R :item 99} simple-zipper)) => 
      '(:foo :bar :baz 99)

    (zip/root (edit-with {:from :left :put :L :item 99} stubby-zipper)) => 
      '(() () 99 (()))
    (zip/root (edit-with {:from :left :put :L :item 99}
      (zip/next (zip/next stubby-zipper)))) => 
      '((99) () (()))


      ))


(fact ":left nil tuples"
  (let [in-subtree
    (-> test-zipper zip/next zip/next zip/next zip/next zip/next zip/next)]

    (zip/root (edit-with {:from :left :put :L :item nil} in-subtree)) => 
      '(1 2 3 (4 5 (6)))
    (zip/root (edit-with {:from :left :put :R} in-subtree)) => 
      '(1 2 3 (4 5 (6)))
    (zip/node (edit-with {:from :left :put :R} in-subtree)) => 4

    (zip/root (edit-with {:from :left :put :L :item nil} empty-zipper)) => 
      '()
    (zip/root (edit-with {:from :left :put :R} empty-zipper)) => 
      '()
    (zip/node (edit-with {:from :left :put :R} empty-zipper)) => '()  

    (zip/root (edit-with {:from :left :put :L :item nil} simple-zipper)) => 
      '(:foo :bar :baz)
    (zip/root (edit-with {:from :left :put :R} simple-zipper)) => 
      '(:foo :bar :baz)
    (zip/node (edit-with {:from :left :put :R} simple-zipper)) => 
      :baz  ))






;; translating genomes



(fact "an empty genome produces an empty program"
  (zip->push [])=> [])

