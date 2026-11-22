//Coloca este objeto en el paquete de estructuras usadas para intercambiar datos con la API.
package com.transcendence.chess.dto;

//Valida que el texto tenga formato de dirección de correo electrónico.
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//Representa los datos de autenticación recibidos desde una petición HTTP.
public class AuthRequest {

    //Exige que el nombre de usuario tenga entre 3 y 32 caracteres cuando se valida el objeto.
    @Size(min = 3, max = 32)
    //Guarda el nombre de usuario enviado por el cliente.
    private String username;

    //Comprueba que el correo tenga un formato válido.
    @Email
    //Exige que el correo esté presente y no sea solo espacios.
    @NotBlank
    //Guarda el correo enviado por el cliente.
    private String email;

    //Exige que la contraseña no esté vacía.
    @NotBlank
    //Limita la contraseña a una longitud de entre 8 y 72 caracteres.
    @Size(min = 8, max = 72)
    //Guarda la contraseña recibida; la transformación segura corresponde a la capa de servicio.
    private String password;

    //Devuelve el nombre de usuario recibido.
    public String getUsername() {
        //Entrega el valor del campo username.
        return username;
    }

    //Permite al serializador de JSON o al código de aplicación asignar el nombre de usuario.
    public void setUsername(String username) {
        //Actualiza el campo de esta instancia con el parámetro recibido.
        this.username = username;
    }

    //Devuelve el correo recibido.
    public String getEmail() {
        //Entrega el valor del campo email.
        return email;
    }

    //Permite asignar el correo recibido en la petición.
    public void setEmail(String email) {
        //Actualiza el campo de esta instancia con el parámetro recibido.
        this.email = email;
    }

    //Devuelve la contraseña recibida.
    public String getPassword() {
        //Entrega el valor del campo password.
        return password;
    }

    //Permite asignar la contraseña recibida en la petición.
    public void setPassword(String password) {
        //Actualiza el campo de esta instancia con el parámetro recibido.
        this.password = password;
    }
}
