package com.chromalab.feature.knowledge

import kotlin.math.ln

interface KnowledgeRetrievalBackend {
    val backendId: KnowledgeRetrievalBackendId

    fun search(
        pack: KnowledgePackVersion,
        query: KnowledgeSearchQuery,
        retrievalId: String,
    ): KnowledgeRetrievalContext
}

object LexicalKnowledgeRetrievalBackend : KnowledgeRetrievalBackend {
    override val backendId: KnowledgeRetrievalBackendId = KnowledgeRetrievalBackendId.LEXICAL_BM25

    override fun search(
        pack: KnowledgePackVersion,
        query: KnowledgeSearchQuery,
        retrievalId: String,
    ): KnowledgeRetrievalContext {
        val requested = query.requestedTypes.toSet()
        val requestedLanguages = query.languages.map { it.lowercase() }.toSet()
        val requestedUses = query.allowedUses.toSet()
        val sourceRefs = pack.sourceRefs.associateBy { it.sourceId }
        val normalizedQuery = normalize(query.rawQuery)
        val candidates = pack.entries.filter { entry ->
            (requested.isEmpty() || entry.type in requested) &&
                (requestedLanguages.isEmpty() || entry.language.lowercase() in requestedLanguages) &&
                (requestedUses.isEmpty() || requestedUses.any { it in entry.policy.allowedUse }) &&
                (!query.exactAliasOnly || entry.aliasesWithCanonical().any { normalize(it) == normalizedQuery })
        }
        val queryTerms = tokenize(query.rawQuery).toSet()
        val documentTerms = candidates.associateWith { tokenize(it.searchableText()) }
        val documentFrequency = queryTerms.associateWith { term ->
            documentTerms.values.count { terms -> term in terms }.coerceAtLeast(1)
        }
        val averageLength = documentTerms.values.map { it.size }.average().takeIf { it.isFinite() && it > 0.0 } ?: 1.0

        val results = candidates.mapNotNull { entry ->
            val terms = documentTerms.getValue(entry)
            val matched = queryTerms.filter { it in terms }.distinct()
            val exactAlias = entry.aliasesWithCanonical().any { normalize(it) == normalizedQuery }
            if (matched.isEmpty() && !exactAlias) {
                null
            } else {
                val baseScore = bm25Score(
                    queryTerms = matched.ifEmpty { queryTerms.toList() },
                    documentTerms = terms,
                    documentFrequency = documentFrequency,
                    documentCount = candidates.size.coerceAtLeast(1),
                    averageLength = averageLength,
                )
                KnowledgeSearchResult(
                    entryId = entry.entryId,
                    entry = entry,
                    score = if (exactAlias) baseScore + 100.0 else baseScore,
                    matchedTerms = matched,
                    sourceRefs = entry.sourceRefIds.mapNotNull(sourceRefs::get),
                    backendId = backendId,
                    indexVersion = INDEX_VERSION,
                    retrievalStage = "lexical_rank",
                    safetyStatus = KnowledgeRetrievalSafetyStatus.POLICY_READY,
                )
            }
        }.sortedWith(
            compareByDescending<KnowledgeSearchResult> { it.score }
                .thenBy { it.entry.entryId },
        ).take(query.maxResults.coerceAtLeast(1))

        return KnowledgeRetrievalContext(
            retrievalId = retrievalId,
            knowledgePackVersion = pack.version,
            query = query,
            results = results,
            backendId = backendId,
            diagnostics = KnowledgeRetrievalDiagnostics(
                backendId = backendId,
                indexVersion = INDEX_VERSION,
                retrievalStage = "lexical_rank",
                safetyStatus = KnowledgeRetrievalSafetyStatus.POLICY_READY,
                notes = listOf("Active local lexical retrieval owner; no dense vector dependency."),
            ),
        )
    }

    internal fun tokenize(text: String): List<String> =
        text.lowercase()
            .replace(Regex("""[^\p{L}\p{N}./+-]+"""), " ")
            .split(Regex("""\s+"""))
            .map { it.trim('.', ',', ':', ';', '(', ')', '[', ']') }
            .filter { it.length >= 2 }

