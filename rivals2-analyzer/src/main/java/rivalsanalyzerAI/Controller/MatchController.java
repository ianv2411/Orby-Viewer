package rivalsanalyzerAI.Controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import rivalsanalyzerAI.Entity.Match;
import rivalsanalyzerAI.Repository.MatchRepository;

@RestController
@RequestMapping("/api/moves")
@CrossOrigin(origins = "http://localhost:5173")
public class MatchController {

    private final MatchRepository matchRepository;

    public MatchController(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }
}