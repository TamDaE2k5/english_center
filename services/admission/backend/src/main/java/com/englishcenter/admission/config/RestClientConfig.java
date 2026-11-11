package com.englishcenter.admission.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Cấu hình HTTP RestClient cho các giao tiếp liên service (East-West traffic).
 * Thiết lập ngưỡng Timeout kết nối (3 giây) và Timeout đọc dữ liệu (5 giây)
 * để tránh tình trạng treo luồng khi dịch vụ lân cận gặp sự cố.
 */
@Configuration
public class RestClientConfig {

    /**
     * Tạo Bean RestClient với cài đặt giới hạn thời gian phản hồi.
     *
     * @return Đối tượng RestClient đã cấu hình timeout
     */
    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
