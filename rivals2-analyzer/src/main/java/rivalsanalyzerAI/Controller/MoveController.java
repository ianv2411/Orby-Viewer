package rivalsanalyzerAI.Controller;

import org.springframework.web.bind.annotation.*;

import rivalsanalyzerAI.Service.MovePredictionService;

import java.util.Map;

@RestController
@RequestMapping("/api/moves")
@CrossOrigin(
		origins = "http://localhost:3000",
		methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class MoveController {

    private final MovePredictionService service;

    public MoveController(MovePredictionService service) {
        this.service = service;
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
}