(require 'quiz-mode)

(define-derived-mode dutch-quiz-mode quiz-mode "Dutch Quiz"
  "Major mode for editing Dutch quiz data files."
  (set (make-local-variable 'iso-language) "dutch")
  (iso-accents-mode 1)
  (do ((char 128 (+ char 1)))
      ((= char 256) nil)
    (modify-syntax-entry char "w")))

(provide 'dutch-quiz-mode)
