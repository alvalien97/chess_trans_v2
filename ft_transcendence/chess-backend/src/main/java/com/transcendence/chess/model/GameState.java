package com.transcendence.chess.model;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private final List<Piece> pieces = new ArrayList<>();
    private int currentColor = 0; // 0 = Blancas, 1 = Negras
    private String status = "PLAYING";
    private Long drawOfferByUserId;
    private Long winnerId;
    private String resultMessage;

    public void initializeBoard() {
        pieces.clear();

        pieces.add(new Rook(1, 0, 0));
        pieces.add(new Knight(1, 1, 0));
        pieces.add(new Bishop(1, 2, 0));
        pieces.add(new Queen(1, 3, 0));
        pieces.add(new King(1, 4, 0));
        pieces.add(new Bishop(1, 5, 0));
        pieces.add(new Knight(1, 6, 0));
        pieces.add(new Rook(1, 7, 0));
        for (int i = 0; i < 8; i++) {
            pieces.add(new Pawn(1, i, 1));
        }

        for (int i = 0; i < 8; i++) {
            pieces.add(new Pawn(0, i, 6));
        }
        pieces.add(new Rook(0, 0, 7));
        pieces.add(new Knight(0, 1, 7));
        pieces.add(new Bishop(0, 2, 7));
        pieces.add(new Queen(0, 3, 7));
        pieces.add(new King(0, 4, 7));
        pieces.add(new Bishop(0, 5, 7));
        pieces.add(new Knight(0, 6, 7));
        pieces.add(new Rook(0, 7, 7));
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    public int getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(int currentColor) {
        this.currentColor = currentColor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDrawOfferByUserId() { return drawOfferByUserId; }
    public void setDrawOfferByUserId(Long drawOfferByUserId) { this.drawOfferByUserId = drawOfferByUserId; }
    public Long getWinnerId() { return winnerId; }
    public void setWinnerId(Long winnerId) { this.winnerId = winnerId; }
    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
}
