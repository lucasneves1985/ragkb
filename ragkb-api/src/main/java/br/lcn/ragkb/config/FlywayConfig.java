package br.lcn.ragkb.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        return flyway;
    }

    @Bean
    public static BeanFactoryPostProcessor flywayDependsOnPostProcessor() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                for (String beanName : beanFactory.getBeanDefinitionNames()) {
                    BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
                    if ("entityManagerFactory".equals(beanName) || (bd.getBeanClassName() != null && bd.getBeanClassName().contains("EntityManagerFactory"))) {
                        List<String> dependsOn = new ArrayList<>();
                        if (bd.getDependsOn() != null) {
                            dependsOn.addAll(Arrays.asList(bd.getDependsOn()));
                        }
                        dependsOn.add("flyway");
                        bd.setDependsOn(dependsOn.toArray(new String[0]));
                    }
                }
            }
        };
    }
}
