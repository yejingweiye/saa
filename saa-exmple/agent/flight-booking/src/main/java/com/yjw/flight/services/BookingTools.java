package com.yjw.flight.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yjw.flight.data.BookingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.core.NestedExceptionUtils;

import java.time.LocalDate;
import java.util.function.Function;

// 函数工具类
@Configuration
public class BookingTools {

    private static final Logger logger = LoggerFactory.getLogger(BookingTools.class);

    @Autowired
    private FlightBookingService flightBookingService;

    /**
     * // 等价效果
     * public static class BookingDetailsRequest {
     *     private final String bookingNumber;
     *     private final String name;
     *
     *     //全参构造器自动生成
     *     public BookingDetailsRequest(String bookingNumber, String name) {
     *         this.bookingNumber = bookingNumber;
     *     }
     *
     *     // 获取值的方法，注意不是getXXX！
     *     public String bookingNumber() { return bookingNumber; }
     *     public String name() { return name; }
     *
     *     //自动生成 equals、hashCode、toString
     *     备注没有set方法
     * }
     */
    public record BookingDetailsRequest(String bookingNumber, String name) {
    }

    public record ChangeBookingDatesRequest(String bookingNumber, String name, String date, String from, String to) {
    }

    public record CancelBookingRequest(String bookingNumber, String name) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BookingDetails(String bookingNumber, String name, LocalDate date, BookingStatus bookingStatus,
                                 String from, String to, String bookingClass) {
    }

    /**
     * 通过 @Bean + Function<...> 注册为可被模型调用的函数
     * 并与具体业务实现类进行解耦
     * Function< T , R >
     *  T = BookingDetailsRequest 输入
     *  R = BookingDetails 返回输出
     * @return
     */
    @Bean
    @Description("获取机票预定详细信息")
    public Function<BookingDetailsRequest, BookingDetails> getBookingDetails() {
        return request -> {
            try {
                return flightBookingService.getBookingDetails(request.bookingNumber(), request.name());
            }
            catch (Exception e) {
                logger.warn("Booking details: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
                return new BookingDetails(request.bookingNumber(), request.name(), null, null, null, null, null);
            }
        };
    }

    @Bean
    @Description("修改机票预定日期")
    public Function<ChangeBookingDatesRequest, String> changeBooking() {
        return request -> {
            flightBookingService.changeBooking(request.bookingNumber(), request.name(), request.date(), request.from(),
                    request.to());
            return "";
        };
    }

    @Bean
    @Description("取消机票预定")
    public Function<CancelBookingRequest, String> cancelBooking() {
        return request -> {
            flightBookingService.cancelBooking(request.bookingNumber(), request.name());
            return "";
        };
    }

}
