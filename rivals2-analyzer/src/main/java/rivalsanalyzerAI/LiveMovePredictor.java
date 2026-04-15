package rivalsanalyzerAI;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.opencv.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;


public class LiveMovePredictor {

    public static void main(String[] args) throws Exception {

        int height = 64;
        int width = 64;
        int channels = 3;

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

        Robot robot = new Robot();

        String templatePath = new File(
                "src/main/resources/training/character idles/zetterburnidle/zetterburn_idle.png")
        		.getAbsolutePath();

        BufferedImage templateImg = ImageIO.read(new File(templatePath));
        Mat template = bufferedImageToMat(templateImg);

        if (template == null || template.empty()) {
            throw new RuntimeException("Template failed to load.");
        }

        if (template.empty()) {
            throw new RuntimeException("Template image failed to load.");
        }
        
        Rect lastKnownBox = null;
        int framesSinceSeen = 0;
        
        while (true) {

            Rectangle gameArea = getGameWindowBounds();

            BufferedImage screenshot = robot.createScreenCapture(gameArea);
            Mat screen = bufferedImageToMat(screenshot);

            // Try fresh detection first
            Rect zetterburnBox = detectZetterburn(screen, template);

            // If found, update tracker
            if (zetterburnBox != null) {
                lastKnownBox = zetterburnBox;
                framesSinceSeen = 0;
            }
            // If not found, reuse last known location briefly
            else if (lastKnownBox != null && framesSinceSeen < 10) {
                zetterburnBox = lastKnownBox;
                framesSinceSeen++;
            }
            // Fully lost
            else {
                lastKnownBox = null;
                System.out.println("Zetterburn not found");
                Thread.sleep(300);
                continue;
            }

            // Crop detected / tracked region
            Mat cropped = new Mat(screen, zetterburnBox);
            BufferedImage cropImg = matToBufferedImage(cropped);

            // Convert crop into DL4J input
            NativeImageLoader loader = new NativeImageLoader(height, width, channels);
            INDArray image = loader.asMatrix(cropImg);

            ImagePreProcessingScaler scaler = new ImagePreProcessingScaler(0, 1);
            scaler.transform(image);

            // Run prediction
            INDArray output = model.output(image);

            int predictedClass = output.argMax(1).getInt(0);
            double confidence = output.getDouble(0, predictedClass);

            // Confidence threshold
            if (confidence > 0.75) {
                String move = labels.get(predictedClass);
                System.out.println("Move: " + move +
                        " | confidence: " + String.format("%.2f", confidence));
            } else {
                System.out.println("No confident move detected");
            }

            Thread.sleep(300);
        }
    }

    // ---------------- WINDOW DETECTION ----------------

    public static Rectangle getGameWindowBounds() {
        final Rectangle[] foundRect = { null };

        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            char[] buffer = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, buffer, 512);
            String windowTitle = new String(buffer).trim();

            if (windowTitle.contains("Rivals")) {
                RECT rect = new RECT();
                User32.INSTANCE.GetWindowRect(hwnd, rect);

                foundRect[0] = new Rectangle(
                        rect.left,
                        rect.top,
                        rect.right - rect.left,
                        rect.bottom - rect.top
                );

                return false;
            }

            return true;
        }, null);

        if (foundRect[0] == null) {
            throw new RuntimeException("Could not find any Rivals window.");
        }

        return foundRect[0];
    }

    // ---------------- TEMPLATE MATCHING ----------------

    public static Rect detectZetterburn(Mat screen, Mat template) {

        int resultCols = screen.cols() - template.cols() + 1;
        int resultRows = screen.rows() - template.rows() + 1;

        if (resultCols <= 0 || resultRows <= 0) {
            return null;
        }

        Mat result = new Mat(resultRows, resultCols, CV_32FC1);

        matchTemplate(screen, template, result, TM_CCOEFF_NORMED);

        DoublePointer minVal = new DoublePointer(1);
        DoublePointer maxVal = new DoublePointer(1);
        Point minLoc = new Point();
        Point maxLoc = new Point();

        minMaxLoc(result, minVal, maxVal, minLoc, maxLoc, null);

        if (maxVal.get() < 0.6) {
            return null;
        }

        int x = Math.max(0, maxLoc.x() - 100);
        int y = Math.max(0, maxLoc.y() - 60);

        int w = Math.min(template.cols() + 200 * 2, screen.cols() - x);
        int h = Math.min(template.rows() + 120 * 2, screen.rows() - y);

        return new Rect(x, y, w, h);
    }

    // ---------------- IMAGE CONVERSION ----------------

    public static Mat bufferedImageToMat(BufferedImage img) {

        BufferedImage converted = new BufferedImage(
                img.getWidth(),
                img.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR
        );

        converted.getGraphics().drawImage(img, 0, 0, null);

        byte[] pixels = ((DataBufferByte) converted.getRaster().getDataBuffer()).getData();

        Mat mat = new Mat(img.getHeight(), img.getWidth(), CV_8UC3);
        mat.data().put(pixels);

        return mat;
    }
    
    public static BufferedImage matToBufferedImage(Mat mat) {

        BufferedImage img = new BufferedImage(
                mat.cols(),
                mat.rows(),
                BufferedImage.TYPE_3BYTE_BGR
        );

        byte[] data = new byte[(int) (mat.total() * mat.channels())];
        mat.data().get(data);

        img.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), data);

        return img;
    }
}