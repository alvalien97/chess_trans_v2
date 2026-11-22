//Declara el paquete para que Java y Spring identifiquen esta clase como parte de la aplicación de ajedrez.
package com.transcendence.chess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//Marca esta clase como punto de configuración principal de Spring Boot.
@SpringBootApplication
//Define la clase que contiene el punto de entrada ejecutable del backend.
public class ChessApplication {

    //Método que la máquina virtual de Java ejecuta al iniciar el programa.
    public static void main(String[] args) {
        //Arranca Spring usando esta clase para localizar la configuración y los componentes de la aplicación.
        SpringApplication.run(ChessApplication.class, args);
    }
}
