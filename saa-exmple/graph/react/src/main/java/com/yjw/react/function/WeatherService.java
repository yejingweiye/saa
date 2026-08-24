package com.yjw.react.function;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class WeatherService implements Function<WeatherService.Request, WeatherService.Response> {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private static final String WEATHER_API_URL = "https://api.weatherapi.com/v1/forecast.json";

    private final WebClient webClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeatherService(WeatherProperties properties) {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .defaultHeader("key", properties.getApiKey())
                .build();
    }

    public static Response fromJson(Map<String, Object> json) {
        Map<String, Object> location = (Map<String, Object>) json.get("location");
        Map<String, Object> current = (Map<String, Object>) json.get("current");
        Map<String, Object> forecast = (Map<String, Object>) json.get("forecast");
        List<Map<String, Object>> forecastDays = (List<Map<String, Object>>) forecast.get("forecastday");
        String city = (String) location.get("name");
        return new Response(city, current, forecastDays);
    }


    @Override
    public Response apply(Request request) {
        if (request == null || !StringUtils.hasText(request.city())) {
            logger.error("Invalid request: city is required.");
            return null;
        }

        // 如果是中文转拼音
        String location = WeatherUtils.preprocessLocation(request.city());

        // 用来安全拼接 HTTP 请求地址和 URL 查询参数
        String url = UriComponentsBuilder.fromHttpUrl(WEATHER_API_URL)
                .queryParam("q", location)
                .queryParam("days", request.days())
                .toUriString();
        logger.info("url : {}", url);

        try {
            return doGetWeatherMock(request);
        }
        catch (Exception e) {
            logger.error("Failed to fetch weather data: {}", e.getMessage());
            return null;
        }
    }

    // 获取假数据
    @NotNull
    private Response doGetWeatherMock(Request request) throws JsonProcessingException {
        if (Objects.equals("杭州", request.city())) {
            return new Response(request.city(), Map.of("temp", 25, "condition", "Sunny"),
                    List.of(Map.of("date", "2025-05-27", "high", 28, "low", 20)));
        }
        else if (Objects.equals("上海", request.city())) {
            return new Response(request.city(), Map.of("temp", 26, "condition", "Sunny"),
                    List.of(Map.of("date", "2025-05-27", "high", 29, "low", 21)));
        }
        else if (Objects.equals("南京", request.city())) {
            return new Response(request.city(), Map.of("temp", 18, "condition", "cloudy"),
                    List.of(Map.of("date", "2025-05-27", "high", 18, "low", 10)));
        }
        else {
            return new Response(request.city(), Map.of("temp", -20, "condition", "Snowy"),
                    List.of(Map.of("date", "2025-05-27", "high", -10, "low", -30)));
        }
    }

    @NotNull
    private Response doGetWeather(String url, Request request) throws JsonProcessingException {
        Mono<String> responseMono = webClient.get().uri(url).retrieve().bodyToMono(String.class);
        String jsonResponse = responseMono.block();
        assert jsonResponse != null;

        Response response = fromJson(objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {
        }));
        logger.info("Weather data fetched successfully for city: {}", response.city());
        return response;
    }

    // Request 类
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("Weather Service API request")
    public record Request(

            @JsonProperty(required = true, value = "city") @JsonPropertyDescription("city name") String city,

            @JsonProperty(required = true,
                    value = "days") @JsonPropertyDescription("Number of days of weather forecast. Value ranges from 1 to 14") int days
    ){}

    // Response 类
    @JsonClassDescription("Weather Service API response")
    public record Response(
            @JsonProperty(required = true, value = "city") @JsonPropertyDescription("city name") String city,
            @JsonProperty(required = true,
                    value = "current") @JsonPropertyDescription("Current weather info") Map<String, Object> current,
            @JsonProperty(required = true,
                    value = "forecastDays") @JsonPropertyDescription("Forecast weather info") List<Map<String, Object>> forecastDays
    ){}

}


