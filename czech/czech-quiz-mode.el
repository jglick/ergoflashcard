(require 'quiz-mode)

(defcustom czech-quiz-include-vocative t
  "*Whether to include the vocative in declensions."
  :type 'boolean)

(defconst czech-quiz-cases-voc '(
				 "nom."
				 "gen."
				 "dat."
				 "acc."
				 "voc."
				 "loc."
				 "inst."
				 ))

(defconst czech-quiz-cases-no-voc '(
				    "nom."
				    "gen."
				    "dat."
				    "acc."
				    "loc."
				    "inst."
				    ))

;; XXX mixed (hard+soft, like moci) 3rd-class
(defun czech-quiz-insert-conjugation ()
  "Insert present-system verbal conjugation."
  (interactive)
  (let* ((cz (read-from-minibuffer "Czech verbal stem: "))
	 (cz+ (read-from-minibuffer "Czech added particles: "))
	 (en (read-from-minibuffer "English meaning (do not use `to'): "))
	 (classes '(("irregular" . 2) ("1st" . 3) ("2nd, -�" . 4) ("2nd, -ej�" . 5) ("3rd hard" . 6) ("3rd soft" . 7)))
	 (class (completing-read "Conjugation class: " classes nil t))
	 (classnum (or (cdr (assoc class classes)) (error))))
    (dolist (pers-num '(
			("I " "" "-" "�m" "�m" "�m" "u" "i")
			("you " "" "-" "�" "��" "��" "e�" "e�")
			("" "-s" "-" "�" "�" "�" "e" "e")
			("we " "" "-" "�me" "�me" "�me" "eme" "eme")
			("y'all " "" "-" "�te" "�te" "�te" "ete" "ete")
			("they " "" "-" "aj�" "�" "ej�" "ou" "�")
			))
      (insert cz (nth classnum pers-num)
	      (if (string-equal cz+ "") "" (concat " " cz+))
	      ":" (car pers-num) en (cadr pers-num) "\n"))))

(defun czech-quiz-insert-noun-declension ()
  "Insert seven-case noun declension."
  (interactive)
  (let* ((cz (read-from-minibuffer "Noun stem: "))
	 (en (read-from-minibuffer "English meaning: ")))
    (dolist (case (if czech-quiz-include-vocative
		      czech-quiz-cases-voc
		    czech-quiz-cases-no-voc))
      (insert cz "-:" en " (" case ")\n"))))

(defun czech-quiz-insert-noun-declension-with-plurals ()
  "Insert seven-case noun declension."
  (interactive)
  (let* ((cz (read-from-minibuffer "Noun stem: "))
	 (en1 (read-from-minibuffer "English meaning (sing.): "))
	 (en2 (read-from-minibuffer "English meaning (plur.): " en1)))
    (dolist (case (if czech-quiz-include-vocative
		      czech-quiz-cases-voc
		    czech-quiz-cases-no-voc))
      (insert cz "-:" en1 " (" case ")\n"))
    (dolist (case czech-quiz-cases-no-voc)
      (insert cz "-:" en2 " (" case ")\n"))))

;; XXX add number, ignore voc. if requested
(defun czech-quiz-insert-adjective-declension ()
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
      (insert cz "-:" en " (" case-gen ")\n"))))

(defun czech-quiz-insert-perfective-vocab ()
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
    (insert impf-inf cz+-append ":" en " (impf.)"
	    (if (string-equal impf-conj "")
		(if (string-equal usage "")
		    ""
		  (concat ":" usage))
	      (if (string-equal usage "")
		  (concat ":" impf-conj)
		(concat ":" impf-conj ", " usage)))
	    "\n"
	    perf-inf cz+-append ":" en " (perf.)"
	    (if (string-equal perf-conj "")
		(if (string-equal usage "")
		    ""
		  (concat ":" usage))
	      (if (string-equal usage "")
		  (concat ":" perf-conj)
		(concat ":" perf-conj ", " usage)))
	    "\n")))

(defun czech-quiz-insert-prefixed-perfective-vocab ()
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
	   (conj-append (if (string-equal conj "") "" (concat ":" conj)))
	   (impf-conj-append (string-regex-subst-all conj-append "\\*" ""))
	   (perf-conj-append (string-regex-subst-all conj-append "\\*" prefix))
	   (en (read-from-minibuffer "English meaning: " "to ")))
      (insert impf-inf ":" en " (impf.)" impf-conj-append "\n"
	      prefix impf-inf ":" en " (perf.)" perf-conj-append "\n"))))

(define-derived-mode czech-quiz-mode quiz-mode "Czech Quiz"
  "Major mode for editing Czech quiz data files.

\\{czech-quiz-mode-map}"
  (set (make-local-variable 'iso-language) "czech")
  (iso-accents-mode 1)
  (do ((char 128 (+ char 1)))
      ((= char 256) nil)
    (modify-syntax-entry char "w")))

(define-key czech-quiz-mode-map "\C-cv" 'czech-quiz-insert-conjugation)
(define-key czech-quiz-mode-map "\C-cn" 'czech-quiz-insert-noun-declension)
(define-key czech-quiz-mode-map "\C-cN" 'czech-quiz-insert-noun-declension-with-plurals)
(define-key czech-quiz-mode-map "\C-ca" 'czech-quiz-insert-adjective-declension)
(define-key czech-quiz-mode-map "\C-cp" 'czech-quiz-insert-perfective-vocab)
(define-key czech-quiz-mode-map "\C-cP" 'czech-quiz-insert-prefixed-perfective-vocab)

(provide 'czech-quiz-mode)
