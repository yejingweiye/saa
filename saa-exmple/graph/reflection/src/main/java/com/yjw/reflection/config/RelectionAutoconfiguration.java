package com.yjw.reflection.config;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

@Configuration
public class RelectionAutoconfiguration {

    public static class AssistantGraphNode implements NodeAction {

        private final LlmNode llmNode;

        private SystemPromptTemplate systemPromptTemplate;

        private final String NODE_ID = "call_model";

        private static final String CLASSIFIER_PROMPT_TEMPLATE = """
                你是一名作文助手，负责撰写优秀的五段式议论文。
                根据用户的要求，生成质量尽可能高的文章。
                如果用户给出修改批评意见，请基于你上一轮输出，返回修改后的完整文章。
                只返回我需要的正文内容，不要额外添加任何对话交互类语言。
                请使用中文进行回答。
                """;

        public AssistantGraphNode(ChatClient chatClient) {
            this.systemPromptTemplate = new SystemPromptTemplate(CLASSIFIER_PROMPT_TEMPLATE);
            this.llmNode = LlmNode.builder()
                    .systemPromptTemplate(systemPromptTemplate.render())
                    .chatClient(chatClient)
                    .messagesKey("messages")
                    .build();
        }


        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private ChatClient chatClient;

            public Builder chatClient(ChatClient chatClient) {
                this.chatClient = chatClient;
                return this;
            }

            public AssistantGraphNode build() {

                if (chatClient == null) {
                    throw new IllegalArgumentException("ChatClient must be provided");
                }
                return new AssistantGraphNode(chatClient);
            }

        }


        @Override
        public Map<String, Object> apply(OverAllState state) throws Exception {
            List<Message> messages = (List<Message>) overAllState.value(ReflectAgent.MESSAGES).get();
            KeyStrategyFactory keyStrategyFactory = () -> {
                HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();

                keyStrategyHashMap.put(ReflectAgent.MESSAGES, new AppendStrategy());
                return keyStrategyHashMap;
            };
            StateGraph stateGraph = new StateGraph(keyStrategyFactory).addNode(this.NODE_ID, AsyncNodeAction.node_async(llmNode))
                    .addEdge(StateGraph.START, this.NODE_ID)
                    .addEdge(this.NODE_ID, StateGraph.END);

            OverAllState invokeState = stateGraph.compile().call(Map.of(ReflectAgent.MESSAGES, messages)).get();
            List<Message> reactMessages = (List<Message>) invokeState.value(ReflectAgent.MESSAGES).orElseThrow();

            return Map.of(ReflectAgent.MESSAGES, reactMessages);

        }
    }

    public static class JudgeGraphNode implements NodeAction {

        private final LlmNode llmNode;

        private final String NODE_ID = "judge_response";

        private SystemPromptTemplate systemPromptTemplate;

        private static final String CLASSIFIER_PROMPT_TEMPLATE = """
                    你是一名教师，负责对学生提交的作文进行评阅。请给出详细的反馈意见以及修改建议。
                
                    你的反馈需要覆盖以下几个方面：
                
                    - 篇幅：文章内容展开是否充分？篇幅是否达标，需要扩充内容还是精简压缩？
                    - 内容深度：观点是否充分展开？是否具备足够的分析、论据与阐释？
                    - 文章结构：行文逻辑是否清晰通顺？开头引入、段落过渡、结尾总结是否到位？
                    - 写作风格与语气：行文风格是否贴合写作场景与阅读对象？语气是否连贯得体？
                    - 语言表达：词汇、语法、句式是否准确，句式是否富有变化？
                
                    只输出可落地的修改建议，不要给出分数、等级，也不要做总体总结评价。
                
                    请使用中文进行回答。
                """;

        public JudgeGraphNode(ChatClient chatClient) {
            this.systemPromptTemplate = new SystemPromptTemplate(CLASSIFIER_PROMPT_TEMPLATE);
            this.llmNode = LlmNode.builder()
                    .chatClient(chatClient)
                    .systemPromptTemplate(systemPromptTemplate.render())
                    .messagesKey(ReflectAgent.MESSAGES)
                    .build();

        }


        public static Builder builder() {
            return new Builder();
        }



        public static class Builder {

            private ChatClient chatClient;

            public JudgeGraphNode.Builder chatClient(ChatClient chatClient) {
                this.chatClient = chatClient;
                return this;
            }

            public JudgeGraphNode build() {
                if (chatClient == null) {
                    throw new IllegalArgumentException("ChatClient must be provided");
                }
                return new JudgeGraphNode(chatClient);
            }

        }

        @Override
        public Map<String, Object> apply(OverAllState state) throws Exception {
            List<Message> messages = (List<Message>) allState.value(ReflectAgent.MESSAGES).get();


            KeyStrategyFactory keyStrategyFactory = () -> {
                HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();

                keyStrategyHashMap.put(ReflectAgent.MESSAGES, new AppendStrategy());
                return keyStrategyHashMap;
            };

            StateGraph stateGraph = new StateGraph(keyStrategyFactory).addNode(this.NODE_ID, AsyncNodeAction.node_async(llmNode))
                    .addEdge(StateGraph.START, this.NODE_ID)
                    .addEdge(this.NODE_ID, StateGraph.END);

            CompiledGraph compile = stateGraph.compile();

            OverAllState invokeState = compile.call(Map.of(ReflectAgent.MESSAGES, messages)).get();

            UnaryOperator<List<Message>> convertLastToUserMessage = messageList -> {
                int size = messageList.size();
                if (size == 0)
                    return messageList;
                Message last = messageList.get(size - 1);
                messageList.set(size - 1, new UserMessage(last.getText()));
                return messageList;
            };

            List<Message> reactMessages = (List<Message>) invokeState.value(ReflectAgent.MESSAGES).orElseThrow();
            convertLastToUserMessage.apply(reactMessages);

            return Map.of(ReflectAgent.MESSAGES, reactMessages);


        }

    }
}