    private fun KnowledgeEntry.searchableText(): String =
        buildString {
            append(canonicalLabel)
            append(' ')
            append(shortText)
            append(' ')
            append(longText.orEmpty())
            append(' ')
            append(type.name)
            append(' ')
            append(aliases.joinToString(" "))
            append(' ')
            append(keywords.joinToString(" "))
            append(' ')
            append(tags.joinToString(" "))
        }

    private fun KnowledgeEntry.aliasesWithCanonical(): List<String> =
        aliases + canonicalLabel + entryId

    private fun bm25Score(
        queryTerms: List<String>,
        documentTerms: List<String>,
        documentFrequency: Map<String, Int>,
        documentCount: Int,
        averageLength: Double,
    ): Double {
        val k1 = 1.2
        val b = 0.75
        val length = documentTerms.size.coerceAtLeast(1).toDouble()
        val termCounts = documentTerms.groupingBy { it }.eachCount()
        return queryTerms.sumOf { term ->
            val tf = termCounts[term]?.toDouble() ?: 0.0
            val df = documentFrequency[term]?.toDouble() ?: 1.0
            val idf = ln(1.0 + (documentCount - df + 0.5) / (df + 0.5))
            idf * ((tf * (k1 + 1.0)) / (tf + k1 * (1.0 - b + b * length / averageLength)))
        }
    }

    private fun normalize(text: String): String =
        tokenize(text).joinToString(" ")

    private const val INDEX_VERSION = "lexical-bm25:${CHROMALAB_KNOWLEDGE_PACK_SCHEMA_VERSION}"
}

object TurboVecKnowledgeRetrievalBackend : KnowledgeRetrievalBackend {
    override val backendId: KnowledgeRetrievalBackendId = KnowledgeRetrievalBackendId.TURBOVEC_DENSE_SHADOW

    override fun search(
        pack: KnowledgePackVersion,
        query: KnowledgeSearchQuery,
        retrievalId: String,
    ): KnowledgeRetrievalContext =
        KnowledgeRetrievalContext(
            retrievalId = retrievalId,
            knowledgePackVersion = pack.version,
            query = query,
            results = emptyList(),
            backendId = backendId,
            diagnostics = KnowledgeRetrievalDiagnostics(
                backendId = backendId,
                indexVersion = "turbovec:unbuilt",
                retrievalStage = "dense_shadow_unavailable",
                safetyStatus = KnowledgeRetrievalSafetyStatus.SHADOW_UNAVAILABLE,
                notes = listOf(
                    "TurboVec is not an active runtime dependency.",
                    "Dense retrieval must pass local benchmark, citation, and safety gates before promotion.",
                ),
            ),
        )
}

object HybridUnionRrfKnowledgeRetrievalBackend : KnowledgeRetrievalBackend {
    override val backendId: KnowledgeRetrievalBackendId = KnowledgeRetrievalBackendId.HYBRID_UNION_RRF_CANDIDATE

    override fun search(
        pack: KnowledgePackVersion,
        query: KnowledgeSearchQuery,
        retrievalId: String,
    ): KnowledgeRetrievalContext {
        val lexicalContext = LexicalKnowledgeRetrievalBackend.search(
            pack = pack,
            query = query,
            retrievalId = "$retrievalId:lexical",
        )
        return HybridUnionRrfKnowledgeRetrievalPolicy.arbitrate(
            pack = pack,
            query = query,
            retrievalId = retrievalId,
            lexicalContext = lexicalContext,
            denseContexts = emptyList(),
        )
    }
}

object HybridUnionRrfKnowledgeRetrievalPolicy {
    private const val INDEX_VERSION = "hybrid-union-rrf-candidate:${CHROMALAB_KNOWLEDGE_PACK_SCHEMA_VERSION}"
    private const val RRF_K = 60.0

