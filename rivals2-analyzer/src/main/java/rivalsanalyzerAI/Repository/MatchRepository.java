package rivalsanalyzerAI.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import rivalsanalyzerAI.Entity.Match;


public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByUserId(Long userId);
}