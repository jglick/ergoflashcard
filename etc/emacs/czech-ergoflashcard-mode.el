;;; Copyright 2004 Jesse N. Glick
;;;
;;; Licensed under the Apache License, Version 2.0 (the "License");
;;; you may not use this file except in compliance with the License.
;;; You may obtain a copy of the License at
;;;
;;;     http://www.apache.org/licenses/LICENSE-2.0
;;;
;;; Unless required by applicable law or agreed to in writing, software
;;; distributed under the License is distributed on an "AS IS" BASIS,
;;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;;; See the License for the specific language governing permissions and
;;; limitations under the License.

(require 'ergoflashcard-mode)

(defcustom czech-ergoflashcard-include-vocative t
  "*Whether to include the vocative in declensions."
  :type 'boolean)

(defconst czech-ergoflashcard-cases-voc '(
				 "nom."
				 "gen."
				 "dat."
				 "acc."
				 "voc."
				 "loc."
				 "inst."
				 ))

(defconst czech-ergoflashcard-cases-no-voc '(
				    "nom."
				    "gen."
				    "dat."
				    "acc."
				    "loc."
				    "inst."
				    ))

;; XXX mixed (hard+soft, like moci) 3rd-class
(defun czech-ergoflashcard-insert-conjugation ()
  "Insert present-system verbal conjugation."
  (interactive)
  (let* ((cz (read-from-minibuffer "Czech verbal stem: "))
	 (cz+ (read-from-minibuffer "Czech added particles: "))
	 (en (read-from-minibuffer "English meaning (do not use `to'): "))
	 (classes '(("irregular" . 2) ("1st" . 3) ("2nd, -í" . 4) ("2nd, -ejí" . 5) ("3rd hard" . 6) ("3rd soft" . 7)))
	 (class (completing-read "Conjugation class: " classes nil t))
	 (classnum (or (cdr (assoc class classes)) (error))))
    (dolist (pers-num '(
			("I " "" "-" "ám" "ím" "ím" "u" "i")
			("you " "" "-" "á¹" "í¹" "í¹" "e¹" "e¹")
			("" "-s" "-" "á" "í" "í" "e" "e")
			("we " "" "-" "áme" "íme" "íme" "eme" "eme")
			("y'all " "" "-" "áte" "íte" "íte" "ete" "ete")
			("they " "" "-" "ají" "í" "ejí" "ou" "í")
			))
      (insert cz (nth classnum pers-num)
	      (if (string-equal cz+ "") "" (concat " " cz+))
	      " - " (car pers-num) en (cadr pers-num) "\n"))))

(defun czech-ergoflashcard-insert-noun-declension ()
  "Insert seven-case noun declension."
  (interactive)
  (let* ((cz (read-from-minibuffer "Noun stem: "))
	 (en (read-from-minibuffer "English meaning: ")))
    (dolist (case (if czech-ergoflashcard-include-vocative
		      czech-ergoflashcard-cases-voc
		    czech-ergoflashcard-cases-no-voc))
      (insert cz "- - " en " (" case ")\n"))))

(defun czech-ergoflashcard-insert-noun-declension-with-plurals ()
  "Insert seven-case noun declension."
  (interactive)
  (let* ((cz (read-from-minibuffer "Noun stem: "))
	 (en1 (read-from-minibuffer "English meaning (sing.): "))
	 (en2 (read-from-minibuffer "English meaning (plur.): " en1)))
    (dolist (case (if czech-ergoflashcard-include-vocative
		      czech-ergoflashcard-cases-voc
		    czech-ergoflashcard-cases-no-voc))
      (insert cz "- - " en1 " (" case ")\n"))
    (dolist (case czech-ergoflashcard-cases-no-voc)
      (insert cz "- - " en2 " (" case ")\n"))))

;; XXX add number, ignore voc. if requested
(defun czech-ergoflashcard-insert-adjective-declension ()
  "Insert declension of adjective (say) in various cases and genders."
  (interactive)
  (let* ((cz (read-from-minibuffer "Adjective stem: "))
	 (en (read-from-minibuffer "English meaning: ")))
    (dolist (case-gen '(
			"masc. nom., voc."
			"masc. gen."
			"masc. dat."
			"inan. acc."
			"anim. acc."
			"masc. loc."
			"masc. inst."
			"fem. nom., voc."
			"fem. gen."
			"fem. dat."
			"fem. acc."
			"fem. loc."
			"fem. inst."
			"neut. nom., voc."
			"neut. gen."
			"neut. dat."
			"neut. acc."
			"neut. loc."
			"neut. inst."
			))
      (insert cz "- - " en " (" case-gen ")\n"))))

