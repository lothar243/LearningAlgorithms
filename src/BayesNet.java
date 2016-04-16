import java.util.*;

/**
 * Uses the K2 algorithm to determine a Bayesian network structure, then uses that structure to predict point classifications
 */
public class BayesNet {
    /**
     * Print some output to help guide the user on the correct use of the command line arguments
     */
    public static void printHelpString() {
        final String helpString = "\nUsage: ./BayesNet.sh -t trainingData.csv -x NUMFOLDS <optional arguments>\n\n" +
                "Bayesian Network Implementation: Uses the K2 Algorithm" +
                "Optional Arguments: \n" +
                "\t-x NUM\n" +
                "\t\tn-fold cross validation\n" +
                "\t-u NUM\n" +
                "\t\tUpper bound on number of parents per node (default 2)\n" +
                "\t-v\n" +
                "\t\tVerbose - show information of each fold\n" +
                "\t-s NUM\n" +
                "\t\tShuffle the ordering of the attributes to find the best tree structure (best of NUM orderings)\n" +
                "\t-balance\n" +
                "\t\tDuplicate existing data points so that all classifications are equally likely\n";

        System.out.println(helpString);
        System.exit(1);

    }


    public static void main(String[] args) {
        boolean verbose = false;
        String trainingDataFile = null;
        int crossFoldNumFolds = -1;
        String positiveString = "0"; // using 0 as a positive so the values correspond to the indices
        boolean positiveStringSpecified = false;
        boolean shuffleAttributeOrder = false;
        int maxParents = 2;
        int numTriesPerFold = 10;
        boolean balanceClasses = false;

        // read in optional arguments
        try {
            for (int argNum = 0; argNum < args.length; argNum++) {
                switch (args[argNum]) {
                    case "-t":
                        trainingDataFile = args[argNum + 1];
                        System.out.println("Using training data file: " + trainingDataFile);
                        argNum++;
                        break;
                    case "-v":
                    case "--verbose":
                        verbose = true;
                        break;
                    case "-x":
                        crossFoldNumFolds = Integer.parseInt(args[argNum + 1]);
                        argNum++;
                        break;
                    case "-h":
                    case "-help":
                        printHelpString();
                        break;
                    case "-u":
                        maxParents = Integer.parseInt(args[argNum + 1]);
                        argNum++;
                        break;
                    case "-balance":
                        balanceClasses = true;
                        break;
                    case "-s":
                        shuffleAttributeOrder = true;
                        numTriesPerFold = Integer.parseInt(args[argNum + 1]);
                        argNum++;
                        break;
                    case "-p":
                        positiveString = args[argNum];
                        positiveStringSpecified = true;
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


        Data data = new Data();
        data.initializeForBinaryData(positiveString);
        FileIO.readFromFile(trainingDataFile, data);
        if(balanceClasses) data.bootstrapToBalanceClasses();

        data.initializeDataForCrossFoldValidation(crossFoldNumFolds);
        double averageAccuracy = 0;
        for (int i = 0; i < crossFoldNumFolds; i++) {
            Data trainingData = data.getCrossFoldTrainingData(i);
            Data testData = data.getCrossFoldTestData(i);
//            System.out.println("Training data: " + trainingData.toString());
//            System.out.println("Test data: " + testData.toString());

            ArrayList<ArrayList<Integer>> bestParentIndices = null;
            double bestTreeScore = -Double.MAX_VALUE;

            if(shuffleAttributeOrder) {
                for (int orderNumber = 0; orderNumber < numTriesPerFold; orderNumber++) {
                    ArrayList<Integer> nodeOrdering = new ArrayList<>();
                    for (int j = 0; j < trainingData.numAttributes; j++) {
                        nodeOrdering.add(j);
                    }
                    Collections.shuffle(nodeOrdering);
                    ArrayList<ArrayList<Integer>> parentIndicesList = k2Algorithm(trainingData.dataPoints, nodeOrdering, maxParents);
                    double currentTreeScore = scoreNetwork(trainingData.dataPoints, trainingData.inferPossibleAttributeValues(), parentIndicesList);
                    if (currentTreeScore > bestTreeScore) {
                        bestTreeScore = currentTreeScore;
                        bestParentIndices = parentIndicesList;
                    }
                }
            }
            else {
                ArrayList<Integer> nodeOrdering = new ArrayList<>();
                for (int j = 0; j < trainingData.numAttributes; j++) {
                    nodeOrdering.add(j);
                }
                bestParentIndices = k2Algorithm(trainingData.dataPoints, nodeOrdering, maxParents);
                bestTreeScore = scoreNetwork(trainingData.dataPoints, trainingData.inferPossibleAttributeValues(), bestParentIndices);
            }
            System.out.println("fold " + i);
            int[][] confusionMatrix = new int[2][2];
            double accuracy = determineAccuracy(trainingData, testData, bestParentIndices, confusionMatrix);
            if(verbose) {
                for (int j = 0; j < trainingData.numAttributes; j++) {
                    System.out.println("Node " + j + ", parents: " + bestParentIndices.get(j).toString());
                }
//                System.out.println("For a score of " + bestTreeScore);
                System.out.println("Confusion matrix: \n" + confusionMatrixString(trainingData.classifications, confusionMatrix) + "\n");
            }
            averageAccuracy += accuracy;
            System.out.println("Accuracy: " + accuracy);
        }

        System.out.println("The overall average accuracy is " + (averageAccuracy / crossFoldNumFolds));
    }

    /**
     * Create a string make the confusion matrix human readable
     * @param classLabels used for column and row titles
     * @param confusionMatrix the matrix of values to be shown
     * @return a human readable string
     */
    private static String confusionMatrixString(ArrayList<String> classLabels, int[][] confusionMatrix) {
        /**
         * Example output:
         *  t    t    t    t    t    t (tab locations)
         * "               Predicted
         * "               "0"  "1"
         * "Actual    "0"  48   2
         * "          "1"  1    49
         */



        String output = "\t\t\tPredicted\n";
        int numClassifications = classLabels.size();
        output += "\t\t";
        // row headers
        for (int i = 0; i < numClassifications; i++) {
            output += "\t\"" + classLabels.get(i) + "\"";
        }
        output += "\n";
        for (int row = 0; row < numClassifications; row++) {
            if(row == 0)
                output += "Actual\t";
            else
                output += "\t\t";
            output += "\"" + classLabels.get(row) + "\"";
            for(int col = 0; col < numClassifications; col++) {
                output += "\t" + confusionMatrix[row][col];
            }
            output += "\n";
        }
        return output;
    }

    /**
     * Runs through the test data making predictions and testing to see how accurate those predictions are
     * @param trainingData Data to gather the bayesian probabilities from
     * @param testData Data to make predictions about
     * @param parentIndices Immediate parents of attributes, indicating tree structure
     * @param confusionMatrix A blank matrix to be edited so that it contains info on true/false class predictions
     * @return The number of correct predictions divided by the total number of predictions
     */
    public static double determineAccuracy(Data trainingData, Data testData, ArrayList<ArrayList<Integer>> parentIndices, int[][] confusionMatrix) {
        int numCorrectPredictions = 0;
        for (int i = 0; i < confusionMatrix.length; i++) {
            Arrays.fill(confusionMatrix[i], 0);
        }
        for(DataPoint point: testData.dataPoints) {
            if(point.classificationIndex == predictClassification(trainingData, parentIndices, point)) {
                numCorrectPredictions++;
                confusionMatrix[point.classificationIndex][point.classificationIndex]++;
            }
            else {
                confusionMatrix[point.classificationIndex][1-point.classificationIndex]++;
            }
        }
        return (double)numCorrectPredictions / testData.dataPoints.size();
    }

    /**
     * Predict the classification of a given point
     * @param trainingData Data to draw the probabilities from
     * @param parentIndicesList Immediate parents, indicates tree structure
     * @param point The point whose classification is being predicted
     * @return The index of the most likely classification
     */
    public static int predictClassification(Data trainingData, ArrayList<ArrayList<Integer>> parentIndicesList, DataPoint point) {
        double[] classificationProbs = classificationProbs(trainingData, parentIndicesList, point);
        double bestProb = 0;
        int bestProbIndex = -1;
        for (int i = 0; i < classificationProbs.length; i++) {
            if(classificationProbs[i] > bestProb) {
                bestProbIndex = i;
                bestProb = classificationProbs[i];
            }
        }
        return bestProbIndex;
    }

    /**
     * Find the probabilities (before normalization) of the point being in each classification
     * @param trainingData Data to draw the probabilities from
     * @param parentIndicesList Immediate parents, indicates tree structure
     * @param point The point whose classification is being predicted
     * @return The probability of the point being each of classes (before normalization, so they don't sum to 1)
     */
    public static double[] classificationProbs(Data trainingData, ArrayList<ArrayList<Integer>> parentIndicesList, DataPoint point) {
//        System.out.println("dataPoints: " + trainingData.dataPoints.toString());
//        System.out.println("num classes: " + trainingData.classifications.size());
//        System.out.println("num attributes: " + trainingData.numAttributes);

        int[][][] fullDataBinCounts = NaiveBayes.findBinCounts(trainingData.dataPoints, trainingData.classifications.size(), trainingData.numAttributes, 2);
        int numClasses = trainingData.classifications.size();
        // using an m-estimator that essentially is putting one data example in every bin
        final int mEstimator = fullDataBinCounts.length * fullDataBinCounts[0].length * fullDataBinCounts[0][0].length;

        double[] classificationProducts = new double[trainingData.classifications.size()];
//        System.out.println("dimension sizes: " + fullDataBinCounts.length + " x " + fullDataBinCounts[0].length + " x " + fullDataBinCounts[0][0].length);
        Arrays.fill(classificationProducts, 1.0);
        // working through each of the dimensions, we will multiply the individual probabilities
        for (int attNumber = 0; attNumber < trainingData.numAttributes; attNumber++) {
            ArrayList<Integer> currentParents = parentIndicesList.get(attNumber);
            if(currentParents.size() == 0) {
                // the current dimension has no parents, so using the full data set
                int currentPointAttributeBin = point.attributes[attNumber].getInt();
                int totalBetweenAllClasses = 0;
                for (int classIndex = 0; classIndex < numClasses; classIndex++) {
                    totalBetweenAllClasses += fullDataBinCounts[classIndex][attNumber][currentPointAttributeBin];
                }
                for (int classIndex = 0; classIndex < numClasses; classIndex++) {
//                    System.out.println("attNumber: " + attNumber + ", class: " + classIndex + ", Multiplying by " + (fullDataBinCounts[classIndex][attNumber][currentPointAttributeBin] + 1) +
//                    " / " + (totalBetweenAllClasses + mEstimator));
                    classificationProducts[classIndex] *=
                            (double)(fullDataBinCounts[classIndex][attNumber][currentPointAttributeBin] + 1)
                            / (totalBetweenAllClasses + mEstimator);
                }
            }
            else {
                // the current dimension has parents, so restricting the data set to only those items that match parental values
                ArrayList<DataPoint> dataSlice = sliceDataToMatchParentValues(trainingData.dataPoints, currentParents, point);
                int totalBetweenAllClasses = dataSlice.size();
                int[] classCounts = new int[numClasses];
                Arrays.fill(classCounts, 0);
                for(DataPoint trainingPoint: dataSlice) {
                    classCounts[trainingPoint.classificationIndex]++;
                }
                for (int classIndex = 0; classIndex < numClasses; classIndex++) {
//                    System.out.println("attNumber: " + attNumber + ", class: " + classIndex + ", Multiplying by " + (classCounts[classIndex] + 1) +
//                            " / " + (totalBetweenAllClasses + mEstimator));
                    classificationProducts[classIndex] *= (double)(classCounts[classIndex] + 1) / (totalBetweenAllClasses + mEstimator);
                }
            }
        }
        return classificationProducts;
    }

    /**
     * Cut out any data points that don't have the correct values for the parents of the current attribute
     * @param dataPoints the data points of the original training data
     * @param parentIndices the indices of the parents of the current attribute in the bayesian network
     * @param matchingPoint the point that has the attributes that must be matched to
     * @return the data points that match on all the parental values
     */
    public static ArrayList<DataPoint> sliceDataToMatchParentValues(ArrayList<DataPoint> dataPoints, ArrayList<Integer> parentIndices, DataPoint matchingPoint) {
        ArrayList<DataPoint> slice = new ArrayList<>();
        for(DataPoint point: dataPoints) {
            // keep data points in the training data that have parental values that match a given point
            boolean match = true;
            for(int i = 0; i < parentIndices.size(); i++) {
                int currentIndex = parentIndices.get(i);
                match &= point.attributes[currentIndex].equals(matchingPoint.attributes[currentIndex]);
            }
            if(match) {
                slice.add(point);
            }
        }
        return slice;
    }

    /**
     * determine the most likely bayesian network tree structure of a particular ordering by using the k2 algorithm
     * @param dataPoints training data
     * @param nodeOrdering the ordering being used
     * @param maxParents the maximum number of allowed parents for each node
     * @return immediate parents of each of the nodes
     */
    public static ArrayList<ArrayList<Integer>> k2Algorithm(ArrayList<DataPoint> dataPoints, ArrayList<Integer> nodeOrdering, int maxParents) {
        ArrayList<ArrayList<AttributeValue>> possibleValues = Data.inferPossibleAttributeValues(dataPoints);
        ArrayList<ArrayList<Integer>> parentIndicesList = new ArrayList<>();
        // initialize the parentIndicesList to the correct size
        for (int i = 0; i < nodeOrdering.size(); i++) {
            parentIndicesList.add(null);
        }

        for (int i = 0; i < nodeOrdering.size(); i++) {
            int currentIndex = nodeOrdering.get(i);
            ArrayList<Integer> parentIndices = new ArrayList<>();
            double p_old = k2Formula(dataPoints, possibleValues, currentIndex, parentIndices);
            boolean okToProceed = true;
            while(okToProceed && parentIndices.size() < maxParents) {
                double p_new = - Double.MAX_VALUE;
                int bestParentIndex = -1;
                // find the node that maximizes f
                for (int j = 0; j < i; j++) {
                    int examiningIndex = nodeOrdering.get(j);
                    if(!parentIndices.contains(examiningIndex)) {
                        ArrayList<Integer> testParents = new ArrayList<>();
                        testParents.addAll(parentIndices);
                        testParents.add(examiningIndex);
                        double potentialNewP = k2Formula(dataPoints, possibleValues, currentIndex, testParents);
                        if(potentialNewP > p_new) {
                            p_new = potentialNewP;
                            bestParentIndex = examiningIndex;
                        }
                    }
                }
                if(p_new > p_old) {
//                    System.out.println("p_new: " + p_new + ", p_old: " + p_old + ", bestIndex: " + bestParentIndex );
                    p_old = p_new;
                    parentIndices.add(bestParentIndex);
                }
                else {
                    okToProceed = false;
                }
            }
            parentIndicesList.set(currentIndex, parentIndices);
//            System.out.println("Node " + nodeOrdering.get(i) + ", parent(s): " + parentIndices.toString());
        }
        return parentIndicesList;
    }

    /**
     * find the natural log of the factorial of a number
     * @param num the number
     * @return ln(num!)
     */
    public static Double logFact(int num) {
        Double output = 0.0;
        while(num > 0) {
            output += Math.log(num--);
        }
//        System.out.println("logFact on " + num + " is " + output);
        return output;
    }

    /**
     * find the score of a given bayesian network structure, using logFact
     * @param dataPoints the training data
     * @param possibleValues a list of all possible values for each of the attributes
     * @param parentIndicesList indices of immediate parents for each of the attributes, indicating the tree structure
     * @return the natural log of the prob that this is the correct tree structure (non normalized)
     */
    public static Double scoreNetwork(ArrayList<DataPoint> dataPoints, ArrayList<ArrayList<AttributeValue>> possibleValues,
                                      ArrayList<ArrayList<Integer>> parentIndicesList) {
        double result = 1;
        for (int i = 0; i < possibleValues.size(); i++) {
            result += k2Formula(dataPoints, possibleValues, i, parentIndicesList.get(i));
        }
        return result;
    }

    /**
     * Calculate the value of the 'g' function used in the k2 algorithm
     * @param dataPoints the training data
     * @param possibleValues a list of all possible values of each attribute
     * @param currentIndex the index being evaluated - 'i' in the k2 algorithm
     * @param parentIndices the indices of the current parental instantiation
     * @return the value of the current parental instantiation
     */
    public static Double k2Formula(ArrayList<DataPoint> dataPoints, ArrayList<ArrayList<AttributeValue>> possibleValues, int currentIndex, ArrayList<Integer> parentIndices) {
        Set<ArrayList<AttributeValue>> phi_i = Sets.cartesianProduct(Sets.generateSets(possibleValues, parentIndices));
        ArrayList<AttributeValue> V_i = possibleValues.get(currentIndex);
        if(parentIndices.size() == 0) {
            phi_i.add(new ArrayList<AttributeValue>());
        }

        double result = 0;
//        System.out.println("phi_i size: " + phi_i.size());
        for (ArrayList<AttributeValue> parentInstantiation: phi_i) {
//            System.out.println("parent instantiation: " + parentInstantiation.toString());
            result += logFact(V_i.size() - 1);
            result -= logFact(numDataPointsWithCondition(dataPoints, parentIndices, parentInstantiation) + V_i.size() - 1);
            for (int k = 0; k < V_i.size(); k++) {
                result += logFact(numDataPointsWithCondition(dataPoints, currentIndex, V_i.get(k), parentIndices, parentInstantiation));
            }
        }
        return result;
    }

    /**
     * Determine the number of data points with certain values in certain positions
     * @param dataPoints the list of datapoints
     * @param attributeIndex a particular attribute to match
     * @param attributeValue the value to be matched of that attribute
     * @param parentIndices more indices of attributes to match
     * @param parentValues
     * @return
     */
    public static int numDataPointsWithCondition(ArrayList<DataPoint> dataPoints, int attributeIndex, AttributeValue attributeValue,
                                                 ArrayList<Integer> parentIndices, ArrayList<AttributeValue> parentValues) {
        if(parentIndices.size() != parentValues.size()) {
            System.out.println("Error in numDataPointsWithCondition - arrayLists must have the same size");
        }
        int count = 0;
        for(DataPoint point: dataPoints) {
            boolean match = point.attributes[attributeIndex].equals(attributeValue);
            for(int i = 0; i < parentIndices.size(); i++) {
                match &= point.attributes[parentIndices.get(i)].equals(parentValues.get(i));
            }
            if(match) {
                count++;
            }
        }
//        System.out.println("numPointsWithCondition: " + count);
        return count;
    }

    public static int numDataPointsWithCondition(ArrayList<DataPoint> dataPoints, ArrayList<Integer> parentIndices, ArrayList<AttributeValue> parentValues) {
        if(parentIndices.size() != parentValues.size()) {
            System.out.println("Error in numDataPointsWithCondition - arrayLists must have the same size");
        }
        int count = 0;
        for(DataPoint point: dataPoints) {
            boolean match = true;
            for(int i = 0; i < parentIndices.size(); i++) {
                match &= point.attributes[parentIndices.get(i)].equals(parentValues.get(i));
            }
            if(match) {
                count++;
            }
        }
        return count;
    }


    static class Sets {
        /**
         * Find all possible combinations of elements (one from each set with order maintained)
         * @param mySets an arrayList of sets
         * @return the cartesian product of the given sets where each result is an arrayList
         */
        public static <T> Set<ArrayList<T>> cartesianProduct(ArrayList<Set<T>> mySets) {
            int numSets = mySets.size();

            // start with the first set, grow it by taking the cartesianProduct product of sets one at a time
            Set<ArrayList<T>> product = new HashSet<>();
            for (int i = 0; i < numSets; i++) {
                product = cartesianProduct(product, mySets.get(i));
            }
            return product;

        }

        public static <T> Set<ArrayList<T>> cartesianProduct(Set<ArrayList<T>> existingLists, Set<T> second) {
            Set<ArrayList<T>> product = new HashSet<>();
            if(existingLists.size() == 0) {
                for(T object: second) {
                    ArrayList<T> singleton = new ArrayList<T>();
                    singleton.add(object);
                    product.add(singleton);
                }
                return product;
            }
            for(ArrayList<T> existingList: existingLists) {
                for(T object: second) {
                    ArrayList<T> newList = (ArrayList<T>)existingList.clone();
                    newList.add(object);
                    product.add(newList);
                }
            }
            return product;
        }

        /**
         * Creates a list of sets to be fed into the cartesian product. Only certain lists of 'values' are used
         * @param values the list
         * @param indices which of the lists in 'values' to use to build sets
         * @param <T> type of the objects in the sets
         * @return a list of sets for which a cartesian product can be found
         */
        public static <T> ArrayList<Set<T>> generateSets(ArrayList<ArrayList<T>> values, ArrayList<Integer> indices) {
            ArrayList<Set<T>> mySets = new ArrayList<>();
            for (int i = 0; i < indices.size(); i++) {
                int currentIndex = indices.get(i);
                Set<T> set = new HashSet<>();
                set.addAll(values.get(currentIndex));
                mySets.add(set);
            }
//            System.out.println("Generated set size: " + mySets.size());
            return mySets;
        }
    }
}
