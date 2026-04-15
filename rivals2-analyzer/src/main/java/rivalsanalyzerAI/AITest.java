package rivalsanalyzerAI;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;

public class AITest {

    public static void main(String[] args) throws Exception {

        // Load OpenCV
        OpenCV.loadLocally();

        VideoCapture cap = new VideoCapture(0);

        if (!cap.isOpened()) {
            System.out.println("Camera not found");
            return;
        }

        MultiLayerNetwork model = SimpleModel.createModel();

        Mat frame = new Mat();

        System.out.println("Capturing frame...");

        cap.read(frame);

        if (!frame.empty()) {

            INDArray input = FrameConverter.toTensor(frame);

            // Flatten input
            input = input.reshape(1, 64 * 64 * 3);

            INDArray output = model.output(input);

            System.out.println("Prediction: " + output);
        }

        cap.release();
    }
}