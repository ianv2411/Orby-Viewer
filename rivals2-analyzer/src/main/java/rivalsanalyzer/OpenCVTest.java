package rivalsanalyzer;

import nu.pattern.OpenCV;
import org.opencv.core.Core;

public class OpenCVTest {
    public static void main(String[] args) {
        OpenCV.loadLocally();
        System.out.println("OpenCV loaded: " + Core.VERSION);
    }
}