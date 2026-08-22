package com.yjw.dashscope.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.MessageFormat;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.yjw.dashscope.helper.FrameExtraHelper;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/dashscope/multi")
public class MultiModelController {

    private final ChatClient dashScopeChatClient;

    @Resource
    private ResourceLoader resourceLoader;

    private static final String DEFAULT_PROMPT="这些是什么?";

    private static final String DEFAULT_VIDEO_PROMPT= "这是一组从视频中提取的图片帧，请描述此视频中的内容。";

    private static final String DEFAULT_AUDIO_PROMPT= "这是一个音频文件，请描述此音频中的内容。";

    // 视觉模型
    private static final String DEFAULT_MODEL = "qwen-vl-max";

    public MultiModelController(ChatModel chatModel){
        this.dashScopeChatClient = ChatClient.builder(chatModel)
                .build();
    }

    // 解析URL 图片内容
    @GetMapping("/image")
    public String image(@RequestParam(value = "prompt",required = false, defaultValue = DEFAULT_PROMPT)String prompt) throws Exception{
        List<Media> mediaList = List.of(new Media(MimeTypeUtils.IMAGE_PNG,
                new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg").toURL().toURI()));

        UserMessage message = UserMessage.builder()
                .text(prompt)
                .media(mediaList)
                .metadata(new HashMap<>())
                .build();

        message.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT, MessageFormat.IMAGE); // 消息类型设置为图片

        ChatResponse response = dashScopeChatClient
                // multiModel(true)开启多模态
                .prompt(new Prompt(message, DashScopeChatOptions.builder().model(DEFAULT_MODEL).multiModel(true).build()))
                .call()
                .chatResponse();

        return response.getResult().getOutput().getText();
    }

    // 抽样图片，然后分析图片
    @GetMapping("/video")
    public String video(@RequestParam(value = "prompt",required = false, defaultValue = DEFAULT_VIDEO_PROMPT)String prompt) throws Exception{

        List<Media> mediaList = FrameExtraHelper.createMediaList(10);

        UserMessage message =  UserMessage.builder()
                .text(prompt)
                .media(mediaList)
                .metadata(new HashMap<>())
                .build();

        message.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT,MessageFormat.VIDEO);

        ChatResponse response = dashScopeChatClient
                .prompt(new Prompt(message,
                        DashScopeChatOptions.builder().model(DEFAULT_MODEL).multiModel(true).build()))
                .call()
                .chatResponse();

        return response.getResult().getOutput().getText();
    }

    @GetMapping("/audio")
    public String audio(@RequestParam(value = "prompt", required = false,defaultValue = DEFAULT_AUDIO_PROMPT)String prompt) {

        Media media = new Media(MediaType.parseMediaType("audio/mpeg"),
                URI.create("https://dashscope.oss-cn-beijing.aliyuncs.com/audios/welcome.mp3"));

        UserMessage message = UserMessage.builder()
                .text(prompt)
                .media(media)
                .metadata(new HashMap<>())
                .build();

        message.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT,MessageFormat.AUDIO);

        ChatResponse response = dashScopeChatClient
                .prompt(new Prompt(message,
                        DashScopeChatOptions.builder().model("qwen-audio-turbo").multiModel(true).build()))
                .call()
                .chatResponse();

        return response.getResult().getOutput().getText();
    }

    @GetMapping("/image/bin")
    public String imageBinary(@RequestParam(value = "prompt", required = false, defaultValue = DEFAULT_PROMPT) String prompt) throws Exception{

        List<Media> mediaList = List.of(new Media(MimeTypeUtils.IMAGE_PNG,
                resourceLoader.getResource("classpath:/multimodel/dog_and_girl.jpeg")));

        UserMessage message = UserMessage.builder()
                .text(prompt)
                .media(mediaList)
                .metadata(new HashMap<>())
                .build();
        message.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT, MessageFormat.IMAGE);

        ChatResponse response = dashScopeChatClient
                .prompt(new Prompt(message,
                        DashScopeChatOptions.builder().model(DEFAULT_MODEL).multiModel(true).build()))
                .call()
                .chatResponse();

        return response.getResult().getOutput().getText();

    }

    @GetMapping("/stream/image")
    public String streamImage(
            @RequestParam(value = "prompt", required = false, defaultValue = DEFAULT_PROMPT) String prompt) {

        List<Media> mediaList = List.of(new Media(MimeTypeUtils.IMAGE_JPEG,
                resourceLoader.getResource("classpath:/multimodel/dog_and_girl.jpeg")));
        UserMessage message = UserMessage.builder()
                .text(prompt)
                .media(mediaList)
                .metadata(new HashMap<>()).build();
        message.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT, MessageFormat.IMAGE);

        List<ChatResponse> response = dashScopeChatClient
                .prompt(new Prompt(message,
                        DashScopeChatOptions.builder().model(DEFAULT_MODEL).multiModel(true).build()))
                .stream()
                .chatResponse()
                .collectList()
                .block();

        StringBuilder result = new StringBuilder();
        if (response != null) {
            for (ChatResponse chatResponse : response) {
                String outputContent = chatResponse.getResult().getOutput().getText();
                result.append(outputContent);
            }
        }

        return result.toString();
    }


}
