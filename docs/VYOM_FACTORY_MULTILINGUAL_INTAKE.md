# Vyom Factory — Multilingual Natural-Language Intake

Status: requirements locked; implementation not yet started.

## Goal
A user should be able to describe a task naturally in their preferred language, without memorizing Factory command prefixes. Factory must understand the requested outcome and constraints, produce a reviewable structured task, and preserve the user's original wording.

## Initial language coverage
- Bengali / Bangla: standard written form, colloquial and regional variants, West Bengal and Bangladesh usage, Romanized Bangla, and Bangla-English code switching.
- Urdu: Urdu script (right-to-left), Roman Urdu, and Urdu-English code switching.
- Japanese: kana, kanji, and mixed Japanese/English technical vocabulary.
- Chinese: Simplified and Traditional Chinese, plus mixed Latin technical vocabulary.
- Hindi, Hinglish and English remain supported regression languages.

Language names are not sufficient as acceptance criteria: regional vocabulary, spelling variation, code-switching, script direction, punctuation, and transliteration must be tested.

## Required pipeline
1. Capture exact original Unicode text and source identity.
2. Detect language/script as a non-blocking hint; support mixed-language messages and uncertainty.
3. Interpret semantic intent and extract a typed task draft.
4. Validate the draft against a versioned schema.
5. Preserve all technical literals exactly: filenames, paths, symbols, commands, URLs, stack traces, quoted text and identifiers.
6. Ask a focused clarification when meaning, scope, or a consequential constraint is ambiguous.
7. Require confirmation for destructive, broad, costly, security-sensitive, or low-confidence work.
8. Register the original request and structured interpretation separately in the existing canonical task record.
9. Execution state remains evidence-based; interpretation or registration must never imply that a worker ran.

## Structured task draft
- original_text (immutable)
- detected_language and script (hints; may be mixed/unknown)
- normalized_intent
- title and objective
- constraints and explicit non-goals
- affected components/files (only when grounded in the request/repository inspection)
- priority plus rationale
- subtasks and dependencies
- measurable acceptance_criteria
- ambiguity/questions
- interpretation confidence and schema version

## Safety and quality rules
- No translated-prefix list as a substitute for understanding.
- No silent translation or normalization of technical literals.
- No guessing when the request is ambiguous; ask one focused question.
- Keep original text even if translation/interpretation fails.
- A failed interpreter must fall back to a safe unstructured draft and clarification, not silently discard or misclassify the task.
- Never mark a task running/completed based on successful interpretation or issue creation.

## Test matrix
| Language family | Required variants |
|---|---|
| Bengali | Standard Bangla, colloquial/regional (West Bengal and Bangladesh), Romanized Bangla, Bangla-English code switching |
| Urdu | Urdu script RTL, Roman Urdu, Urdu-English code switching, punctuation/digit direction |
| Japanese | Kana, kanji, polite/casual forms, Latin technical identifiers |
| Chinese | Simplified, Traditional, Latin technical identifiers, punctuation variants |
| Hindi/Hinglish | Devanagari Hindi, Roman Hindi, Hindi-English code switching |
| English | Plain requests, technical requests, multi-step requests |

Every fixture must verify intent, constraints, original text preservation, exact technical literals, subtasks, priority rationale, acceptance criteria, ambiguity behavior, and safe failure. Script detection alone is not a passing test.

## Scope constraints
Implementation is Android-only. Do not change Render, Hermes backend, production Supabase, or main. Inspect existing API/model routing before selecting an implementation strategy. If no supported structured interpretation path exists, expose that gap rather than claiming multilingual semantic support.

## Current baseline (verified by source inspection, 2026-11-04)
MainActivity.extractFactoryComplaint() accepts only six English prefixes (factory complaint:, register complaint:, complaint:, report bug:, factory:, issue:). The chat path assigns fixed P2. There is no language detection or structured semantic task interpretation in this intake path.
