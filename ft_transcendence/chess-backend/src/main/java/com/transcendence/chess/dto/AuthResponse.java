// Coloca esta respuesta en el paquete de objetos de intercambio de la API.
package com.transcendence.chess.dto;

// Importa el modelo de usuario que se incluirá en la respuesta de autenticación.
import com.transcendence.chess.model.User;

// Agrupa la información que el servidor devuelve tras registrar o autenticar a alguien.
public class AuthResponse {
    // Token que el cliente enviará posteriormente para identificarse ante la API.
    private String token;
    // Datos del usuario autenticado que acompañan al token.
    private User user;

    // Crea una respuesta con el token y el usuario que se van a devolver al cliente.
    public AuthResponse(String token, User user) {
        // Guarda el token recibido en el campo de esta respuesta.
        this.token = token;
        // Guarda el usuario recibido en el campo de esta respuesta.
        this.user = user;
    }

    // Permite que el serializador de respuesta obtenga el token.
    public String getToken() {
        // Devuelve el token almacenado.
        return token;
    }

    // Permite que el serializador de respuesta obtenga el usuario.
    public User getUser() {
        // Devuelve el usuario almacenado.
        return user;
    }
}
