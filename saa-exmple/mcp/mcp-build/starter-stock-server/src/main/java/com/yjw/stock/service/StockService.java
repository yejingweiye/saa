package com.yjw.stock.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;

/**
 * 股票服务，用于从东方财富接口获取股票实时行情
 * 该服务可以获取股票实时价格、最高价、最低价、开盘价、成交量、成交额等信息
 */
@Service
public class StockService {
    private static final Logger logger = LoggerFactory.getLogger(StockService.class);
    // 东方财富股票实时接口地址
    private static final String BASE_URL = "https://push2.eastmoney.com/api/qt/stock/get";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RestClient restClient;

    public StockService() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 东方财富接口原始返回数据实体
     * 接口返回价格单位：分；成交量单位：手；成交额单位：元
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StockData(
            @JsonProperty("f43") Double currentPrice,    // 最新价（单位：分）
            @JsonProperty("f44") Double highPrice,       // 最高价（单位：分）
            @JsonProperty("f45") Double lowPrice,        // 最低价（单位：分）
            @JsonProperty("f46") Double openPrice,       // 开盘价（单位：分）
            @JsonProperty("f47") Double volume,          // 成交量（单位：手）
            @JsonProperty("f48") Double amount,          // 成交额（单位：元）
            @JsonProperty("f57") String code,            // 股票代码
            @JsonProperty("f58") String name             // 股票名称
    ) {
    }

    /**
     * 对外输出的股票信息实体，已做单位换算
     */
    @JsonSerialize
    public record StockInfo(
            @JsonProperty("code") String code,
            @JsonProperty("name") String name,
            @JsonProperty("currentPrice") Double currentPrice,
            @JsonProperty("highPrice") Double highPrice,
            @JsonProperty("lowPrice") Double lowPrice,
            @JsonProperty("openPrice") Double openPrice,
            @JsonProperty("volume") Double volume,
            @JsonProperty("amount") Double amount
    ) implements Serializable {
    }

    /**
     * 获取指定股票代码的实时行情信息
     * @param stockCode 6位股票代码
     * @return 股票行情信息
     */
    @Tool(name = "getStockInfo", description = "根据股票代码获取股票实时行情信息")
    public StockInfo getStockInfo(String stockCode) {
        try {
            // 校验股票代码格式，必须6位数字
            if (!stockCode.matches("^[0-9]{6}$")) {
                throw new IllegalArgumentException("股票代码必须为6位数字");
            }

            logger.info("开始获取股票 {} 的行情数据", stockCode);

            // 东方财富secid规则：沪市6开头=1.xxxxxx；深市0/3开头=0.xxxxxx
            String secid = stockCode.startsWith("6") ? "1." + stockCode : "0." + stockCode;

            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("secid", secid)
                            .queryParam("fields", "f43,f44,f45,f46,f47,f48,f57,f58")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            logger.info("接口原始返回内容：{}", response);

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");

            if (data.isMissingNode()) {
                logger.warn("未查询到股票数据");
                throw new IllegalArgumentException("未查询到股票代码【" + stockCode + "】的相关信息");
            }

            StockData stockData = objectMapper.treeToValue(data, StockData.class);
            logger.info("解析后的原始数据：{}", stockData);

            if (stockData == null || stockData.name() == null) {
                throw new IllegalArgumentException("股票代码【" + stockCode + "】返回数据格式异常");
            }

            // 单位转换：分→元；手→万手；元→亿元
            return new StockInfo(
                    stockCode,
                    stockData.name(),
                    stockData.currentPrice() / 100.0,      // 分转元
                    stockData.highPrice() / 100.0,         // 分转元
                    stockData.lowPrice() / 100.0,          // 分转元
                    stockData.openPrice() / 100.0,         // 分转元
                    stockData.volume() / 10000.0,          // 手 → 万手
                    stockData.amount() / 100000000.0       // 元 → 亿元
            );
        } catch (IllegalArgumentException e) {
            logger.error("参数校验异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("获取股票【{}】行情失败：{}", stockCode, e.getMessage(), e);
            throw new RuntimeException("获取股票【" + stockCode + "】行情失败：" + e.getMessage());
        }
    }
}