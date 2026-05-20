package com.chromalab.feature.knowledge

import kotlinx.serialization.Serializable

@Serializable
enum class KnowledgeTextRuleClass {
    TITLE_OR_CHANNEL,
    ION_CHANNEL_TERM,
    METHOD_METADATA,
    PEAK_ANNOTATION_CANDIDATE,
    TICK_LABEL_CANDIDATE,
    AXIS_LABEL,
    NOT_PEAK_ANNOTATION,
    UNKNOWN,
}

@Serializable
data class KnowledgeRuleResult(
    val ruleId: String,
    val textClass: KnowledgeTextRuleClass,
    val matchedEntryIds: List<String>,
    val confidence: Float,
    val caveats: List<String> = emptyList(),
)

object KnowledgeRuleEngine {
    private val ionRangePattern = Regex("""(?i)\b(?:ion|xic|eic)\s+[0-9]+(?:[.,][0-9]+)?\s*\([^)]*\bto\b[^)]*\)""")
    private val mzPattern = Regex("""(?i)\bm/z\s*[0-9]+(?:[.,][0-9]+)?\b""")
    private val simPattern = Regex("""(?i)\bSIM\s+[0-9]+(?:[.,][0-9]+)?\b""")
    private val numericPattern = Regex("""^\s*[0-9]+(?:[.,][0-9]+)?\s*$""")
    private val titleHints = Regex("""(?i)\b(?:title|header|ion|channel|method|scan|sim|xic|eic|tic)\b""")

    fun classifyText(
        text: String,
        surroundingText: String = text,
        isInTitleOrHeader: Boolean = false,
        isNearApex: Boolean = false,
        hasLocalSignalVerification: Boolean = false,
        hasLinkedTickPosition: Boolean = false,
    ): KnowledgeRuleResult {
        val context = surroundingText.ifBlank { text }
        return when {
            ionRangePattern.containsMatchIn(context) -> result(
                ruleId = "kp2-rule-ion-title-not-peak",
                textClass = KnowledgeTextRuleClass.TITLE_OR_CHANNEL,
                confidence = 0.99f,
                caveat = "Ion/mass range numbers are channel metadata, not retention-time peak labels.",
            )
            simPattern.containsMatchIn(context) -> result(
                ruleId = "kp2-ms-sim",
                textClass = KnowledgeTextRuleClass.METHOD_METADATA,
                confidence = 0.96f,
                caveat = "SIM labels describe acquisition/channel metadata.",
            )
            mzPattern.containsMatchIn(context) -> result(
                ruleId = "kp2-ms-mz",
                textClass = KnowledgeTextRuleClass.ION_CHANNEL_TERM,
                confidence = 0.96f,
                caveat = "m/z values are ion/channel terms, not retention-time values.",
            )
            isInTitleOrHeader || titleHints.containsMatchIn(context) && numericPattern.matches(text) -> result(
                ruleId = "kp2-rule-title-header-not-peak",
                textClass = KnowledgeTextRuleClass.NOT_PEAK_ANNOTATION,
                confidence = 0.9f,
                caveat = "Title/header numeric text cannot become a peak label without plot/signal evidence.",
            )
            hasLinkedTickPosition && numericPattern.matches(text) -> result(
                ruleId = "kp2-rule-tick-label-geometry",
                textClass = KnowledgeTextRuleClass.TICK_LABEL_CANDIDATE,
                confidence = 0.9f,
            )
            isNearApex && hasLocalSignalVerification && numericPattern.matches(text) -> result(
                ruleId = "kp2-rule-peak-annotation-signal-verified",
                textClass = KnowledgeTextRuleClass.PEAK_ANNOTATION_CANDIDATE,
                confidence = 0.86f,
                caveat = "Peak annotation candidate still requires deterministic local signal verification before use.",
            )
            context.contains("abundance", ignoreCase = true) || context.contains("retention", ignoreCase = true) -> result(
                ruleId = "kp2-axis-axis-label",
                textClass = KnowledgeTextRuleClass.AXIS_LABEL,
                confidence = 0.82f,
            )
            else -> KnowledgeRuleResult(
                ruleId = "knowledge-rule-unknown",
                textClass = KnowledgeTextRuleClass.UNKNOWN,
                matchedEntryIds = emptyList(),
                confidence = 0f,
            )
        }
    }

    fun forbiddenUseIssues(entry: KnowledgeEntry, attemptedUses: List<String>): List<KnowledgeUseValidationIssue> =
        attemptedUses
            .filter { attempted -> attempted in entry.policy.forbiddenUse }
            .map { attempted ->
                KnowledgeUseValidationIssue(
                    code = "knowledge.rule.forbidden_use",
                    message = "Entry '${entry.entryId}' cannot be used for '$attempted'.",
                )
            }

    fun caveatsForReleaseGate(missingGateCodes: List<String>): List<KnowledgeRuleResult> =
        missingGateCodes.map { code ->
            when (code) {
                "calibration" -> result(
                    ruleId = "kp2-caveat-calibration-required",
                    textClass = KnowledgeTextRuleClass.NOT_PEAK_ANNOTATION,
                    confidence = 1f,
                    caveat = "Release metrics require valid calibration evidence.",
                )
                "trace" -> result(
                    ruleId = "kp2-caveat-trace-required",
                    textClass = KnowledgeTextRuleClass.NOT_PEAK_ANNOTATION,
                    confidence = 1f,
                    caveat = "Peak metrics require linked trace evidence.",
                )
                "kovats" -> result(
                    ruleId = "kp2-caveat-no-kovats-without-reference",
                    textClass = KnowledgeTextRuleClass.NOT_PEAK_ANNOTATION,
                    confidence = 1f,
                    caveat = "Kovats/RI requires a valid same-method reference series.",
                )
                else -> result(
                    ruleId = "kp2-snippet-diagnostic-only",
                    textClass = KnowledgeTextRuleClass.NOT_PEAK_ANNOTATION,
                    confidence = 0.8f,
                    caveat = "Missing evidence keeps the report diagnostic/review-grade.",
                )
            }
        }

    private fun result(
        ruleId: String,
        textClass: KnowledgeTextRuleClass,
        confidence: Float,
        caveat: String? = null,
    ): KnowledgeRuleResult =
        KnowledgeRuleResult(
            ruleId = ruleId,
            textClass = textClass,
            matchedEntryIds = listOf(ruleId),
            confidence = confidence,
            caveats = listOfNotNull(caveat),
        )
}
