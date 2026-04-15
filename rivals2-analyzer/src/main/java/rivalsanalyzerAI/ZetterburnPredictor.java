package rivalsanalyzerAI;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;

public class ZetterburnPredictor {

    public static void main(String[] args) throws Exception {

        int height = 64;
        int width = 64;
        int channels = 3;

        // MUST match folder order used in training
        List<String> labels = Arrays.asList(
                "bair",
                "dair",
                "dash attack",
                "dspecial",
                "dstrong",
                "dtilt",
                "fair",
                "fspecial",
                "fstrong",
                "ftilt",
                "grab",
                "jab",
                "nair",
                "nspecial",
                "upair",
                "upspecial",
                "upstrong",
                "utilt"
        );

        MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(
                new File("src/main/resources/models/zetterburnModel.zip"));

        File imageFile = new File("src/main/resources/test/test_move.png");

        NativeImageLoader loader = new NativeImageLoader(height, width, channels);
        INDArray image = loader.asMatrix(imageFile);

        ImagePreProcessingScaler scaler = new ImagePreProcessingScaler(0, 1);
        scaler.transform(image);

        INDArray output = model.output(image);

        int predictedClass = output.argMax(1).getInt(0);

        for (int i = 0; i < labels.size(); i++) {
            System.out.println(labels.get(i) + ": " + output.getDouble(i));
        }
        System.out.println("Confidence: " + output);
    }
}