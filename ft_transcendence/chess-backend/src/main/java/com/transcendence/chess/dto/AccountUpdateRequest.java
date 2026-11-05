// Paquete de objetos que reciben o devuelven datos de la API.
package com.transcendence.chess.dto;

// Restricción para validar la longitud del nombre de usuario.
import jakarta.validation.constraints.Size;

// Datos opcionales y de confirmación para actualizar una cuenta.
public class AccountUpdateRequest {
    // Contraseña vigente que permite confirmar cambios protegidos.
    private String currentPassword;

    // Si se proporciona, el nombre debe tener entre 3 y 32 caracteres.
    @Size(min = 3, max = 32)
    // Nuevo nombre de usuario solicitado.
    private String username;

    // Nueva contraseña solicitada, si se quiere cambiar.
    private String newPassword;
    // Nueva URL del avatar, si se quiere cambiar.
    private String avatarUrl;

    // Devuelve la contraseña vigente proporcionada para confirmar la operación.
    public String getCurrentPassword() { return currentPassword; }
    // Guarda la contraseña vigente enviada por el cliente.
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    // Devuelve el nombre de usuario solicitado.
    public String getUsername() { return username; }
    // Guarda el nuevo nombre de usuario solicitado.
    public void setUsername(String username) { this.username = username; }
    // Devuelve la nueva contraseña solicitada.
    public String getNewPassword() { return newPassword; }
    // Guarda la nueva contraseña solicitada.
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    // Devuelve la URL de avatar solicitada.
    public String getAvatarUrl() { return avatarUrl; }
    // Guarda la URL de avatar solicitada.
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}