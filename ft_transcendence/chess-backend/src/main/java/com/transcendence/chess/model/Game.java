// Paquete con las entidades que representan datos del dominio.
package com.transcendence.chess.model;

// Marca temporal absoluta usada para registrar la creación.
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

// Mapea las partidas a la tabla games de la base de datos.
@Entity
@Table(name = "games")
public class Game {

    // Identificador asignado a la partida y usado como clave primaria.
    @Id
    // Limita a 64 caracteres el campo que se guardará en la columna.
    @Column(length = 64)
    private String id;

    // Usuario que juega con blancas; la relación se guarda como clave foránea.
    @ManyToOne
    @JoinColumn(name = "white_player_id")
    private User whitePlayer;

    // Usuario que juega con negras.
    @ManyToOne
    @JoinColumn(name = "black_player_id")
    private User blackPlayer;

    // Usuario ganador, o null si la partida no tiene ganador.
    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    // Usuario que hizo la propuesta de tablas, si existe una pendiente.
    @ManyToOne
    @JoinColumn(name = "draw_offer_by_user_id")
    private User drawOfferer;

    // Estado de la partida; es obligatorio y comienza esperando jugadores.
    @Column(nullable = false)
    private String status = "WAITING";

    // Fecha de creación, obligatoria y no modificable después del alta.
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Callback de JPA que se ejecuta justo antes de insertar la entidad.
    @PrePersist
    void onCreate() {
        // Si nadie asignó una fecha, usa el instante actual como fecha de alta.
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // Devuelve el identificador de la partida.
    public String getId() {
        return id;
    }

    // Asigna el identificador de la partida.
    public void setId(String id) {
        this.id = id;
    }

    // Devuelve el usuario de blancas.
    public User getWhitePlayer() {
        return whitePlayer;
    }

    // Asigna el usuario de blancas.
    public void setWhitePlayer(User whitePlayer) {
        this.whitePlayer = whitePlayer;
    }

    // Devuelve el usuario de negras.
    public User getBlackPlayer() {
        return blackPlayer;
    }

    // Asigna el usuario de negras.
    public void setBlackPlayer(User blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    // Devuelve el ganador, si se ha establecido.
    public User getWinner() { return winner; }
    // Asigna el ganador de la partida.
    public void setWinner(User winner) { this.winner = winner; }
    // Devuelve quién propuso tablas.
    public User getDrawOfferer() { return drawOfferer; }
    // Asigna quién propuso tablas.
    public void setDrawOfferer(User drawOfferer) { this.drawOfferer = drawOfferer; }

    // Devuelve el estado actual de la partida.
    public String getStatus() {
        return status;
    }

    // Actualiza el estado de la partida.
    public void setStatus(String status) {
        this.status = status;
    }

    // Devuelve el instante en que se creó la partida.
    public Instant getCreatedAt() {
        return createdAt;
    }
}
