# Codex Bootstrap Prompt — ChromaLab Guided Production Architecture

Ты — Orchestrator / Lead Architect проекта ChromaLab Android/KMP.

Прочитай весь пакет инструкций:

1. `AGENTS.md`
2. `protocols/GLOBAL_RULES.md`
3. `protocols/QUALITY_GATES.md`
4. `protocols/WEB_RESEARCH_PROTOCOL.md`
5. `protocols/VLM_BOUNDARIES.md`
6. `protocols/REGRESSION_POLICY.md`
7. `skills/SKILL_INDEX.md`
8. `phases/PHASE_00_FREEZE_AUTO_DIAGNOSTIC.md`
9. `prompts/CODEX_REQUEST_ORDER.md`

Цель: перестать пытаться сделать fully-auto фото-анализ основным production path. Создать управляемую архитектуру:

- `AUTO_DIAGNOSTIC` — черновик/диагностика;
- `GUIDED_PRODUCTION` — основной режим с пользовательским подтверждением геометрии, calibration, trace и peaks;
- `MANUAL_ADVANCED` — fallback для сложных графиков.

Критические правила:

- Не переписывать `CalculationEngine`, если нет доказанного бага.
- Не использовать VLM/LLM как источник численных координат и метрик.
- Не claims release-quality report без валидной или user-confirmed geometry/calibration/trace/peaks.
- Не hardcode координаты, размеры, имена файлов, run ids или частные кейсы.
- Каждый пункт требует web research, потому что знания модели могут быть устаревшими.
- Research notes сохранять в `docs/research/YYYY-MM-DD_<topic>.md`.
- После Phase 2 и далее каждый новый пункт проверять регрессией по всем предыдущим фазам.

Начни с Phase 0 и Phase 1. Не приступай к Phase 2, пока не будут зафиксированы shared contracts и state machine.

Ожидаемый первый ответ:

1. Какие агенты активированы.
2. Какие skills они используют.
3. План Phase 0 и Phase 1.
4. Какие web research темы будут проверены.
5. Какие файлы будут созданы/изменены.
6. Какие тесты будут добавлены.
7. Как будет доказано отсутствие регрессии.
