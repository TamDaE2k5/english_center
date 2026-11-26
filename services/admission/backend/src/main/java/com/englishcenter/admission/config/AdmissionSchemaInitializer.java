package com.englishcenter.admission.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;

/**
 * Tự động áp dụng các script SQL bổ sung trong schema admission khi khởi động trên PostgreSQL.
 * Tất cả script đều có tính idempotent (IF NOT EXISTS / ON CONFLICT DO NOTHING) để bảo toàn dữ liệu hiện có.
 */
@Component
public class AdmissionSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdmissionSchemaInitializer.class);

    private static final List<String> MIGRATION_SCRIPTS = List.of(
            "db/admission-account-audit.sql",
            "db/admission-cash-confirmation.sql",
            "db/admission-class-cancellation.sql",
            "db/admission-hold-extension-audit.sql",
            "db/admission-payment-callback.sql",
            "db/admission-student-sync.sql"
    );

    private final ObjectProvider<DataSource> dataSourceProvider;

    public AdmissionSchemaInitializer(ObjectProvider<DataSource> dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    /**
     * Kiểm tra kết nối PostgreSQL và chạy tuần tự các script DDL bổ sung của schema admission.
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            return;
        }
        try (Connection conn = dataSource.getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("postgresql")) {
                log.info("Bỏ qua script migration PostgreSQL vì đang dùng CSDL: {}", product);
                return;
            }
        }

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        for (String path : MIGRATION_SCRIPTS) {
            populator.addScript(new ClassPathResource(path));
        }
        populator.execute(dataSource);
        log.info("Đã đồng bộ {} script migration cho schema admission", MIGRATION_SCRIPTS.size());
    }
}
