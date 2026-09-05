package com.mirror.agent.rag;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * BM25 管理器：按用户管理 BM25 索引实例（与 Go 版本一致）
 */
@Component
public class BM25Manager {

    private final Map<String, BM25Retriever> retrievers = new ConcurrentHashMap<>();

}
