package project.kjhjdh.ibid.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import project.kjhjdh.ibid.TestcontainersConfiguration;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
// IntegrationTestSupport 와 달리 DbCleaner 를 쓰지 않는다 — 이 기반의 테스트는 DB 를 건드리지 않는 HTTP 경계 확인용이다.
// DB 를 쓰는 테스트가 생기면 IntegrationTestSupport 를 쓰거나 여기에 DbCleaner 를 더한다.
public abstract class WebIntegrationTestSupport {

    @Autowired
    protected TestRestTemplate restTemplate;
}
