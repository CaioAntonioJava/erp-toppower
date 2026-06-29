package br.com.toppower.erp_toppower;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ErpToppowerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ErpToppowerApplication.class, args);
	}
}
