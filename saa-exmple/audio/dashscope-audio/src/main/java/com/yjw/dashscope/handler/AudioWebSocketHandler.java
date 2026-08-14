package com.yjw.dashscope.handler;

import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioTranscriptionApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 音频转录处理器
 * 接收前端音频数据，调用 DashScope 实时转录 API，返回转录结果
 */
@Component
public class AudioWebSocketHandler extends BinaryWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AudioWebSocketHandler.class);

    private final DashScopeAudioTranscriptionApi dashScopeAudioTranscriptionApi;
    private final ObjectMapper objectMapper;
    private final Map<String, Sinks.Many<DataBuffer>> audioSinks = new ConcurrentHashMap<>();

    public AudioWebSocketHandler(DashScopeAudioTranscriptionApi dashScopeAudioTranscriptionApi) {
        this.dashScopeAudioTranscriptionApi = dashScopeAudioTranscriptionApi;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection established: {}", session.getId());

        // 为每个连接创建一个 Sink
        Sinks.Many<DataBuffer> sink = Sinks.many().multicast().onBackpressureBuffer();
        audioSinks.put(session.getId(), sink);

        // 获取 Flux<ByteBuffer>
        Flux<ByteBuffer> audioStream = sink.asFlux().map(dataBuffer -> dataBuffer.asByteBuffer());

        // 调用转录 API
        Flux<DashScopeAudioTranscriptionApi.RealtimeResponse> responses =
                dashScopeAudioTranscriptionApi.realtimeStream(audioStream);

        // 订阅结果，推回前端
        responses.subscribe(
                response -> {
                    try {
                        DashScopeAudioTranscriptionApi.RealtimeResponse.Payload.Output output = response.payload() == null ? null : response.payload().output();
                        DashScopeAudioTranscriptionApi.RealtimeResponse.Payload.Output.Sentence sentence = output == null ? null : output.sentence();
                        if (sentence != null && sentence.text() != null) {
                            String text = sentence.text();

                            logger.info("Transcription result for session {}: {}", session.getId(), text);

                            // 构建并发送 JSON 响应
                            Map<String, String> message = Map.of("text", text);
                            String jsonMessage = objectMapper.writeValueAsString(message);
                            session.sendMessage(new TextMessage(jsonMessage));
                        }
                    } catch (Exception e) {
                        logger.error("Error processing transcription for session {}", session.getId(), e);
                        sendError(session, "转录处理错误: " + e.getMessage());
                    }
                },
                error -> {
                    logger.error("Transcription error for session {}", session.getId(), error);
                    sendError(session, "转录错误: " + error.getMessage());
                },
                () -> logger.info("Transcription completed for session {}", session.getId())
        );
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        // 接收音频数据，包装为 DataBuffer，发送到 Sink
        ByteBuffer buffer = message.getPayload();
        DataBuffer dataBuffer = new DefaultDataBufferFactory(true).wrap(buffer);

        Sinks.Many<DataBuffer> sink = audioSinks.get(session.getId());
        if (sink != null) {
            sink.tryEmitNext(dataBuffer);
        } else {
            logger.warn("No sink found for session {}", session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("WebSocket connection closed: {}, status: {}", session.getId(), status);

        // 完成流并清理资源
        Sinks.Many<DataBuffer> sink = audioSinks.remove(session.getId());
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session {}", session.getId(), exception);
        sendError(session, "连接错误: " + exception.getMessage());

        // 清理资源
        Sinks.Many<DataBuffer> sink = audioSinks.remove(session.getId());
        if (sink != null) {
            sink.tryEmitError(exception);
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            Map<String, String> message = Map.of("error", errorMessage);
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
        } catch (Exception e) {
            logger.error("Failed to send error message to session {}", session.getId(), e);
        }
    }
}
