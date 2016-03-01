import java.util.Arrays;

/**
 * Naive Bayes implementation using Bins
 */
public class NaiveBayes {
    public static void main(String[] args) {
        boolean verbose = false;
        boolean rescaleData = false;
        double mEstimator = 1000;
        int numBins = 10;
        final String helpString = "\nUsage: ./NaiveBayes.sh trainingData.csv testData.csv <optional arguments>\n\n" +
                "Naive Bayes implementation: Creates bins for the data, then uses these bins to predict the class of the test data\n\n" +
                "Optional Arguments: \n" +
                "\t-n NUM\n" +
                "\t\tnumber of bins (Defaults to 100)\n" +
                "\t-v, --verbose\n" +
                "\t\tverbose\n" +
                "\t-m NUM\n" +
                "\t\tspecify an m-Estimator (default 1000)\n" +
                "\t-r\n" +
                "\t\tRescale probabilities so more frequent observations aren't favored\n";
        if(args.length < 2) {
            System.out.println(helpString);
            System.exit(1);
        }

        // read in optional arguments
        try {
            for (int argNum = 2; argNum < args.length; argNum++) {
                switch (args[argNum]) {
                    case "-n":
                        numBins = Integer.parseInt(args[argNum + 1]);
                        argNum++;
                        break;
                    case "-v":
                    case "--verbose":
                        verbose = true;
                        break;
                    case "-m":
                        mEstimator = Double.parseDouble(args[argNum + 1]);
                        argNum++;
                        break;
                    case "-r":
                        System.out.println("Rescaling the data so that more frequent observations are not favored");
                        rescaleData = true;
                        break;
                    case "-h":
                    case "-help":
                        System.out.println(helpString);
                        System.exit(1);
                    default:
                        System.out.println("Unknown argument encountered: " + args[argNum] + " - use -h for help");
                        System.exit(0);
                }
            }
        }
        catch (Exception e) {
            System.out.println(e.toString());
            System.exit(0);

        }
        System.out.println("Using " + numBins + " bins");
        System.out.println("Using an m-Estimator of " + mEstimator);
        Data trainingData = new Data();
        FileIO.readFromFile(args[0], trainingData);
        trainingData.determineExtremes();

        BinInfo binInfo = new BinInfo(trainingData.minValues, trainingData.maxValues, numBins);

        // find the number of times each classification has an item in each bin
        int[][][] binCounts = binInfo.findBinCounts(trainingData);

        // output some information if verbose is on
        if(verbose) {
            System.out.println("Mins: " + MyTools.arrayToString(trainingData.minValues, ", "));
            System.out.println("Maxes: " + MyTools.arrayToString(trainingData.maxValues, ", "));
            System.out.println("Bin sizes: " + MyTools.arrayToString(binInfo.binSizes, ", "));
            System.out.println();

            for (int attIndex = 0; attIndex < trainingData.numAttributes; attIndex++) {
                System.out.println("Attribute: " + trainingData.attributeNames[attIndex]);
                for (int classIndex = 0; classIndex < trainingData.classifications.size(); classIndex++) {
                    System.out.print(MyTools.leftPad(trainingData.classifications.get(classIndex) + ": ", 9));
                    for (int binIndex = 0; binIndex < numBins; binIndex++) {
                        System.out.print(MyTools.leftPad("" + binCounts[classIndex][attIndex][binIndex], 3) + " ");
                    }
                    System.out.println();
                }
                System.out.println(); // extra newline after each category
            }
        }

        // the bins are all set up, so now it's time to make some predictions about the test data
        Data testData = new Data(trainingData.attributeNames, trainingData.classifications);
        FileIO.readFromFile(args[1], testData);

        for (int i = 0; i < trainingData.classifications.size(); i++) {
            System.out.println(trainingData.classifications.get(i) + ": " + trainingData.classificationCounts.get(i) +
                    ", " + testData.classifications.get(i) + ": " + testData.classificationCounts.get(i));
        }


        int numPointsTested = 0;
        int numPredictionsCorrect = 0;

        Integer[] proportionAdjustment = new Integer[trainingData.classifications.size()];
        if(rescaleData) {
            proportionAdjustment = trainingData.classificationCounts.toArray(proportionAdjustment);
        }
        else {
            Arrays.fill(proportionAdjustment, 1);
        }

        for(DataPoint testPoint: testData.dataPoints) {
            // first figure out which bin it goes in
            int predictedClassIndex = binInfo.predictedClass(testPoint, binCounts, mEstimator, proportionAdjustment);
//            String predictedClass = trainingData.classifications.get(predictedClassIndex);

//            if(testPoint.classification.equals(predictedClass)) {
            if(testPoint.classificationIndex == predictedClassIndex) {
                numPredictionsCorrect++;
            }
            else if(verbose) {
                System.out.println("Item wrongly classified as " + trainingData.classifications.get(predictedClassIndex)
                        + " (" + testPoint.toString() + ": " + trainingData.classifications.get(testPoint.classificationIndex) + ")");
            }
            numPointsTested++;
        }
        System.out.println("Correct predictions: " + numPredictionsCorrect + " out of " + numPointsTested +
            " for an accuracy of " + ((double)numPredictionsCorrect/numPointsTested*100) + "%");

    }




