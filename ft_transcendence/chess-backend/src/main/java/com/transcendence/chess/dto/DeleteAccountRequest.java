//Paquete de objetos que reciben o devuelven datos de la API.
package com.transcendence.chess.dto;

//Rechaza valores nulos, vacíos o compuestos solo por espacios.
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//Datos requeridos para confirmar el borrado de una cuenta.
public class DeleteAccountRequest {
    //Exige que se proporcione la contraseña actual.
    @NotBlank
    //Limita la contraseña a una longitud compatible con el resto del backend.
    @Size(min = 8, max = 72)
    //Contraseña que el servicio comparará con la credencial vigente.
    private String currentPassword;

    //Devuelve la contraseña enviada para confirmar el borrado.
    public String getCurrentPassword() { return currentPassword; }
    //Guarda la contraseña recibida en el cuerpo JSON.
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
}