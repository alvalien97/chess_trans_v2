package com.transcendence.chess.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tournament_matches")
public class TournamentMatch {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(nullable = false)
    private int roundNumber;

    @Column(nullable = false)
    private int matchNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "white_player_id", nullable = false)
    private User whitePlayer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "black_player_id", nullable = false)
    private User blackPlayer;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    @OneToOne(optional = false)
    @JoinColumn(name = "game_id", nullable = false, unique = true)
    private Game game;

    @Column(nullable = false, length = 16)
    private String status = "PLAYING";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament tournament) { this.tournament = tournament; }
    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public int getMatchNumber() { return matchNumber; }
    public void setMatchNumber(int matchNumber) { this.matchNumber = matchNumber; }
    public User getWhitePlayer() { return whitePlayer; }
    public void setWhitePlayer(User whitePlayer) { this.whitePlayer = whitePlayer; }
    public User getBlackPlayer() { return blackPlayer; }
    public void setBlackPlayer(User blackPlayer) { this.blackPlayer = blackPlayer; }
    public User getWinner() { return winner; }
    public void setWinner(User winner) { this.winner = winner; }
    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}