    /**
     * BinInfo is a container class to store information used when 'binning' the data, along with associate methods
     */
    static class BinInfo {
        public Double[] minValues;
        public Double[] binSizes;
        public Integer numBins;
        final Integer numAttributes;

        public BinInfo (Double[] minValues, Double[] maxValues, int numBins) {
            // ensuring nothing strange got passed in
            if(minValues.length != maxValues.length) {
                System.out.println("Error in BinInfo constructor: minValues is size " + minValues.length +
                "but maxValues is size " + maxValues.length);
                System.exit(0);
            }
            numAttributes = minValues.length;
            if(numBins <= 0) {
                System.out.println("Error in BinInfo constructor: numBins can't be " + numBins);
                System.exit(0);
            }

            // setting up the variables
            this.numBins = numBins;
            this.minValues = minValues;
            // we don't need to store the maxValues - it could be recomputed if we really needed to
            binSizes = new Double[numAttributes];
            for (int i = 0; i < numAttributes; i++) {
                binSizes[i] = (maxValues[i] - minValues[i]) / numBins;
            }
        }

        /**
         * Determine which bins a dataPoint falls in
         * @param dataPoint The dataPoint to be binned
         * @return An integer array of the indices - one for each attribute
         */
        public Integer[] findBinIndices(DataPoint dataPoint) {
            Integer[] binIndices = new Integer[numAttributes];
            for (int i = 0; i < numAttributes; i++) {
                // calculate which bin the datapoint should be in
                int currentIndex = (int) ((dataPoint.attributes[i].minus(minValues[i])) / binSizes[i]);
                // ensure the index is in the correct range
                if(currentIndex < 0) {
                    currentIndex = 0;
                }
                else if(currentIndex > numBins - 1) {
                    currentIndex = numBins- 1;
                }
                binIndices[i] = currentIndex;
            }
            return binIndices;
        }

        /**
         * Determine the number of dataPoints of each class are in each bin
         * @param data The training data
         * @return An 3d integer array indexed by class number, attribute number, then bin number
         */
        public int[][][] findBinCounts(Data data) {
            int[][][] binCounts = new int[data.classifications.size()][numAttributes][numBins];

            // start by filling all of the bins with zeros
            for (int classIndex = 0; classIndex < numAttributes; classIndex++) {
                for (int attIndex = 0; attIndex < data.numAttributes; attIndex++) {
                    Arrays.fill(binCounts[classIndex][attIndex], 0);

                }
            }

            // for each dataPoint, determine which bin it goes into, then increment the count for that bin
            for(DataPoint dataPoint: data.dataPoints) {
                Integer[] binIndices = findBinIndices(dataPoint);
                for (int attIndex = 0; attIndex < numAttributes; attIndex++) {
                    // binIndices[i] contains the index of the bin for attribute i
                    binCounts[dataPoint.classificationIndex][attIndex][binIndices[attIndex]]++;
                }
            }
            return binCounts;
        }

        /**
         * Predict the class of test point after training
         * @param testPoint The dataPoint to predict the class
         * @param binCounts The 3d integer array indexed by class, attribute, bin formed when training
         * @param mEstimator A value to use as an m-estimator
         * @param dataPointsPerClass number of datapoints in each of the classes, used to adjust the weight the counts
         * @return The index of the predicted class
         */
        public int predictedClass(DataPoint testPoint, int[][][] binCounts, double mEstimator, Integer[] dataPointsPerClass) {
            Integer[] binIndices = findBinIndices(testPoint);
            int numClasses = binCounts.length;


            // for each attribute, determine the likelihood the datapoint belongs to a class
            double[] probabilityProduct = new double[numClasses];
            for (int i = 0; i < numClasses; i++) {
                // we could scale each attribute individually, but we're starting with the scaling in place already
                probabilityProduct[i] = (double)1 / Math.pow(dataPointsPerClass[i], numAttributes);
            }
            int mTimesP = mEstimator == 0 ? 0: 1; // decide if we are adding 0 or 1 to the numerator
            for (int attIndex = 0; attIndex < numAttributes; attIndex++) {
                int binIndex = binIndices[attIndex];
                int totalInBin = 0;
                for (int classIndex = 0; classIndex < numClasses; classIndex++) {
                    totalInBin += binCounts[classIndex][attIndex][binIndex];
                }
                for (int classIndex = 0; classIndex < numClasses; classIndex++) {
                    double prob = ((double)binCounts[classIndex][attIndex][binIndex] + mTimesP) / (totalInBin + mEstimator);
                    probabilityProduct[classIndex] *= prob;
                }
            }
//            System.out.println(arrayToString(nonPrimitiveDouble(probabilityProduct), " "));

            // now find which class has the largest value
            double highestProb = 0;
            int highestProbIndex = -1;
            for (int classIndex = 0; classIndex < numClasses; classIndex++) {
                if(probabilityProduct[classIndex] > highestProb) {
                    highestProb = probabilityProduct[classIndex];
                    highestProbIndex = classIndex;
                }
            }
            return highestProbIndex;
        }
    }

    public static int sum(Integer[] terms) {
        int output = 0;
        for(Integer term: terms) {
            output += term;
        }
        return output;
    }

}
