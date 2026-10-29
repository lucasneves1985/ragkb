package br.lcn.ragkb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RagkbApplication {

	public static void main(String[] args) {
		SpringApplication.run(RagkbApplication.class, args);
	}

}
