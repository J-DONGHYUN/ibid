package project.kjhjdh.ibid.common.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.user.domain.Email;
import project.kjhjdh.ibid.user.domain.User;
import project.kjhjdh.ibid.user.infra.UserRepository;

@Profile("local")
@Component
@RequiredArgsConstructor
public class LocalDataInitializer implements ApplicationRunner {

    private static final String TEST_EMAIL = "test@test.test";
    private static final String TEST_EMAIL2 = "test2@test.test";
    private static final String ADMIN_EMAIL = "admin@admin.test";
    private static final String TEST_PASSWORD = "test";
    private static final String TEST_USERNAME = "test";
    private static final String TEST_USERNAME2 = "test2";
    private static final String ADMIN_USERNAME = "admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductRepository productRepository;

    @Override
    public void run(ApplicationArguments args) {
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
        userRepository.save(User.create(TEST_EMAIL, encodedPassword, TEST_USERNAME));
        User seller = userRepository.save(User.create(TEST_EMAIL2, encodedPassword, TEST_USERNAME2));
        userRepository.save(User.createAdmin(ADMIN_EMAIL, encodedPassword, ADMIN_USERNAME));

        Product entity = Product.create(seller.getId(), "판매해요", "판매해요", 1000, 100);
        entity.openForSale();
        productRepository.save(entity);
    }
}
