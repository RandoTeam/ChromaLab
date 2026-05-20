package com.chromalab.feature.knowledge

import kotlin.math.ln

object KnowledgeRetrievalEngine {

    fun search(
        pack: KnowledgePackVersion = ChromaLabKnowledgeSeedV1.pack,
        query: KnowledgeSearchQuery,
        retrievalId: String = "knowledge:${query.rawQuery.hashCode()}",
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
        )
    }

    fun retrieveSnippets(
        pack: KnowledgePackVersion = ChromaLabKnowledgeSeedV1.pack,
        query: KnowledgeSearchQuery,
    ): List<KnowledgeGroundedSnippet> =
        KnowledgeUsePolicyValidator.snippetsFor(search(pack, query), pack)

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

    internal fun tokenize(text: String): List<String> =
        text.lowercase()
            .replace(Regex("""[^\p{L}\p{N}./+-]+"""), " ")
            .split(Regex("""\s+"""))
            .map { it.trim('.', ',', ':', ';', '(', ')', '[', ']') }
            .filter { it.length >= 2 }

    private fun normalize(text: String): String =
        tokenize(text).joinToString(" ")
}
