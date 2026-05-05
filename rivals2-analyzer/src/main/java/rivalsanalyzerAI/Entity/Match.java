package rivalsanalyzerAI.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // links each match to a user
    @Column(name = "user_id")
    private Long userId;

    // stores move counts JSON
    @Column(columnDefinition = "TEXT")
    private String moves;

    // auto timestamp
    private LocalDateTime matchTime = LocalDateTime.now();

    public Match() {
    }

    // ---------------- NEW CONSTRUCTOR ----------------
    public Match(Long userId, String moves) {
        this.userId = userId;
        this.moves = moves;
    }

    // getters/setters

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getMoves() {
        return moves;
    }

    public LocalDateTime getMatchTime() {
        return matchTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setMoves(String moves) {
        this.moves = moves;
    }

    public void setMatchTime(LocalDateTime matchTime) {
        this.matchTime = matchTime;
    }
}