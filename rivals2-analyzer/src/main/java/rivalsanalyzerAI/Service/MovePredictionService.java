package rivalsanalyzerAI.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import java.util.*;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Arrays;
import java.io.File;
import javax.imageio.ImageIO;

import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.indexing.INDArrayIndex;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.RECT;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.opencv.opencv_core.*;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Service
public class MovePredictionService {

    private final Map<String, Integer> moveCounts = new HashMap<>();

    public Map<String, Integer> getMoveCounts() {
        return moveCounts;
    }

    static int height = 64;
    static int width = 64;
    static int channels = 3;
    static int sequenceLength = 5;

    static List<String> labels = Arrays.asList(
            "bair","dair","dash attack","dspecial","dstrong","dtilt",
            "fair","fspecial","fstrong","ftilt","grab","jab",
            "nair","nspecial","upair","upspecial","upstrong","utilt"
    );

    List<INDArray> predictionHistory = Collections.synchronizedList(new LinkedList<>());
    int SMOOTH_WINDOW = 12;

    String lastMove = "";
    String pendingMove = "";
    int pendingCount = 0;
    final int STABILITY_FRAMES = 2;

    //predictor toggle
    private volatile boolean running = false;

    public void startPredictor() {
        if (!running) {
            running = true;
            new Thread(() -> runPredictor()).start();
        }
    }

    public void stopPredictor() {
        running = false;
    }
    
    public void runPredictor() {
        try {

            MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(
                    new File("src/main/resources/models/zetterburn_sequence_model.zip"));

            NativeImageLoader loader = new NativeImageLoader(height, width, channels);
            Robot robot = new Robot();

            BufferedImage templateImg = ImageIO.read(
                    new File("src/main/resources/training/character idles/zetterburnidle/zetterburn_idle.png")
            );
            Mat template = bufferedImageToMat(templateImg);

            Rect lastKnownBox = null;
            int framesSinceSeen = 0;

            LinkedList<INDArray> frameBuffer = new LinkedList<>();

            while (running) {

                Rectangle gameArea = getGameWindowBounds();
                BufferedImage screenshot = robot.createScreenCapture(gameArea);
                Mat screen = bufferedImageToMat(screenshot);

                Rect currentBox = detectZetterburn(screen, template);

                if (currentBox != null) {
                    lastKnownBox = currentBox;
                    framesSinceSeen = 0;
                } else {
                    framesSinceSeen++;
                }

                Rect boxToUse = currentBox != null ? currentBox : lastKnownBox;

                if (boxToUse == null || framesSinceSeen > 10) {
                    frameBuffer.clear();
                    Thread.sleep(100);
                    continue;
                }

                Mat cropped = new Mat(screen, boxToUse);
                BufferedImage cropImg = matToBufferedImage(cropped);

                INDArray frame = loader.asMatrix(cropImg).div(255.0);
                INDArray flat = frame.reshape(1, channels * height * width);

                frameBuffer.add(flat);
                if (frameBuffer.size() > sequenceLength) {
                    frameBuffer.removeFirst();
                }

                if (frameBuffer.size() == sequenceLength) {

                    INDArray sequence = Nd4j.zeros(1, channels * height * width, sequenceLength);

                    for (int t = 0; t < sequenceLength; t++) {
                        sequence.put(new INDArrayIndex[]{
                                NDArrayIndex.point(0),
                                NDArrayIndex.all(),
                                NDArrayIndex.point(t)
                        }, frameBuffer.get(t));
                    }

                    model.rnnClearPreviousState();
                    INDArray output = model.output(sequence);

                    INDArray lastOutput = output;
                    if (lastOutput.rank() == 3) {
                        lastOutput = lastOutput.tensorAlongDimension(
                                (int) lastOutput.size(2) - 1, 1);
                    }

                    // smoothing w thread protection
                    synchronized (predictionHistory) {
                        predictionHistory.add(lastOutput);

                        if (predictionHistory.size() > SMOOTH_WINDOW) {
                            predictionHistory.remove(0);
                        }
                    }

                    //better logic to stop thread blocking
                    INDArray avg = Nd4j.zeros(labels.size());

                    synchronized (predictionHistory) {
                        for (INDArray p : predictionHistory) {
                            avg.addi(p);
                        }
                    }
                    
                    //fixed prediction history to decrease crash risk
                    int size;
                    synchronized (predictionHistory) {
                        size = predictionHistory.size();
                    }

                    if (size > 0) {
                        avg.divi(size);
                    }

                    int predictedClass = Nd4j.argMax(avg, 0).getInt(0);
                    double confidence = avg.getDouble(predictedClass);

                    if (confidence > 0.06) {
                        String move = labels.get(predictedClass);

                        if (move.equals(pendingMove)) {
                            pendingCount++;
                        } else {
                            pendingMove = move;
                            pendingCount = 1;
                        }

                        if (pendingCount >= STABILITY_FRAMES && !move.equals(lastMove)) {

                            lastMove = move;
                            pendingCount = 0;

                            moveCounts.put(move,
                                    moveCounts.getOrDefault(move, 0) + 1);
                            System.out.println("Move: " + move +
                                    " | confidence: " + String.format("%.3f", confidence) +
                                    " | total: " + moveCounts.get(move));
                        }
                    }
                }
                

                Thread.sleep(30);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Rectangle getGameWindowBounds() {
        final Rectangle[] rect = {null};

        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            char[] buffer = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, buffer, 512);
            String title = new String(buffer).trim();

            if (title.contains("Rivals")) {
                RECT r = new RECT();
                User32.INSTANCE.GetWindowRect(hwnd, r);

                rect[0] = new Rectangle(
                        r.left,
                        r.top,
                        r.right - r.left,
                        r.bottom - r.top
                );
                return false;
            }
            return true;
        }, null);

        if (rect[0] == null) throw new RuntimeException("Game window not found");
        return rect[0];
    }

