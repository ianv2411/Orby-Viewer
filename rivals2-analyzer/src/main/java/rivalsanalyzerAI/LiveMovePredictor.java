package rivalsanalyzerAI;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;

import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.*;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.opencv.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class LiveMovePredictor {

    static int height = 64;
    static int width = 64;
    static int numFrames = 3;

    public static void main(String[] args) throws Exception {

        List<String> labels = Arrays.asList(
            "bair","dair","dash attack","dspecial","dstrong","dtilt",
            "fair","fspecial","fstrong","ftilt","grab","jab",
            "nair","nspecial","upair","upspecial","upstrong","utilt"
        );

        MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(
                new File("src/main/resources/models/zetterburnModel.zip"));

        Robot robot = new Robot();

        NativeImageLoader loader = new NativeImageLoader(height, width, 3);
        ImagePreProcessingScaler scaler = new ImagePreProcessingScaler(0, 1);

        LinkedList<BufferedImage> frameBuffer = new LinkedList<>();

        String lastMove = "";
        int stableCount = 0;

        while (true) {

            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenshot = robot.createScreenCapture(screenRect);

            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            resized.getGraphics().drawImage(screenshot, 0, 0, width, height, null);

            frameBuffer.add(resized);

            if (frameBuffer.size() > numFrames)
                frameBuffer.removeFirst();

            if (frameBuffer.size() < numFrames)
                continue;

            INDArray stacked = Nd4j.create(1, 3 * numFrames, height, width);

            for (int i = 0; i < numFrames; i++) {
                INDArray frame = loader.asMatrix(frameBuffer.get(i));
                scaler.transform(frame);

                stacked.put(
                        new INDArrayIndex[]{
                                NDArrayIndex.point(0),
                                NDArrayIndex.interval(i * 3, (i + 1) * 3),
                                NDArrayIndex.all(),
                                NDArrayIndex.all()
                        },
                        frame
                );
            }

            INDArray output = model.output(stacked);

            int predicted = output.argMax(1).getInt(0);
            double confidence = output.getDouble(0, predicted);

            if (confidence > 0.80) {
                String move = labels.get(predicted);

                if (move.equals(lastMove)) {
                    stableCount++;
                } else {
                    stableCount = 0;
                }

                if (stableCount >= 2) {
                    System.out.println("Move: " + move + " | confidence: " + String.format("%.2f", confidence));
                }

                lastMove = move;
            }

            Thread.sleep(100);
        }
    }
}