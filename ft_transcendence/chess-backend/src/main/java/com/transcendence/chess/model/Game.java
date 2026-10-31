package com.transcendence.chess.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "games")
public class Game {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne
    @JoinColumn(name = "white_player_id")
    private User whitePlayer;

    @ManyToOne
    @JoinColumn(name = "black_player_id")
    private User blackPlayer;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    @ManyToOne
    @JoinColumn(name = "draw_offer_by_user_id")
    private User drawOfferer;

    @Column(nullable = false)
    private String status = "WAITING";

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getWhitePlayer() {
        return whitePlayer;
    }

    public void setWhitePlayer(User whitePlayer) {
        this.whitePlayer = whitePlayer;
    }

    public User getBlackPlayer() {
        return blackPlayer;
    }

    public void setBlackPlayer(User blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public User getWinner() { return winner; }
    public void setWinner(User winner) { this.winner = winner; }
    public User getDrawOfferer() { return drawOfferer; }
    public void setDrawOfferer(User drawOfferer) { this.drawOfferer = drawOfferer; }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