(defun czech-ergoflashcard-insert-perfective-vocab ()
  "Insert a pair of imperfect + perfect verb definitions."
  (interactive)
  (let* ((impf-inf (read-from-minibuffer "Imperfective infinitive: "))
	 (impf-conj (read-from-minibuffer "Imperfective conjugation notes: "))
	 (perf-inf (read-from-minibuffer "Perfective infinitive: "))
	 (perf-conj (read-from-minibuffer "Perfective conjugation notes: "))
	 (cz+ (read-from-minibuffer "Czech added particles: "))
	 (cz+-append (if (string-equal cz+ "") "" (concat " " cz+)))
	 (en (read-from-minibuffer "English meaning: " "to "))
	 (usage (read-from-minibuffer "Usage notes: "))
	 (usage-append (if (string-equal usage "") "" (concat ", " usage))))
    (insert impf-inf cz+-append " - " en " (impf.)"
	    (if (string-equal impf-conj "")
		(if (string-equal usage "")
		    ""
		  (concat " - " usage))
	      (if (string-equal usage "")
		  (concat " - " impf-conj)
		(concat " - " impf-conj ", " usage)))
	    "\n"
	    perf-inf cz+-append " - " en " (perf.)"
	    (if (string-equal perf-conj "")
		(if (string-equal usage "")
		    ""
		  (concat " - " usage))
	      (if (string-equal usage "")
		  (concat " - " perf-conj)
		(concat " - " perf-conj ", " usage)))
	    "\n")))

(defun czech-ergoflashcard-insert-prefixed-perfective-vocab ()
  "Insert a verb pair using prefixation for the perfective."
  (interactive)
  ;; Ich. Is there no library function to do this?
  (flet ((string-regex-subst-all
	  (string from to)
	  (do ((idx 0))
	      ((not (string-match from string idx)) string)
	    (let ((oldlen (length string)))
	      (setq string (replace-match to nil nil string))
	      (setq idx (+ (match-end 0) (length string) (- oldlen)))))))
    (let* ((impf-inf (read-from-minibuffer "Imperfective infinitive (+ particles): "))
	   (prefix (read-from-minibuffer "Perfective prefix: "))
	   (conj (read-from-minibuffer "Conjugation & usage notes (incl. `*' for prefix): "))
	   (conj-append (if (string-equal conj "") "" (concat " - " conj)))
	   (impf-conj-append (string-regex-subst-all conj-append "\\*" ""))
	   (perf-conj-append (string-regex-subst-all conj-append "\\*" prefix))
	   (en (read-from-minibuffer "English meaning: " "to ")))
      (insert impf-inf " - " en " (impf.)" impf-conj-append "\n"
	      prefix impf-inf " - " en " (perf.)" perf-conj-append "\n"))))

(define-derived-mode czech-ergoflashcard-mode ergoflashcard-mode "Czech ErgoFlashCard"
  "Major mode for editing Czech ergoflashcard data files.

\\{czech-ergoflashcard-mode-map}"
  (set (make-local-variable 'iso-language) "czech")
  (iso-accents-mode 1)
  ;; XXX revise the following for UTF-8:
  (do ((char 128 (+ char 1)))
      ((= char 256) nil)
    (modify-syntax-entry char "w")))

(define-key czech-ergoflashcard-mode-map "\C-cv" 'czech-ergoflashcard-insert-conjugation)
(define-key czech-ergoflashcard-mode-map "\C-cn" 'czech-ergoflashcard-insert-noun-declension)
(define-key czech-ergoflashcard-mode-map "\C-cN" 'czech-ergoflashcard-insert-noun-declension-with-plurals)
(define-key czech-ergoflashcard-mode-map "\C-ca" 'czech-ergoflashcard-insert-adjective-declension)
(define-key czech-ergoflashcard-mode-map "\C-cp" 'czech-ergoflashcard-insert-perfective-vocab)
(define-key czech-ergoflashcard-mode-map "\C-cP" 'czech-ergoflashcard-insert-prefixed-perfective-vocab)

(provide 'czech-ergoflashcard-mode)
