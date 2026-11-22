//Paquete con las entidades que representan datos del dominio.
package com.transcendence.chess.model;

//Anotaciones JPA para persistir encuentros y relacionarlos con torneo, jugadores y partida.
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

//Persiste cada encuentro en la tabla tournament_matches.
@Entity
@Table(name = "tournament_matches")
public class TournamentMatch {
    //Clave primaria textual del encuentro.
    @Id
    @Column(length = 36)
    private String id;

    //Torneo al que pertenece este encuentro.
    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    //Número de ronda en la que se disputa.
    @Column(nullable = false)
    private int roundNumber;

    //Posición del encuentro dentro de la ronda.
    @Column(nullable = false)
    private int matchNumber;

    //Usuario que juega con blancas.
    @ManyToOne(optional = false)
    @JoinColumn(name = "white_player_id", nullable = false)
    private User whitePlayer;

    //Usuario que juega con negras.
    @ManyToOne(optional = false)
    @JoinColumn(name = "black_player_id", nullable = false)
    private User blackPlayer;

    //Ganador del encuentro, nulo mientras no se resuelva.
    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    //Partida asociada; cada partida solo puede pertenecer a un encuentro de torneo.
    @OneToOne(optional = false)
    @JoinColumn(name = "game_id", nullable = false, unique = true)
    private Game game;

    //Estado textual del encuentro, inicialmente en juego.
    @Column(nullable = false, length = 16)
    private String status = "PLAYING";

    //Métodos de acceso para que JPA y los servicios lean o actualicen los campos.
    //Devuelve el identificador del encuentro.
    public String getId() { return id; }
    //Asigna el identificador del encuentro.
    public void setId(String id) { this.id = id; }
    //Devuelve el torneo propietario de este encuentro.
    public Tournament getTournament() { return tournament; }
    //Asocia el encuentro a un torneo.
    public void setTournament(Tournament tournament) { this.tournament = tournament; }
    //Devuelve la ronda.
    public int getRoundNumber() { return roundNumber; }
    //Asigna la ronda.
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    //Devuelve el número dentro de la ronda.
    public int getMatchNumber() { return matchNumber; }
    //Asigna el número dentro de la ronda.
    public void setMatchNumber(int matchNumber) { this.matchNumber = matchNumber; }
    //Devuelve el jugador de blancas.
    public User getWhitePlayer() { return whitePlayer; }
    //Asigna el jugador de blancas.
    public void setWhitePlayer(User whitePlayer) { this.whitePlayer = whitePlayer; }
    //Devuelve el jugador de negras.
    public User getBlackPlayer() { return blackPlayer; }
    //Asigna el jugador de negras.
    public void setBlackPlayer(User blackPlayer) { this.blackPlayer = blackPlayer; }
    //Devuelve al ganador, si existe.
    public User getWinner() { return winner; }
    //Asigna al ganador.
    public void setWinner(User winner) { this.winner = winner; }
    //Devuelve la partida asociada.
    public Game getGame() { return game; }
    //Asocia la partida al encuentro.
    public void setGame(Game game) { this.game = game; }
    //Devuelve el estado actual del encuentro.
    public String getStatus() { return status; }
    //Actualiza el estado actual del encuentro.
    public void setStatus(String status) { this.status = status; }
}