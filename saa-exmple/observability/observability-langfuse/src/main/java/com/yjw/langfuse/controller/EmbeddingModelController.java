
package com.yjw.langfuse.controller;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/observability/embedding")
public class EmbeddingModelController {

	private final EmbeddingModel embeddingModel;

	public EmbeddingModelController(EmbeddingModel embeddingModel) {
		this.embeddingModel = embeddingModel;
	}

	@GetMapping
	public String embedding() {

		var embeddings = embeddingModel.embed("hello world.");
		return "embedding vector size:" + embeddings.length;
	}

}
