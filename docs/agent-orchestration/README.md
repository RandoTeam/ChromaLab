# ChromaLab Agent Orchestration Pack

Версия: 2026-05-20

Назначение: пакет инструкций для Codex/оркестратора и субагентов, чтобы перевести ChromaLab из нестабильного fully-auto pipeline в production-grade мобильное приложение с режимами:

1. `AUTO_DIAGNOSTIC` — автоматическая попытка, только diagnostic/review без доказанной геометрии.
2. `GUIDED_PRODUCTION` — основной точный режим с подтверждением пользователем graphPanel, plotArea, calibration, trace, peaks.
3. `MANUAL_ADVANCED` — fallback для сложных фото/скриншотов.

Ключевой принцип: VLM/Gemma/Qwen используются как OCR/semantic/judge assistant, но не как измерительный прибор. Все численные метрики строятся только через проверяемую геометрию, calibration, trace и CalculationEngine.

## Как использовать

1. Передайте Codex файл `CODEX_BOOTSTRAP_PROMPT.md`.
2. Попросите Codex прочитать `AGENTS.md`, `protocols/GLOBAL_RULES.md`, `protocols/QUALITY_GATES.md` и `phases/PHASE_00_FREEZE_AUTO_DIAGNOSTIC.md`.
3. Запускайте фазы строго по порядку из `prompts/CODEX_REQUEST_ORDER.md`.
4. После Phase 2 и каждой следующей фазы прогоняйте регрессию всех предыдущих фаз.
5. Не закрывайте фазу без research notes, тестов, evidence package и phase closeout.

## Структура

- `AGENTS.md` — роли агентов и правила взаимодействия.
- `CODEX_BOOTSTRAP_PROMPT.md` — первый запрос в Codex.
- `agents/` — карточки агентов.
- `skills/` — skill cards, которые агенты выбирают по задаче.
- `phases/` — фазы реализации.
- `protocols/` — глобальные правила, quality gates, VLM boundaries, evidence spec.
- `prompts/` — готовые запросы к Codex по порядку.
- `templates/` — шаблоны отчётов, research notes, closeout, validation.
- `config/` — YAML/JSON index для автоматического ingestion.
