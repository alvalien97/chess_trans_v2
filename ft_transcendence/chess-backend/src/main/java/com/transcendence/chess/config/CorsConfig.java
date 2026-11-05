// Paquete que contiene la configuración de infraestructura de Spring.
package com.transcendence.chess.config;

// Permite registrar objetos de configuración administrados por Spring.
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Configuración MVC que define CORS y el bean para codificar contraseñas.
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Sobrescribe el registro de reglas de origen cruzado de Spring MVC.
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Aplica las reglas a todas las rutas del backend.
        registry.addMapping("/**")
                // Permite peticiones desde cualquier patrón de origen.
                .allowedOriginPatterns("*")
                // Acepta estos métodos HTTP desde el navegador.
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // Permite cualquier cabecera enviada por el cliente.
                .allowedHeaders("*");
    }

    // Registra un PasswordEncoder que Spring puede inyectar donde se necesite.
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt guarda hashes con sal y permite comprobar contraseñas sin almacenarlas en claro.
        return new BCryptPasswordEncoder();
    }
}
