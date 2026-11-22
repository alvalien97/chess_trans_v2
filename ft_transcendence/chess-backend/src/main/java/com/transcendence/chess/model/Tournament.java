//Paquete con las entidades que representan datos del dominio.
package com.transcendence.chess.model;

//Marca temporal para registrar cuándo se creó el torneo.
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

//Persiste el torneo en la tabla tournaments.
@Entity
@Table(name = "tournaments")
public class Tournament {
    //Clave primaria textual del torneo.
    @Id
    //Identificador limitado a 36 caracteres, compatible con UUIDs.
    @Column(length = 36)
    private String id;

    //Nombre obligatorio del torneo, limitado a 80 caracteres.
    @Column(nullable = false, length = 80)
    private String name;

    //Propietario obligatorio del torneo, enlazado mediante owner_id.
    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    //Cantidad máxima de jugadores permitida.
    @Column(nullable = false)
    private int maxPlayers;

    //Estado textual del torneo; comienza esperando a que se inicie.
    @Column(nullable = false, length = 16)
    private String status = "WAITING";

    //Fecha de creación obligatoria e inmutable después del alta.
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    //Método de ciclo de vida llamado por JPA antes de insertar el torneo.
    @PrePersist
    void onCreate() {
        //Rellena la fecha automáticamente si no se asignó previamente.
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    //Lee y modifica los campos del torneo mediante métodos accesores.
    //Devuelve el identificador del torneo.
    public String getId() { return id; }
    //Asigna el identificador del torneo.
    public void setId(String id) { this.id = id; }
    //Devuelve el nombre visible.
    public String getName() { return name; }
    //Cambia el nombre visible.
    public void setName(String name) { this.name = name; }
    //Devuelve el usuario propietario.
    public User getOwner() { return owner; }
    //Asigna el usuario propietario.
    public void setOwner(User owner) { this.owner = owner; }
    //Devuelve el límite de participantes.
    public int getMaxPlayers() { return maxPlayers; }
    //Cambia el límite de participantes.
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    //Devuelve el estado actual.
    public String getStatus() { return status; }
    //Cambia el estado actual.
    public void setStatus(String status) { this.status = status; }
    //Devuelve la fecha de creación.
    public Instant getCreatedAt() { return createdAt; }
}