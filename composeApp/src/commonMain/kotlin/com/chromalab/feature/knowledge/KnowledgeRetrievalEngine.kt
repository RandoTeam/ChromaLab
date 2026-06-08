package com.chromalab.feature.knowledge

object KnowledgeRetrievalEngine {

    fun search(
        pack: KnowledgePackVersion = ChromaLabKnowledgeSeedV1.pack,
        query: KnowledgeSearchQuery,
        retrievalId: String = "knowledge:${query.rawQuery.hashCode()}",
        backend: KnowledgeRetrievalBackend = LexicalKnowledgeRetrievalBackend,
    ): KnowledgeRetrievalContext =
        backend.search(
            pack = pack,
            query = query,
            retrievalId = retrievalId,
        )

    fun retrieveSnippets(
        pack: KnowledgePackVersion = ChromaLabKnowledgeSeedV1.pack,
        query: KnowledgeSearchQuery,
        backend: KnowledgeRetrievalBackend = LexicalKnowledgeRetrievalBackend,
    ): List<KnowledgeGroundedSnippet> =
        KnowledgeUsePolicyValidator.snippetsFor(search(pack, query, backend = backend), pack)

    internal fun tokenize(text: String): List<String> =
        LexicalKnowledgeRetrievalBackend.tokenize(text)
}
