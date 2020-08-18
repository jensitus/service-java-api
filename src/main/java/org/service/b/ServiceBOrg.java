package org.service.b;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class ServiceBOrg implements WebMvcConfigurer {

	public static void main(String[] args) {
		SpringApplication.run(ServiceBOrg.class, args);
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/test/**").addResourceLocations("file:///home/jensitus/Documents/filefolder").setCachePeriod(0);
	}
}
