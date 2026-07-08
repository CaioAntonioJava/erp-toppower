package br.com.toppower.erp_toppower;

import br.com.toppower.erp_toppower.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableConfigurationProperties(AppProperties.class)
public class ErpToppowerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ErpToppowerApplication.class, args);
	}
}
