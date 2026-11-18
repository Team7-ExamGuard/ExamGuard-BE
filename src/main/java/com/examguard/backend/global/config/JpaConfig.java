package com.examguard.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * @CreatedDate, @LastModifiedDate 자동 입력을 켠다.
 * 메인 클래스에 붙이면 웹 계층만 띄우는 테스트(@WebMvcTest)에서 JPA 설정을 찾다가 실패하므로 따로 둔다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
