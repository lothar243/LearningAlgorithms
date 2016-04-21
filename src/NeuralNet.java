import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by jeff on 4/16/16.
 */
public class NeuralNet implements Serializable {
    enum GraphType {SquaredError, Accuracy, SquaredErrorOutputOnly}
    static GraphType graphType = GraphType.SquaredError;
    /**
     * Print some output to help guide the user on the correct use of the command line arguments
     */
    public static void printHelpString() {
        final String helpString = "\nUsage: ./NeuralNet.sh <optional arguments>\n\n" +
                "Artificial Neural Network Implementation" +
                "Arguments: \n" +
                "--Create or load the network--\n" +
                "\t-t trainingData.ser\n" +
                "\t\tCreate a new from specific training data\n" +
                "\t-balance\n" +
                "\t\tDuplicate existing data points so that all classifications are equally likely\n" +
                "\t-loadNet net.csv\n" +
                "\t\tLoad a previously saved network\n\n" +

                "--What to do with the Neural Network--\n" +
                "\t-x NUM\n" +
                "\t\tn-fold cross validation\n" +
                "\t-T testData.ser\n" +
                "\t\tSpecify a file to use as test data\n" +
                "\t-saveNet net.csv\n" +
                "\t\tSave the network to a file\n\n" +

                "\t-e NUM\n" +
                "\t\tNumber of epochs to run for\n" +
                "\t-l <layer structure>\t" +
                "\t\tSpecify layer structure of network (example -l \"3 2\" would have 3 nodes in the first layer, 2 nodes in the second layer with a number of inputs and outputs automatically calculated)\n" +
                "\t-seed NUM\n" +
                "\t\tSpecify the seed to use for the random numbers\n" +
                "\t-v\n" +
                "\t\tVerbose - show information of each fold\n";

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
        int numEpochsPerUpdate = 100;
        String saveNetFilename = null;
        String loadNetFilename = null;

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
                        String[] layerStrings = args[++argNum].split(" ");
                        for(String layerString:layerStrings) {
                            layerStructure.add(Integer.parseInt(layerString));
                        }
                        break;
                    case "-updateSize":
                        numEpochsPerUpdate = Integer.parseInt(args[++argNum]);
                        break;
                    case "-h":
                    case "-help":
                        printHelpString();
                        break;
                    case "-saveNet":
                        saveNetFilename = args[++argNum];
                        break;
                    case "-loadNet":
                        loadNetFilename = args[++argNum];
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

        // ensure either the trainingData file is specified or the neural network is going to be loaded
        if(trainingDataFile == null && loadNetFilename == null || trainingDataFile != null && loadNetFilename != null) {
            System.out.println("Either training data (with -t) or an existing net must be specified (with -loadNet)");
            System.exit(0);
        }
        // ensure exactly one method of getting test data is specified
        if((testDataFile == null && crossFoldNumFolds == -1) || (testDataFile != null && crossFoldNumFolds != -1)) {
            System.out.println("Either a test data file or a number of folds for cross fold must be specified");
            System.exit(0);
        }
        if(loadNetFilename != null) {
            if(testDataFile == null) {
                System.out.println("When loading a net, you must also specify a test file");
                System.exit(2);
            }
            NeuralNet net = FileIO.readNet(loadNetFilename);
            System.out.println("Loaded network: " + net.toString());
            Data testData = new Data();
            FileIO.readFromFile(testDataFile, testData);
            int numClassifications = testData.classifications.size();
            int[][] confusionMatrix = new int[numClassifications][numClassifications];
            double accuracy = determineAccuracy(testData, net, confusionMatrix);
            System.out.println("Confusion matrix: \n" + MyTools.confusionMatrixString(testData.classifications, confusionMatrix) + "\n");
            System.out.println("Accuracy: " + accuracy);
            System.exit(0);

        }
        if(saveNetFilename != null) {
            if(crossFoldNumFolds != -1) {
                System.out.println("It is unclear which neural net should be saved with cross fold validation");
                System.exit(3);
            }
        }


        Data data = new Data();
        if(trainingDataFile != null && !FileIO.readFromFile(trainingDataFile, data)) {
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

        ArrayList<String[]> accuracyOutput = new ArrayList<>();

        int numClassifications = data.classifications.size();

        if(crossFoldNumFolds != -1) {
            data.initializeDataForCrossFoldValidation(crossFoldNumFolds);
            double overallAccuracy = 0;
            for (int foldNumber = 0; foldNumber < crossFoldNumFolds; foldNumber++) {
                NeuralNet net = createNeuralNet(layerStructure, generator);
                if(verbose) {
                    System.out.println("Fold " + foldNumber + ", Beginning neural net structure:\n" + net.toString());
                }
                net.learningRate = learningRate;
                Data trainingData = data.getCrossFoldTrainingData(foldNumber);
                Data testData = data.getCrossFoldTestData(foldNumber);
                double accuracy = trainAndTest(trainingData, testData, numEpochs, net, verbose, numEpochsPerUpdate,
                        numClassifications, accuracyOutput, foldNumber);
                overallAccuracy += accuracy;
                System.out.println("Accuracy of fold " + foldNumber + ", " + accuracy);
                if(verbose) {
                    System.out.println("Fold " + foldNumber + ", Ending neural net structure:\n" + net.toString());

                    System.out.println("----------------------------------------------------------------------\n");
                }

            }
            System.out.println("Overall accuracy of all folds: " + (overallAccuracy / crossFoldNumFolds));
        }
        else if(testDataFile != null){
            Data testData = new Data();
            FileIO.readFromFile(testDataFile, testData);

            NeuralNet net = createNeuralNet(layerStructure, generator);
            net.learningRate = learningRate;
            System.out.println("Before: " + net.toString());
            trainAndTest(data, testData, numEpochs, net, verbose, numEpochsPerUpdate, numClassifications,
                    accuracyOutput, 0);
            System.out.println("After: " + net.toString());
            if(saveNetFilename != null) {
                FileIO.saveNet(saveNetFilename, net);
            }
        }


        String[] headerLine = new String[]{"Fold Number", "Epoch", "Accuracy"};
        FileIO.writeToFile("ANNAccuracy.csv", headerLine, accuracyOutput);
    }

    public static double trainAndTest(Data trainingData, Data testData, int numEpochs, NeuralNet net, boolean verbose,
                                    int numEpochsPerUpdate, int numClassifications, ArrayList<String[]> accuracyOutput,
                                      int outputLabel) {
        for (int i = 0; i < numEpochs; i++) {
            if(i % numEpochsPerUpdate == 0) {
//                int[][] confusionMatrix = new int[numClassifications][numClassifications];
                int[][] confusionMatrix = new int[numClassifications][numClassifications];
                double accuracy = determineAccuracy(testData, net, confusionMatrix);
                if(verbose) System.out.println("Epoch " + i + ": accuracy " + accuracy);
                if(graphType == GraphType.Accuracy)
                    accuracyOutput.add(new String[]{"" + outputLabel, "" + i, "" + accuracy}); // show change in accuracy over time
                else if(graphType == GraphType.SquaredError && i > 0) {
                    // sum the squared error over all nodes
                    double sumOfSqauredErrors = 0;
                    for (int rowIndex = 0; rowIndex < net.nodes.length; rowIndex++) {
                        for (int nodeIndex = 0; nodeIndex < net.nodes[rowIndex].length; nodeIndex++) {
                            sumOfSqauredErrors += Math.pow(net.nodes[rowIndex][nodeIndex].lastError, 2);
                        }
                    }
                    accuracyOutput.add(new String[]{"" + outputLabel, "" + i, "" + sumOfSqauredErrors});
                }
                else if(graphType == GraphType.SquaredErrorOutputOnly) {
                    double sum = 0;
                    int outputRowIndex = net.nodes.length - 1;
                    for (int nodeIndex = 0; nodeIndex < net.nodes[outputRowIndex].length; nodeIndex++) {
                        for(DataPoint point: testData.dataPoints) {
                            sum += Math.pow(point.classificationIndex - indexOfPrediction(net, point), 2);
                        }
                    }
                    accuracyOutput.add(new String[]{"" + outputLabel, "" + i, "" + sum});
                }
            }
            runEpoch(trainingData, net);
        }
        int[][] confusionMatrix = new int[numClassifications][numClassifications];
        double accuracy = determineAccuracy(testData, net, confusionMatrix);
        System.out.println("Confusion matrix: \n" + MyTools.confusionMatrixString(trainingData.classifications, confusionMatrix) + "\n");
        return accuracy;
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
            int bestPrediction = indexOfPrediction(net, point);
            confusionMatrix[point.classificationIndex][bestPrediction]++;
            if(point.classificationIndex == bestPrediction) numCorrectPredictions++;
        }
        return (double)numCorrectPredictions / testData.dataPoints.size();
    }

    public static int indexOfPrediction(NeuralNet net, DataPoint point) {
        double[] predictiveValues = net.feedForward(point);
        double highestValue = 0;
        int bestPrediction = -1;
        for (int i = 0; i < predictiveValues.length; i++) {
            if(predictiveValues[i] > highestValue) {
                highestValue = predictiveValues[i];
                bestPrediction = i;
            }
        }
        return bestPrediction;
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
            output += "Layer " + i + ", " + nodes[i].length + " nodes- " + Arrays.toString(nodes[i]) + "\n";
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
