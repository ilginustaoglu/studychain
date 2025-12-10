package com.studychain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * StudyChain Uygulamasının Başlangıç Noktası
 * 
 * @SpringBootApplication: Bu sınıfın Spring Boot uygulamasının ana sınıfı olduğunu belirtir
 * - @Configuration: Bu sınıfın yapılandırma sınıfı olduğunu belirtir
 * - @EnableAutoConfiguration: Spring Boot'un otomatik yapılandırmasını aktif eder
 * - @ComponentScan: Bu paketteki tüm bileşenleri tarar
 */
@SpringBootApplication
public class StudyChainApplication {

    /**
     * Uygulamanın başlangıç metodu
     * @param args Komut satırı argümanları
     */
    public static void main(String[] args) {
        // Spring Boot uygulamasını başlat
        SpringApplication.run(StudyChainApplication.class, args);
        
        System.out.println("\n=================================");
        System.out.println("🚀 StudyChain Successfully Started!");
        System.out.println("📍 URL: http://localhost:8080");
        System.out.println("=================================\n");
    }
}