package com.harshil.ragqa.service;

import com.harshil.ragqa.dto.Dtos;
import com.harshil.ragqa.vectorstore.FlatVectorIndex;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryService {

    private static final int TOP_K = 4;

    private final EmbeddingModel embeddingModel;
    private final ChatLanguageModel chatModel;
    private final FlatVectorIndex vectorIndex;

    public QueryService(EmbeddingModel embeddingModel, ChatLanguageModel chatModel, FlatVectorIndex vectorIndex) {
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
        this.vectorIndex = vectorIndex;
    }

    /**
     * The actual "RAG" in this project:
     *  1. Embed the question with the same model used for chunks (they
     *     must share embedding space).
     *  2. Retrieve the top-K most similar chunks from the vector index.
     *  3. Stuff those chunks into the prompt as grounding context, and ask
     *     the chat model to answer ONLY from that context — this is what
     *     keeps answers tied to the ingested documents instead of the
     *     model's general training data (and reduces hallucination).
     *
     * The <2s latency target on the resume bullet comes from: (a) a flat
     * in-memory index being fast at this scale (b), keeping TOP_K small
     * so the prompt stays short, and (c) using a small/fast chat model
     * (gpt-4o-mini) rather than a large one for the generation step.
     */
    public Dtos.QueryResponse answer(String question) {
        long start = System.currentTimeMillis();

        Response<dev.langchain4j.data.embedding.Embedding> questionEmbedding = embeddingModel.embed(question);
        List<FlatVectorIndex.SearchResult> topChunks =
                vectorIndex.search(questionEmbedding.content().vector(), TOP_K);

        String context = topChunks.stream()
                .map(FlatVectorIndex.SearchResult::text)
                .reduce("", (a, b) -> a + "\n---\n" + b);

        String prompt = """
                Answer the question using ONLY the context below. If the context
                doesn't contain the answer, say you don't have enough information —
                do not make something up.

                Context:
                %s

                Question: %s
                """.formatted(context, question);

        String answer = chatModel.generate(prompt);

        List<Dtos.SourceChunk> sources = topChunks.stream()
                .map(c -> new Dtos.SourceChunk(c.chunkId(), snippet(c.text()), c.score()))
                .toList();

        long latency = System.currentTimeMillis() - start;
        return new Dtos.QueryResponse(answer, sources, latency);
    }

    private String snippet(String text) {
        return text.length() > 150 ? text.substring(0, 150) + "..." : text;
    }
}
