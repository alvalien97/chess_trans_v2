// Paquete de objetos que reciben o devuelven datos de la API.
package com.transcendence.chess.dto;

// Cuerpo de petición con el usuario al que se quiere añadir como amistad.
public class AddFriendRequest {
    // Nombre de usuario que identifica a la persona buscada.
    private String username;

    // Devuelve el nombre enviado en la petición.
    public String getUsername() {
        // Entrega el valor almacenado en el campo.
        return username;
    }

    // Permite al deserializador JSON asignar el nombre recibido.
    public void setUsername(String username) {
        // Conserva en esta instancia el valor del parámetro.
        this.username = username;
    }
}
