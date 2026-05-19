# Global Rules

## Product modes

- `AUTO_DIAGNOSTIC`: автоматическая попытка, не production по умолчанию.
- `GUIDED_PRODUCTION`: основной точный режим.
- `MANUAL_ADVANCED`: fallback для тяжёлых изображений.

## Non-negotiable rules

1. Не переписывать `CalculationEngine` без доказанного, изолированного бага.
2. Не использовать VLM/LLM как numeric geometry source.
3. Не использовать VLM/LLM для RT, height, area, FWHM, S/N, baseline, Kovats.
4. Не claims release-quality report без валидной или user-confirmed evidence.
5. Не hardcode координаты, размеры, filenames, run ids, fixture-specific branches.
6. Каждый terminal state экспортирует RuntimeEvidencePackage.
7. Каждый phase требует web research, потому что знания модели могут быть устаревшими.
8. После Phase 2 и далее каждый новый phase должен прогонять все предыдущие acceptance tests.
9. Если новый пункт ломает старый — phase не закрыт.
10. Полированный UI не должен скрывать invalid geometry/calibration.
