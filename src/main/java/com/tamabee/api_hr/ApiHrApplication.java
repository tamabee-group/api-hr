package com.tamabee.api_hr;

import com.tamabee.api_hr.config.SshTunnelInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ApiHrApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(ApiHrApplication.class);
		app.addInitializers(new SshTunnelInitializer());
		app.run(args);
	}

}
