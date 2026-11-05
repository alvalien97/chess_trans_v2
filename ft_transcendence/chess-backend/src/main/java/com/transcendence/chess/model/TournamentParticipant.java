// Paquete con las entidades que representan datos del dominio.
package com.transcendence.chess.model;

// Anotaciones JPA para persistir participantes y sus relaciones.
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// Representa una inscripción en la tabla tournament_participants.
@Entity
// Impide inscribir dos veces al mismo usuario en el mismo torneo.
@Table(name = "tournament_participants", uniqueConstraints =
        @UniqueConstraint(columnNames = {"tournament_id", "user_id"}))
public class TournamentParticipant {
    // Clave primaria numérica asignada por la base de datos.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Torneo en el que participa el usuario.
    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    // Usuario inscrito en el torneo.
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Posición inicial asignada al participante para organizar el cuadro.
    @Column(nullable = false)
    private int seed;

    // Métodos de acceso para consultar y modificar la inscripción.
    // Devuelve la clave primaria de la inscripción.
    public Long getId() { return id; }
    // Devuelve el torneo de la inscripción.
    public Tournament getTournament() { return tournament; }
    // Asocia la inscripción a un torneo.
    public void setTournament(Tournament tournament) { this.tournament = tournament; }
    // Devuelve el usuario inscrito.
    public User getUser() { return user; }
    // Asigna el usuario inscrito.
    public void setUser(User user) { this.user = user; }
    // Devuelve la posición inicial asignada.
    public int getSeed() { return seed; }
    // Asigna la posición inicial.
    public void setSeed(int seed) { this.seed = seed; }
}