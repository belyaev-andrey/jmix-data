package io.jmix.data.config;

import io.jmix.data.repository.JmixRepositoryFactoryBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Inherited
@Documented
@EnableJpaRepositories(repositoryFactoryBeanClass = JmixRepositoryFactoryBean.class)
@Import(JmixConfiguration.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableJmix {
}
