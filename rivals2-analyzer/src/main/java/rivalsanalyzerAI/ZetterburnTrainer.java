package rivalsanalyzerAI;

import java.io.File;
import java.util.Random;

import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.datavec.image.transform.FlipImageTransform;
import org.datavec.image.transform.RotateImageTransform;

import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;

import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class ZetterburnTrainer {

    public static void main(String[] args) throws Exception {

        int height = 64;
        int width = 64;
        int channels = 3;
        int batchSize = 8;
        int numClasses = 18; 

        File trainDir = new File("src/main/resources/training/zetterburn");

        FileSplit fileSplit = new FileSplit(
                trainDir,
                NativeImageLoader.ALLOWED_FORMATS,
                new Random(123));

        ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();

        ImageRecordReader reader =
                new ImageRecordReader(height, width, channels, labelMaker);

        reader.initialize(fileSplit);
        FlipImageTransform flip = new FlipImageTransform(1);
        RotateImageTransform rotate = new RotateImageTransform(new Random(123), 10);
        System.out.println(reader.getLabels());
        DataSetIterator dataIter =
                new RecordReaderDataSetIterator(reader, batchSize, 1, numClasses);

        ImagePreProcessingScaler scaler = new ImagePreProcessingScaler(0, 1);
        scaler.fit(dataIter);
        dataIter.setPreProcessor(scaler);

        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                .seed(123)
                .updater(new Adam(0.0005))
                .weightInit(WeightInit.XAVIER)
                .list()

                // Block 1
                .layer(new ConvolutionLayer.Builder(3, 3)
                        .nIn(channels)
                        .stride(1, 1)
                        .padding(1, 1)
                        .nOut(32)
                        .activation(Activation.RELU)
                        .build())

                .layer(new ConvolutionLayer.Builder(3, 3)
                        .stride(1, 1)
                        .padding(1, 1)
                        .nOut(32)
                        .activation(Activation.RELU)
                        .build())

                .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())

                // Block 2
                .layer(new ConvolutionLayer.Builder(3, 3)
                        .stride(1, 1)
                        .padding(1, 1)
                        .nOut(64)
                        .activation(Activation.RELU)
                        .build())

                .layer(new ConvolutionLayer.Builder(3, 3)
                        .stride(1, 1)
                        .padding(1, 1)
                        .nOut(64)
                        .activation(Activation.RELU)
                        .build())

                .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())

                // Dense classifier
                .layer(new DenseLayer.Builder()
                        .nOut(256)
                        .activation(Activation.RELU)
                        .dropOut(0.5)
                        .build())

                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .nOut(numClasses)
                        .activation(Activation.SOFTMAX)
                        .build())

                .setInputType(InputType.convolutional(height, width, channels))
                .build();
        
        MultiLayerNetwork model = new MultiLayerNetwork(config);
        model.init();

        for (int epoch = 0; epoch < 15; epoch++) {

            reader.initialize(fileSplit);
            dataIter.reset();
            model.fit(dataIter);

            reader.initialize(fileSplit, new FlipImageTransform(1));
            dataIter.reset();
            model.fit(dataIter);

            reader.initialize(fileSplit, new RotateImageTransform(new Random(), 8));
            dataIter.reset();
            model.fit(dataIter);

            System.out.println("Epoch " + (epoch + 1) + " complete");
        }

        System.out.println("Training complete");
        
        File modelFile = new File("src/main/resources/models/zetterburnModel.zip");
        model.save(modelFile, true);

        System.out.println("Model saved");
    }
}