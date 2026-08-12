package project.kjhjdh.ibid.support.seed;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import project.kjhjdh.ibid.product.infra.ProductRepository;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SeedProperties.class)
@ConditionalOnProperty(prefix = "seed", name = "enabled", havingValue = "true")
public class SeedConfiguration {

    @Bean
    ProductJpaBaseline productJpaBaseline(ProductRepository productRepository,
                                         PlatformTransactionManager transactionManager) {
        return new ProductJpaBaseline(productRepository, new TransactionTemplate(transactionManager));
    }

    @Bean
    DataSeeder dataSeeder(DataSource dataSource, SeedProperties properties,
                          ProductJpaBaseline productJpaBaseline, PasswordEncoder passwordEncoder) {
        return new DataSeeder(dataSource, properties, productJpaBaseline, passwordEncoder);
    }
}
