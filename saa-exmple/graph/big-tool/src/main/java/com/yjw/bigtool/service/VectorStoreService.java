

package com.yjw.bigtool.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorStoreService {

	private final EmbeddingModel embeddingModel;

	private final VectorStore vectorStore;

	public VectorStoreService(EmbeddingModel embeddingModel) {
		this.embeddingModel = embeddingModel;
		this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
	}

	public void addDocuments(List<Document> documents) {
		vectorStore.add(documents);
	}

	public List<Document> search(String query, int topK) {
		return vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(topK).build());
	}

}
