package rivalsanalyzerAI;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class SimpleModel {

    public static MultiLayerNetwork createModel() {

        MultiLayerNetwork model = new MultiLayerNetwork(
            new NeuralNetConfiguration.Builder()
                .list()
                .layer(new DenseLayer.Builder()
                        .nIn(64 * 64 * 3)   // input size
                        .nOut(100)
                        .activation(Activation.RELU)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .nIn(100)
                        .nOut(3)            // pretend: 3 move types
                        .activation(Activation.SOFTMAX)
                        .build())
                .build()
        );

        model.init();
        return model;
    }
}