import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by jeff on 4/16/16.
 */
public class NeuralNet {
    /**
     * Print some output to help guide the user on the correct use of the command line arguments
     */
    public static void printHelpString() {
        final String helpString = "\nUsage: ./NeuralNet.sh -t trainingData.csv <optional arguments>\n\n" +
                "Bayesian Network Implementation: Uses the K2 Algorithm" +
                "Optional Arguments: \n" +
                "\t-x NUM\n" +
                "\t\tn-fold cross validation\n" +
                "\t-T testData.csv\n" +
                "\t\tSpecify a file to use as test data\n" +
                "\t-e NUM\n" +
                "\t\tNumber of epochs to run for\n" +
                "\t-l <layer structure>\t" +
                "\t\tSpecify layer structure of network (example -l \"3 2\" would have 3 nodes in the first layer, 2 nodes in the second layer with a number of inputs and outputs automatically calculated)\n" +
                "\t-seed NUM\n" +
                "\t\tSpecify the seed to use for the random numbers\n" +
                "\t-v\n" +
                "\t\tVerbose - show information of each fold\n" +
                "\t-balance\n" +
                "\t\tDuplicate existing data points so that all classifications are equally likely\n";

        System.out.println(helpString);
        System.exit(1);

    }


    public static void main(String[] args) {
        boolean verbose = false;
        String trainingDataFile = null;
        String testDataFile = null;
        int crossFoldNumFolds = -1;
        boolean balanceClasses = false;
        int numEpochs = 1;
        ArrayList<Integer> layerStructure = new ArrayList<>();
        layerStructure.add(3);
        long seed = 0;
        boolean seedSpecified = false;
        double learningRate = .05;

        // read in optional arguments
        try {
            for (int argNum = 0; argNum < args.length; argNum++) {
                switch (args[argNum]) {
                    case "-t":
                        trainingDataFile = args[++argNum];
                        System.out.println("Using training data file: " + trainingDataFile);
                        break;
                    case "-T":
                        testDataFile = args[++argNum];
                        System.out.println("Using test data file: " + testDataFile);
                        break;
                    case "-seed":
                        seed = Long.parseLong(args[++argNum]);
                        seedSpecified = true;
                        break;
                    case "-v":
                    case "--verbose":
                        verbose = true;
                        break;
                    case "-x":
                        crossFoldNumFolds = Integer.parseInt(args[++argNum]);
                        break;
                    case "-balance":
                        balanceClasses = true;
                        break;
                    case "-e":
                        numEpochs = Integer.parseInt(args[++argNum]);
                        break;
                    case "-rate":
                        learningRate = Double.parseDouble(args[++argNum]);
                        break;
                    case "-l":
                        layerStructure = new ArrayList<>();
                        String[] layerStrings = args[++argNum].split(" ");
                        for(String layerString:layerStrings) {
                            layerStructure.add(Integer.parseInt(layerString));
                        }
                        break;
                    case "-h":
                    case "-help":
                        printHelpString();
                        break;
                    default:
                        System.out.println("Unknown argument encountered: " + args[argNum] + " - use -h for help");
                        System.exit(0);
                }
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            printHelpString();
            System.exit(0);
        }
        if(trainingDataFile == null) {
            System.out.println("Training data must be specified");
            System.exit(0);
        }
        if((testDataFile == null && crossFoldNumFolds == -1) || (testDataFile != null && crossFoldNumFolds != -1)) {
            System.out.println("Either a test data file or a number of folds for cross fold must be specified");
            System.exit(0);
        }

        Data data = new Data();
        if(!FileIO.readFromFile(trainingDataFile, data)) {
            System.out.println("Error reading training data. Quitting");
            System.exit(1);
        }

        int numInputs = data.numAttributes;
        int numOutputs = data.classifications.size();
        layerStructure.add(0, numInputs);
        layerStructure.add(numOutputs);

        System.out.println("Performing " + numEpochs + " epochs with a network structure of " + layerStructure.toString()
                + ", " + numInputs + " inputs and " + numOutputs + " outputs");

        if(balanceClasses) {
            data.bootstrapToBalanceClasses();
        }
        Random generator;
        if(seedSpecified) {
            generator = new Random(seed);
        }
        else {
            generator = new Random();
        }


        int numClassifications = data.classifications.size();

        if(crossFoldNumFolds != -1) {
            data.initializeDataForCrossFoldValidation(crossFoldNumFolds);
            double overallAccuracy = 0;
            for (int foldNumber = 0; foldNumber < crossFoldNumFolds; foldNumber++) {
                NeuralNet net = createNeuralNet(layerStructure, generator);
                net.learningRate = learningRate;
                if(verbose) {
                    System.out.println("Fold " + foldNumber + ", Beginning neural net structure:\n" + net.toString());
                }
                Data trainingData = data.getCrossFoldTrainingData(foldNumber);
                Data testData = data.getCrossFoldTestData(foldNumber);
                for (int i = 0; i < numEpochs; i++) {
                    if(i % 100 == 0) {
                        int[][] confusionMatrix = new int[numClassifications][numClassifications];
                        double accuracy = determineAccuracy(testData, net, confusionMatrix);
                        if(verbose) System.out.println("Epoch " + i + ": accuracy " + accuracy);
                    }
                    runEpoch(trainingData, net);
                }
                int[][] confusionMatrix = new int[numClassifications][numClassifications];
                double accuracy = determineAccuracy(testData, net, confusionMatrix);
                overallAccuracy += accuracy;
                System.out.println("Accuracy of fold " + foldNumber + ", " + accuracy);
                if(verbose) {
                    System.out.println("Fold " + foldNumber + ", Ending neural net structure:\n" + net.toString());
                    System.out.println("Confusion matrix: \n" + MyTools.confusionMatrixString(trainingData.classifications, confusionMatrix) + "\n");
                    System.out.println("----------------------------------------------------------------------\n");
                }
            }
            System.out.println("Overall accuracy of all folds: " + (overallAccuracy / crossFoldNumFolds));
        }


    }

    public static void runEpoch(Data trainingData, NeuralNet net) {
        for(DataPoint point: trainingData.dataPoints) {
            net.feedForward(point);
            double[] outputs = new double[trainingData.classifications.size()];
            Arrays.fill(outputs, 0);
            outputs[point.classificationIndex] = 1;
            net.backPropagate(outputs);
        }
    }

    public static double determineAccuracy(Data testData, NeuralNet net, int[][] confusionMatrix) {
        int numCorrectPredictions = 0;
        for(DataPoint point: testData.dataPoints) {
            double[] predictiveValues = net.feedForward(point);
            double highestValue = 0;
            int bestPrediction = -1;
            for (int i = 0; i < predictiveValues.length; i++) {
                if(predictiveValues[i] > highestValue) {
                    highestValue = predictiveValues[i];
                    bestPrediction = i;
                }
            }
            confusionMatrix[point.classificationIndex][bestPrediction]++;
            if(point.classificationIndex == bestPrediction) numCorrectPredictions++;
        }
        return (double)numCorrectPredictions / testData.dataPoints.size();
    }

    private static NeuralNet createNeuralNet(ArrayList<Integer> layerStructure, Random generator) {
//        System.out.println("structure: " + layerStructure.toString());
        double[][][] weights = new double[layerStructure.size() - 1][][];
        int numLayers = layerStructure.size() - 1;
        for (int layerIndex = 0; layerIndex < numLayers; layerIndex++) {
            int numWeights = layerStructure.get(layerIndex) + 1;
            int numNodesInLayer = layerStructure.get(layerIndex + 1);
            weights[layerIndex] = new double[numNodesInLayer][];
            for (int nodeIndex = 0; nodeIndex < numNodesInLayer; nodeIndex++) {
                weights[layerIndex][nodeIndex] = new double[numWeights];
                for (int inputIndex = 0; inputIndex < numWeights; inputIndex++) {
                    weights[layerIndex][nodeIndex][inputIndex] = .05 * generator.nextDouble();
                }
//                System.out.println("layer " + layerIndex + ", node " + nodeIndex + ": " + Arrays.toString(weights[layerIndex][nodeIndex]));
            }
        }
        return new NeuralNet(weights);
    }

    //------------------------------------------------------------------------------------------------------------------

    final int numLayers;
    final NeuralNode[][] nodes;
    double learningRate = .01;

    public NeuralNet() {
        numLayers = 0;
        nodes = new NeuralNode[0][0];
    }
    public NeuralNet(double[][][] inputWeights) {
        numLayers = inputWeights.length;
        nodes = new NeuralNode[numLayers][];
        for (int i = 0; i < numLayers; i++) {
            int numNodesInLayer = inputWeights[i].length;
            nodes[i] = new NeuralNode[numNodesInLayer];
            for (int j = 0; j < numNodesInLayer; j++) {
                nodes[i][j] = new NeuralNode(inputWeights[i][j]);
            }
        }
    }
    public String toString() {
        String output = "";
        for (int i = 0; i < nodes.length; i++) {
            output += Arrays.toString(nodes[i]) + "\n";
        }
        return output;
    }
    public double[] feedForward(DataPoint point) {
        int numAttributes = point.attributes.length;
        double[] inputs = new double[numAttributes];
        for (int attIndex = 0; attIndex < numAttributes; attIndex++) {
            inputs[attIndex] = point.attributes[attIndex].getDouble();
        }
        return feedForward(inputs);
    }
    public double[] feedForward(double[] inputValues) {
        double[] currentInputs = Arrays.copyOf(inputValues, inputValues.length);
        double[] outputs = null;

        for (int layer = 0; layer < numLayers; layer++) {
            int numNodesInLayer = nodes[layer].length;
            outputs = new double[numNodesInLayer];
            for (int i = 0; i < numNodesInLayer; i++) {
                outputs[i] = nodes[layer][i].evaluate(currentInputs);
            }
            currentInputs = outputs;
        }
        return outputs;
    }

    public void backPropagate(double[] targets) {
//        System.out.println("BackPropagating: " + Arrays.toString(targets));
        // first determine all of the error values, starting with the output layer
        int layerIndex = nodes.length-1;
        int numNodesInLayer = nodes[layerIndex].length;
        double[][] downstreamErrors = new double[numNodesInLayer][];
        for (int nodeIndex = 0; nodeIndex < numNodesInLayer; nodeIndex++) {
            downstreamErrors[nodeIndex] = nodes[layerIndex][nodeIndex].calcLastError(targets[nodeIndex]);
        }
        double[][] upstreamErrors;
        for(layerIndex = nodes.length - 2; layerIndex >= 0; layerIndex--) {
            numNodesInLayer = nodes[layerIndex].length;
            upstreamErrors = new double[numNodesInLayer][];
            for (int nodeIndex = 0; nodeIndex < numNodesInLayer; nodeIndex++) {
                upstreamErrors[nodeIndex] = nodes[layerIndex][nodeIndex].calcLastError(downstreamErrors, nodeIndex);
            }
            downstreamErrors = upstreamErrors;
        }
        for (layerIndex = 0; layerIndex < nodes.length; layerIndex++) {
            for (int nodeIndex = 0; nodeIndex < nodes[layerIndex].length; nodeIndex++) {
                nodes[layerIndex][nodeIndex].updateWeights(learningRate);
            }
        }
    }


}
