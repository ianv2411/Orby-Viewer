package rivalsanalyzerAI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import rivalsanalyzerAI.Service.MovePredictionService;

@SpringBootApplication
public class AnalyzerApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(AnalyzerApplication.class, args);
    }

    @Bean
    public CommandLineRunner startPredictor(MovePredictionService service) {
        return args -> {
            Thread t = new Thread(() -> service.runPredictor());
            t.setDaemon(true); 
            t.start();
        };
    }
}