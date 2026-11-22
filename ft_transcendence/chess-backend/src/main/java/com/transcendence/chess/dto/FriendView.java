//Paquete de objetos que reciben o devuelven datos de la API.
package com.transcendence.chess.dto;

//Vista inmutable de una amistad con solo los datos que la interfaz necesita.
public record FriendView(Long id, String username, String avatarUrl, boolean online) {
	//id identifica al usuario; username y avatarUrl muestran el perfil; online indica la presencia actual.
}
