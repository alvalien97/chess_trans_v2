//Paquete que contiene la configuración de infraestructura de Spring.
package com.transcendence.chess.config;

//Código HTTP y contenedor de respuesta usado para crear errores REST.
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//Convierte excepciones de controladores en respuestas HTTP coherentes.
@RestControllerAdvice
public class RestExceptionHandler {

    //Capturo errores de argumento y los presenta como peticiones incorrectas.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        //Devuelve HTTP 400 con un objeto JSON que contiene la explicación del error.
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    //Capturo errores de validación de los parámetros del controlador.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        //Busca el *primer error de campo producido por la validación.
        String message = ex.getBindingResult().getFieldErrors().stream()
                //Convierte el resultado a un Optional para manejar que no haya errores de campo.
                .findFirst()
                //Construye un texto con el nombre del campo y el motivo de validación.
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                //Usa un mensaje genérico si el resultado no contiene errores de campo.
                .orElse("Datos inválidos");
        //Devuelve HTTP 400 y el mensaje de validación en JSON.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }
}