    public Rect detectZetterburn(Mat screen, Mat template) {
        int resultCols = screen.cols() - template.cols() + 1;
        int resultRows = screen.rows() - template.rows() + 1;

        if (resultCols <= 0 || resultRows <= 0) return null;

        Mat result = new Mat(resultRows, resultCols, CV_32FC1);
        matchTemplate(screen, template, result, TM_CCOEFF_NORMED);

        DoublePointer minVal = new DoublePointer(1);
        DoublePointer maxVal = new DoublePointer(1);
        Point minLoc = new Point();
        Point maxLoc = new Point();

        minMaxLoc(result, minVal, maxVal, minLoc, maxLoc, null);

        if (maxVal.get() < 0.6) return null;

        return new Rect(
                Math.max(0, maxLoc.x() - 100),
                Math.max(0, maxLoc.y() - 60),
                Math.min(template.cols() + 200, screen.cols()),
                Math.min(template.rows() + 120, screen.rows())
        );
    }

    public Mat bufferedImageToMat(BufferedImage img) {
        BufferedImage converted = new BufferedImage(
                img.getWidth(), img.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR);

        converted.getGraphics().drawImage(img, 0, 0, null);

        byte[] pixels = ((DataBufferByte)
                converted.getRaster().getDataBuffer()).getData();

        Mat mat = new Mat(img.getHeight(), img.getWidth(), CV_8UC3);
        mat.data().put(pixels);
        return mat;
    }

    public BufferedImage matToBufferedImage(Mat mat) {
        BufferedImage img = new BufferedImage(
                mat.cols(), mat.rows(),
                BufferedImage.TYPE_3BYTE_BGR);

        byte[] data = new byte[(int) (mat.total() * mat.channels())];
        mat.data().get(data);

        img.getRaster().setDataElements(0, 0,
                mat.cols(), mat.rows(), data);

        return img;
    }
}