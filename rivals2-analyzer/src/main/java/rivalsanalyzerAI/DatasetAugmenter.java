package rivalsanalyzerAI;

import java.io.File;
import java.util.Random;

import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.transform.FlipImageTransform;
import org.datavec.image.transform.RotateImageTransform;
import org.datavec.image.transform.ScaleImageTransform;
import org.datavec.image.transform.WarpImageTransform;

public class DatasetAugmenter {

    public static void main(String[] args) throws Exception {

        File baseDir = new File("src/main/resources/training/zetterburn");

        Random rng = new Random(123);

        FlipImageTransform flip = new FlipImageTransform(1);
        RotateImageTransform rotate = new RotateImageTransform(rng, 10);
        ScaleImageTransform scale = new ScaleImageTransform(rng, 0.9f);
        WarpImageTransform warp = new WarpImageTransform(rng, 10);

        System.out.println("Transforms ready");
        System.out.println("Flip: left/right");
        System.out.println("Rotate: ±10 degrees");
        System.out.println("Scale: zoom");
        System.out.println("Warp: slight motion distortion");

        System.out.println("Apply these during training iterator stage");
    }
}