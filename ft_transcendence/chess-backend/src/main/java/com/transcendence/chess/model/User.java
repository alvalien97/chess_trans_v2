//Paquete con las entidades que representan datos del dominio.
package com.transcendence.chess.model;

//Marca temporal absoluta usada para registrar cuándo se creó la cuenta.
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

//Persiste esta clase como una fila de la tabla users.
@Entity
@Table(name = "users")
public class User {

    //Clave primaria cuyo valor asigna la base de datos.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Nombre obligatorio, único y limitado a 32 caracteres.
    @Column(nullable = false, unique = true, length = 32)
    private String username;

    //Correo obligatorio y único para que dos cuentas no compartan dirección.
    @Column(nullable = false, unique = true)
    private String email;

    //Nunca incluir el hash de contraseña en JSON devuelto por la API.
    @JsonIgnore
    //Guarda el hash de la contraseña, no la contraseña en texto plano.
    @Column(nullable = false)
    private String passwordHash;

    //URL opcional del avatar; TEXT permite guardar una cadena larga.
    @Column(columnDefinition = "TEXT")
    private String avatarUrl;

    //Fecha obligatoria de creación que no se actualiza en modificaciones posteriores.
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    //Callback ejecutado por JPA antes de insertar la cuenta.
    @PrePersist
    void onCreate() {
        //Establece la fecha actual solo si aún no se proporcionó una.
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    //Devuelve la clave primaria asignada por la base de datos.
    public Long getId() {
        return id;
    }

    //Devuelve el nombre público de usuario.
    public String getUsername() {
        return username;
    }

    //Cambia el nombre público de usuario.
    public void setUsername(String username) {
        this.username = username;
    }

    //Devuelve el correo asociado a la cuenta.
    public String getEmail() {
        return email;
    }

    //Cambia el correo asociado a la cuenta.
    public void setEmail(String email) {
        this.email = email;
    }

    //Devuelve el hash de contraseña para uso interno del servicio.
    public String getPasswordHash() {
        return passwordHash;
    }

    //Guarda un hash calculado por la lógica de autenticación.
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    //Devuelve la URL del avatar.
    public String getAvatarUrl() {
        return avatarUrl;
    }

    //Cambia la URL del avatar.
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    //Devuelve la fecha de creación de la cuenta.
    public Instant getCreatedAt() {
        return createdAt;
    }
}
