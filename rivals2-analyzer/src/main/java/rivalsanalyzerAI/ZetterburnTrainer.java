package rivalsanalyzerAI;

import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;

import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.io.File;
import java.util.*;

public class ZetterburnTrainer {

	// image dimentions
    static int height = 64;
    static int width = 64;
    static int channels = 3;
    
    //frames used per sequence
    static int sequenceLength = 5;

    //move labels
    static List<String> labels = Arrays.asList(
            "bair","dair","dash attack","dspecial","dstrong","dtilt",
            "fair","fspecial","fstrong","ftilt","grab","jab",
            "nair","nspecial","upair","upspecial","upstrong","utilt"
    );

    public static void main(String[] args) throws Exception {

        File baseDir = new File("src/main/resources/training/zetterburn");
        NativeImageLoader loader = new NativeImageLoader(height, width, channels);

        List<DataSet> datasetList = new ArrayList<>();

        //loop through every move label folder
        for (int labelIndex = 0; labelIndex < labels.size(); labelIndex++) {

            File moveDir = new File(baseDir, labels.get(labelIndex));

            File[] seqFolders = moveDir.listFiles();

            if (seqFolders == null) {
                System.out.println("Missing folder: " + moveDir.getAbsolutePath());
                continue;
            }

            //loop through each sequence folder
            for (File seqFolder : seqFolders) {

                if (!seqFolder.isDirectory()) continue;

                File[] frames = seqFolder.listFiles((d, name) -> name.endsWith(".png"));

                if (frames == null || frames.length < 3) continue;

                Arrays.sort(frames);

                INDArray sequence = Nd4j.zeros(1, channels * height * width, sequenceLength);

                //fill sequence with frame data
                for (int t = 0; t < sequenceLength; t++) {

                    INDArray frame;

                    if (t < frames.length) {
                        frame = loader.asMatrix(frames[t]).div(255.0);
                    } else {
                        frame = Nd4j.zeros(1, channels, height, width);
                    }

                    INDArray flat = frame.reshape(1, channels * height * width);

                    sequence.put(
                            new INDArrayIndex[]{
                                    NDArrayIndex.point(0),
                                    NDArrayIndex.all(),
                                    NDArrayIndex.point(t)
                            },
                            flat
                    );
                }

                INDArray label = Nd4j.zeros(1, labels.size(), sequenceLength);

                //put the label only on the last frame
                label.putScalar(
                	new int[]{0, labelIndex, sequenceLength - 1},
                	1.0
                );

                datasetList.add(new DataSet(sequence, label));
            }
        }

        System.out.println("Loaded sequences: " + datasetList.size());

        //merge samples together
        DataSet allData = DataSet.merge(datasetList);

        // Ai configuration
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                .updater(new Adam(0.001))
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(new LSTM.Builder()
                        .nIn(channels * height * width)
                        .nOut(256)
                        .activation(Activation.TANH)
                        .build())
                .layer(new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX)
                        .nOut(labels.size())
                        .build())
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(config);
        model.init();

        model.setListeners(new ScoreIterationListener(10));

        //train for 10 epoch's
        for (int i = 0; i < 10; i++) {
            model.fit(allData);
            System.out.println("Epoch " + (i + 1) + " complete");
        }

        //save to ZIP file
        File modelFile = new File("models/zetterburn_sequence_model.zip");
        modelFile.getParentFile().mkdirs();

        ModelSerializer.writeModel(model, modelFile, true);

        System.out.println("Model saved");
    }
}