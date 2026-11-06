package com.foodconnect.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Application-level configuration properties.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppConfig {

    /** Radius in km for nearby donation notifications and discovery. */
    private double notificationRadiusKm = 10.0;

    /** Maximum upload size in MB for food images. */
    private int maxUploadSizeMb = 5;
}
