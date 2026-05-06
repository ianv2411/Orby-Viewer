package rivalsanalyzerAI.Controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import rivalsanalyzerAI.Service.MovePredictionService;
import rivalsanalyzerAI.Repository.MatchRepository;
import rivalsanalyzerAI.Entity.Match;

import java.util.Map;

@RestController
@RequestMapping("/api/moves")
@CrossOrigin(
		origins = "http://localhost:3000",
		methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class MoveController {

    private final MovePredictionService service;
    private final MatchRepository matchRepository;

    public MoveController(
            MovePredictionService service,
            MatchRepository matchRepository) {

        this.service = service;
        this.matchRepository = matchRepository;
    }

    @GetMapping
    public Map<String, Integer> getMoves() {
        return service.getMoveCounts();
    }
    @PostMapping("/start")
    public void start() {
        service.startPredictor();
    }

    @PostMapping("/stop")
    public void stop() {
        service.stopPredictor();
    }
    
    @GetMapping("/history/{userId}")
    public List<Match> getHistory(@PathVariable Long userId) {
        return matchRepository.findByUserId(userId);
    }
}