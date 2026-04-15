package rivalsanalyzerAI;
import org.opencv.core.Mat;
import org.datavec.image.loader.NativeImageLoader;
import org.nd4j.linalg.api.ndarray.INDArray;

public class FrameConverter {

    public static INDArray toTensor(Mat frame) throws Exception {

        int height = 64;
        int width = 64;
        int channels = 3;

        NativeImageLoader loader = new NativeImageLoader(height, width, channels);

        INDArray image = loader.asMatrix(frame);

        // Normalize pixels
        image = image.div(255.0);

        return image;
    }
}