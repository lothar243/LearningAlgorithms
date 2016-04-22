import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by jeff on 4/16/16.
 */
public class NeuralNode implements Serializable {
    final int numInputs;
    final double[] inputWeights;
    double lastOutput;
    double[] lastInputs;
    double lastError;

    public NeuralNode(double[] inputWeights) {
        this.numInputs = inputWeights.length - 1; // the last weight is the bias weight
        this.inputWeights = inputWeights;
    }

    /**
     * Given an input values, calculate the output of this node
     * @param inputValues The inputs
     * @return The value of the output
     */
    public double evaluate(double[] inputValues) {
        lastInputs = inputValues;
        double sum = 0;
        for (int i = 0; i < numInputs; i++) {
            sum += inputWeights[i] * inputValues[i];
        }
        sum += inputWeights[numInputs]; // the bias weight
        lastOutput = sigmoid(sum);
        return lastOutput;
    }

    /**
     * Squashes a double to be between 0 and 1, exclusive
     * @param before a double of any value
     * @return the squashed value
     */
    public static double sigmoid(double before) {
//        System.out.println("Taking sigmoid of " + before);
        return 1.0 / (1 + Math.exp(-before));
    }

    public String toString() {
        return "Node - " + numInputs + " inputs: " + Arrays.toString(inputWeights);
    }

    /**
     * Given a specified target value, determines the error of the most recently performed calculation, as well as how
     * much each upstream node contributed to this error. This error is remembered for an upcoming weight adjustment
     * @param target The target output of the last calculation
     * @return The error from each of the input nodes, used to propagate the error values
     */
    public double[] calcLastError(double target) {
        // used for output nodes
        lastError = lastOutput * (1 - lastOutput) * (target - lastOutput);
        return propagatedErrors();
    }
    public double[] calcLastError(double[][] propagatedErrors, int currentNodeIndex) {
        // used for non-output nodes, the propagated errors must already have been multiplied by the edge weights
        lastError = 0;
        for (int errorIndex = 0; errorIndex < propagatedErrors.length; errorIndex++) {
            lastError += propagatedErrors[errorIndex][currentNodeIndex];
        }
        lastError *= lastOutput * (1 - lastOutput);
        return propagatedErrors();
    }

    /**
     * Uses the value of the current error to determine how much error is propagated to each of the node's inputs
     * @return The error from each of the input nodes, used to propagate the error values
     */
    private double[] propagatedErrors() {
        double[] propagatedErrors = new double[numInputs];
        for (int i = 0; i < numInputs; i++) {
            propagatedErrors[i] = inputWeights[i] * lastError;
        }
        return propagatedErrors;
    }

    /**
     * After the error has been calculated, weights can be adjusted to make the output of the node slightly closer to
     * the target value
     * @param learningRate
     */
    public void updateWeights(double learningRate) {
        for (int i = 0; i < numInputs; i++) {
            inputWeights[i] += learningRate * lastError * lastInputs[i];
        }
        // updating the bias also
        inputWeights[numInputs] += learningRate * lastError;
    }
}
