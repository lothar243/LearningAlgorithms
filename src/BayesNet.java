import java.util.*;

/**
 * Created by jeff on 3/22/16.
 */
public class BayesNet {
    public static void main(String[] args) {
        boolean verbose = false;
        String trainingDataFile = null;
        String testDataFile = null;
        int crossFoldNumFolds = -1;
        String positiveString = "1";
        boolean positiveStringSpecified = false;
        int maxParents = 2;
        int numTriesPerFold = 10;

        // read in optional arguments
        try {
            for (int argNum = 0; argNum < args.length; argNum++) {
                switch (args[argNum]) {
                    case "-t":
                        trainingDataFile = args[argNum + 1];
                        System.out.println("Using training data file: " + trainingDataFile);
                        argNum++;
                        break;
                    case "-T":
                        testDataFile = args[argNum + 1];
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
                    case "-s":
                        numTriesPerFold = Integer.parseInt(args[argNum + 1]);
                        argNum++;
                        break;
                    default:
                        System.out.println("Unknown argument encountered: " + args[argNum] + " - use -h for help");
                        System.exit(0);
                }
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(0);
        }

        Data data = new Data();
        FileIO.readFromFile(trainingDataFile, data);

        data.initializeDataForCrossFoldValidation(10);
        for (int i = 0; i < 10; i++) {
            Data trainingData = data.getCrossFoldTrainingData(i);
            Data testData = data.getCrossFoldTestData(i);
//            System.out.println("Training data: " + trainingData.toString());
//            System.out.println("Test data: " + testData.toString());

            ArrayList<ArrayList<Integer>> bestParentIndices = null;
            double bestTreeScore = -Double.MAX_VALUE;

            for (int orderNumber = 0; orderNumber < numTriesPerFold; orderNumber++) {
                ArrayList<Integer> nodeOrdering = new ArrayList<>();
                for (int j = 0; j < trainingData.numAttributes; j++) {
                    nodeOrdering.add(j);
                }
                Collections.shuffle(nodeOrdering);
                ArrayList<ArrayList<Integer>> parentIndicesList = k2Algorithm(trainingData.dataPoints, nodeOrdering, maxParents);
                double currentTreeScore = scoreNetwork(trainingData.dataPoints, trainingData.inferPossibleAttributeValues(), parentIndicesList);
                if(currentTreeScore > bestTreeScore) {
                    bestTreeScore = currentTreeScore;
                    bestParentIndices = parentIndicesList;
                }
            }
            System.out.println("fold " + i);
            int[][] confusionMatrix = new int[2][2];
            double accuracy = determineAccuracy(trainingData, testData, bestParentIndices, confusionMatrix);
            if(verbose) {
                for (int j = 0; j < trainingData.numAttributes; j++) {
                    System.out.println("Node " + j + ", parents: " + bestParentIndices.get(j).toString());
                }
                System.out.println("For a score of " + bestTreeScore);
            }

        }
    }

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
     * Print some output to help guide the user on the correct use of the command line arguments
     */
    public static void printHelpString() {
        final String helpString = "\nUsage: ./BayesNet.sh -t trainingData.csv <optional arguments>\n\n" +
                "Bayesian Network Implementation: Uses the K2 Algorithm" +
                "Optional Arguments: \n" +
                "\t-T testData.csv\n" +
                "\t\tSpecify which data to use as a test set\n" +
                "\t-x NUM\n" +
                "\t\tn-fold cross validation\n" +
                "\t-u NUM\n" +
                "\t\tUpper bound on number of parents per node (default 2)\n" +
                "\t-v\n" +
                "\t\tVerbose - show expressions";

        System.out.println(helpString);
        System.exit(1);

    }

    public static Double logFact(int num) {
        Double output = 0.0;
        while(num > 0) {
            output += Math.log(num--);
        }
//        System.out.println("logFact on " + num + " is " + output);
        return output;
    }

    public static Double scoreNetwork(ArrayList<DataPoint> dataPoints, ArrayList<ArrayList<AttributeValue>> possibleValues,
                                      ArrayList<ArrayList<Integer>> parentIndicesList) {
        double result = 1;
        for (int i = 0; i < possibleValues.size(); i++) {
            result += k2Formula(dataPoints, possibleValues, i, parentIndicesList.get(i));
        }
        return result;
    }

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
         *
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
