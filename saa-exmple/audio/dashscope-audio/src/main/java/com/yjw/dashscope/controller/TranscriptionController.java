package com.yjw.dashscope.controller;

import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioTranscriptionApi;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.util.List;

/**
 * 语音识别、语音翻译
 */
@RestController
@RequestMapping("/ai/video/transcription")
public class TranscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptionController.class);

    private static final String API_KEY_ENV = "DASHSCOPE_API_KEY";

    // Test audio URL from DashScope official documentation
    private static final String TEST_AUDIO_URL = "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250211/tixcef/cherry.wav";

    // Test audio URLs for Paraformer and Fun-ASR file recognition
    private static final String PARAFORMER_TEST_AUDIO_URL_1 = "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_female2.wav";
    private static final String PARAFORMER_TEST_AUDIO_URL_2 = "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_male2.wav";

    // Test audio URL for Qwen-ASR
    private static final String QWEN_ASR_TEST_AUDIO_URL = "https://dashscope.oss-cn-beijing.aliyuncs.com/audios/welcome.mp3";

    private final DashScopeAudioTranscriptionModel transcriptionModel;

    public TranscriptionController(DashScopeAudioTranscriptionModel transcriptionModel) {
        this.transcriptionModel = transcriptionModel;
    }

    @GetMapping("/qwen3-call")
    public String qwen3Call() throws MalformedURLException {

        var audioFile = new UrlResource(TEST_AUDIO_URL);

        var transcriptionOptions = DashScopeAudioTranscriptionOptions.builder()
                .model(DashScopeModel.AudioModel.PARAFORMER_V1.getValue())
                .languageHints(List.of("zh", "en"))
                .punctuationPredictionEnabled(true)
                .format(DashScopeAudioTranscriptionApi.AudioFormat.WAV)
                .build();


        AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);
        AudioTranscriptionResponse response = transcriptionModel.call(transcriptionRequest);

        logger.info("LiveTranslate call test passed");

        String transcribedText = response.getResult().getOutput();
        logger.info("content: {}", transcribedText);


        return transcribedText;

    }

    /**
     * LiveTranslate 流式调用
     */
//    @GetMapping("/qwen3-livetranslate/stream")
//    public Flux<String> qwen3LiveTranslateStream() throws MalformedURLException {
//
//        var audioFile = new UrlResource(TEST_AUDIO_URL);
//
//        var transcriptionOptions = DashScopeAudioTranscriptionOptions.builder()
//                .model(DashScopeModel.AudioModel.PARAFORMER_V1.getValue())
//                .languageHints(List.of("zh", "en"))
//                .punctuationPredictionEnabled(true)
//                .format(DashScopeAudioTranscriptionApi.AudioFormat.WAV)
//                .build();
//        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);
//        Flux<AudioTranscriptionResponse> responseStream = transcriptionModel.stream(prompt);
//        // 把responseStream 转换为Flux<String>
//        return responseStream.map(response -> response.getResult().getOutput());
//
//
//    }

}
