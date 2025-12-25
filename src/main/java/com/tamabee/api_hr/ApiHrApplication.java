package com.tamabee.api_hr;

import com.tamabee.api_hr.config.SshTunnelInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiHrApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(ApiHrApplication.class);
		app.addInitializers(new SshTunnelInitializer());
		app.run(args);
	}

}
