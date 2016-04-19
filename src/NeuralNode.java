import java.util.Arrays;

/**
 * Created by jeff on 4/16/16.
 */
public class NeuralNode {
    final int numInputs;
    final double[] inputWeights;
    double lastOutput;
    double[] lastInputs;
    double lastError;

    public NeuralNode(double[] inputWeights) {
        this.numInputs = inputWeights.length - 1; // one of them is the bias weight
        this.inputWeights = inputWeights;
    }

    public double evaluate(double[] inputValues) {
        lastInputs = inputValues;
        double sum = 0;
        for (int i = 0; i < numInputs; i++) {
            sum += inputWeights[i] * inputValues[i];
//            System.out.println("Input of " + inputValues[i] + ", sum of " + sum);
        }
        sum += inputWeights[numInputs]; // the bias weight
//        System.out.println("After the bias, the sum is " + sum);
        lastOutput = sigmoid(sum);
//        System.out.println("output: " + lastOutput);
        return lastOutput;
    }

    public static double sigmoid(double before) {
//        System.out.println("Taking sigmoid of " + before);
        return 1.0 / (1 + Math.exp(-before));
    }

    public String toString() {
        return "Node - " + numInputs + " inputs: " + Arrays.toString(inputWeights);
    }

    public double[] calcLastError(double target) {
        // used for output nodes
        lastError = lastOutput * (1 - lastOutput) * (target - lastOutput);
        return propagatedErrors();
    }
    public double[] calcLastError(double[][] propagatedErrors, int currentNodeIndex) {
        // used for non-output nodes, the propagated errors must already have been multiplied by the edge weights
//        System.out.println("Current weights " + Arrays.toString(inputWeights));
        lastError = 0;
        for (int errorIndex = 0; errorIndex < propagatedErrors.length; errorIndex++) {
            lastError += propagatedErrors[errorIndex][currentNodeIndex];
        }
        lastError *= lastOutput * (1 - lastOutput);
        return propagatedErrors();
    }
    
    private double[] propagatedErrors() {
        double[] propagatedErrors = new double[numInputs];
        for (int i = 0; i < numInputs; i++) {
            propagatedErrors[i] = inputWeights[i] * lastError;
        }
        return propagatedErrors;
    }

    public void updateWeights(double learningRate) {
        for (int i = 0; i < numInputs; i++) {
            inputWeights[i] += learningRate * lastError * lastInputs[i];
        }
        // updating the bias also
        inputWeights[numInputs] += learningRate * lastError;
    }
}
