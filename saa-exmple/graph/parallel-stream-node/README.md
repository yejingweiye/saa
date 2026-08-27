parallel-stream-node 就是"并行节点 + 流式输出"的 demo（README 里写的"注重流式处理"）。拓扑是 START → expander/translate（并行）→ merge → END，两个节点同时跑，各自把 Flux<ChatResponse> 塞进
state，框架（NodeExecutor.getEmbedGraphFlux）会订阅这个 Flux 并逐 chunk 转成 StreamingOutput 事件，再由 GraphProcess 推送成 SSE。

