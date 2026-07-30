# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build the entire project
mvn clean compile

# Run the application
mvn -pl saa-graph spring-boot:run

# Package
mvn clean package
```

## Environment

- **Java 17+** required
- Set `AI_DASHSCOPE_API_KEY` environment variable (Alibaba DashScope API key): `export AI_DASHSCOPE_API_KEY=your_key`
- Default model: `qwen-max` (configurable in `application.yml`)

## API Endpoints (port 8080)

| Endpoint | Parameters | Description |
|---|---|---|
| `GET /graph/human/expand` | `query`, `expander_number`(default:3), `thread_id`(default:yingzi) | Start a new workflow, returns SSE stream |
| `GET /graph/human/resume` | `thread_id`, `feed_back`(default:true) | Resume from human-feedback pause, returns SSE stream |

## Architecture

This is a Spring Boot 3.x demo of a **stateful AI workflow graph** using Spring AI Alibaba Graph (`StateGraph`).

### Workflow

```
START → expander → human_feedback → translate → END
                            │
                     (conditional edge)
                   ┌───────┴───────┐
                   │               │
               translate        END
              (feed_back=true)  (feed_back=false)
```

### Nodes (all under `com.yjw.node`)

- **ExpanderNode** — Calls Qwen LLM to generate query variants from the original query
- **HumanFeedbackNode** — Checks `feed_back` state; sets `human_next_node` to `"translate"` or `StateGraph.END`
- **TranslateNode** — Calls Qwen LLM to translate the query to English

### Edge Actions (`com.yjw.dispatcher`)

- **HumanFeedbackDispatcher** — Reads `human_next_node` from state to decide which edge to follow

### Controller (`com.yjw.controller`)

- **WFGraphController** compiles the `StateGraph` with `interruptBefore("human_feedback")`, so the workflow pauses before the human_feedback node and waits for a `/resume` call
- Both endpoints return **SSE streams** via Reactor Flux
- State is persisted in-memory via `MemorySaver`, keyed by `thread_id`

### Streaming (`com.yjw.controller.graphprocess`)

- **GraphProcess** subscribes to `NodeOutput` flux from the compiled graph and emits `ChatMessage` SSE events with node name + content
- `StreamingOutput` chunks (from LLM streaming) and full node states are both supported

### Configuration (`com.yjw.config`)

- **WFGraphConfiguration** defines the `StateGraph` bean with node/edge topology and key-value state strategies using `ReplaceStrategy`

## Key State Keys

| Key | Description |
|---|---|
| `query` | Input query |
| `thread_id` | Conversation thread identifier |
| `expander_number` | How many query variants to generate |
| `expander_content` | LLM output from expander node |
| `feed_back` | User feedback boolean (true = continue to translate) |
| `human_next_node` | Next node decision from HumanFeedbackNode |
| `translate_content` | LLM output from translate node |

## Dependencies

- Spring Boot 3.5.7, Spring AI 1.1.0, Spring AI Alibaba 1.1.0.0
- LLM provider: Alibaba DashScope (Qwen models)
- Graph framework: `spring-ai-alibaba-graph-core`