    fun arbitrate(
        pack: KnowledgePackVersion,
        query: KnowledgeSearchQuery,
        retrievalId: String,
        lexicalContext: KnowledgeRetrievalContext,
        denseContexts: List<KnowledgeRetrievalContext>,
    ): KnowledgeRetrievalContext {
        val maxResults = query.maxResults.coerceAtLeast(1)
        val sourceRefs = pack.sourceRefs.associateBy { it.sourceId }
        val entriesById = pack.entries.associateBy { it.entryId }
        val allContexts = listOf(lexicalContext) + denseContexts
        val scores = linkedMapOf<String, Double>()
        val matchedTerms = mutableMapOf<String, List<String>>()
        val sourceBackendIds = mutableMapOf<String, MutableSet<KnowledgeRetrievalBackendId>>()
        allContexts.forEach { context ->
            context.results.forEachIndexed { index, result ->
                if (result.entryId in entriesById) {
                    scores[result.entryId] = (scores[result.entryId] ?: 0.0) + 1.0 / (RRF_K + index + 1)
                    matchedTerms.putIfAbsent(result.entryId, result.matchedTerms)
                    sourceBackendIds.getOrPut(result.entryId) { linkedSetOf() }.add(result.backendId)
                }
            }
        }
        val orderedIds = scores.keys.sortedWith(
            compareByDescending<String> { scores.getValue(it) }
                .thenBy { it },
        ).toMutableList()
        if (shouldPinLexicalTop1(query)) {
            lexicalContext.results.firstOrNull()?.entryId?.let { pinned ->
                if (pinned in orderedIds) {
                    orderedIds.remove(pinned)
                    orderedIds.add(0, pinned)
                }
            }
        }
        val results = orderedIds.take(maxResults).mapNotNull { entryId ->
            val entry = entriesById[entryId] ?: return@mapNotNull null
            KnowledgeSearchResult(
                entryId = entryId,
                entry = entry,
                score = scores.getValue(entryId),
                matchedTerms = matchedTerms[entryId].orEmpty(),
                sourceRefs = entry.sourceRefIds.mapNotNull(sourceRefs::get),
                backendId = KnowledgeRetrievalBackendId.HYBRID_UNION_RRF_CANDIDATE,
                indexVersion = INDEX_VERSION,
                retrievalStage = "hybrid_union_rrf",
                safetyStatus = KnowledgeRetrievalSafetyStatus.POLICY_READY,
            )
        }
        val denseBackendIds = denseContexts.map { it.backendId }.distinct()
        return KnowledgeRetrievalContext(
            retrievalId = retrievalId,
            knowledgePackVersion = pack.version,
            query = query,
            results = results,
            backendId = KnowledgeRetrievalBackendId.HYBRID_UNION_RRF_CANDIDATE,
            diagnostics = KnowledgeRetrievalDiagnostics(
                backendId = KnowledgeRetrievalBackendId.HYBRID_UNION_RRF_CANDIDATE,
                indexVersion = INDEX_VERSION,
                retrievalStage = "hybrid_union_rrf",
                safetyStatus = KnowledgeRetrievalSafetyStatus.POLICY_READY,
                denseIndexManifest = denseContexts.firstNotNullOfOrNull { it.diagnostics.denseIndexManifest },
                notes = buildList {
                    add("TV-4 candidate policy: reciprocal-rank fusion over lexical and optional dense contexts.")
                    add("Lexical retrieval remains the default active owner.")
                    if (denseContexts.isEmpty()) {
                        add("No dense runtime context was supplied; result is lexical-compatible and disableable.")
                    } else {
                        add("Dense backend ids: ${denseBackendIds.joinToString()}.")
                    }
                    if (shouldPinLexicalTop1(query)) {
                        add("Safety-critical exact-rule query: lexical top-1 pinned before final truncation.")
                    }
                    val sourceIds = sourceBackendIds.mapValues { (_, ids) -> ids.joinToString() }
                    add("Source backend ids by entry: $sourceIds.")
                },
            ),
        )
    }

    private fun shouldPinLexicalTop1(query: KnowledgeSearchQuery): Boolean =
        query.arbitrationHint.safetyCritical &&
            query.arbitrationHint.queryClass == KnowledgeRetrievalQueryClass.EXACT_RULE
}
