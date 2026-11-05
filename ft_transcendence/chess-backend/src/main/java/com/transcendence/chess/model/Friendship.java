// Paquete con las entidades que representan datos del dominio.
package com.transcendence.chess.model;

// Anotaciones JPA para definir entidad, columnas y relaciones.
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// Persiste las relaciones de amistad en la tabla friendships.
@Entity
// Evita guardar dos veces la misma relación ordenada de usuario y amigo.
@Table(
    name = "friendships",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "friend_id"})
)
public class Friendship {

    // Clave primaria asignada automáticamente.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Usuario desde cuya cuenta se registra la relación.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Usuario añadido como amigo; LAZY carga la relación cuando se necesita.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id", nullable = false)
    private User friend;

    // Devuelve el id de esta relación.
    public Long getId() {
        return id;
    }

    // Devuelve el usuario que inició la relación almacenada.
    public User getUser() {
        return user;
    }

    // Asigna el usuario que inicia la relación.
    public void setUser(User user) {
        this.user = user;
    }

    // Devuelve al usuario añadido como amigo.
    public User getFriend() {
        return friend;
    }

    // Asigna al usuario añadido como amigo.
    public void setFriend(User friend) {
        this.friend = friend;
    }
}
