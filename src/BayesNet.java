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
            System.out.println("Training data: " + trainingData.toString());
            System.out.println("Test data: " + testData.toString());

            NaiveBayes.naiveBayes(trainingData, testData, 2, true, true, 1);
        }


    }

    public static Double probability(Data data, int attributeIndex, AttributeValue value) {
        return probability(data.dataPoints, attributeIndex, value);
    }
    public static Double probability(ArrayList<DataPoint> dataPoints, int attributeIndex, AttributeValue value) {
        int count = 0;
        for(DataPoint dataPoint: dataPoints) {
            if(dataPoint.attributes[attributeIndex].equals(value)) {
                count++;
            }
        }
        return (double)count / dataPoints.size();
    }

    /**
     * Calculate the conditional probability that an attribute has a value given other attributes having specific values
     * @param data The training data
     * @param attributeIndex The index of the unknown attribute
     * @param value The value we're finding the probability of
     * @param marginalIndices Indices of the attributes that are given
     * @param marginalValues Values of the indices, this must be the same size as marginalIndices
     * @return P(value at index is certain value given values at other indices are other specific values)
     */
    public static Double marginalProbability(Data data, int attributeIndex, AttributeValue value,
                                      ArrayList<Integer> marginalIndices, ArrayList<AttributeValue> marginalValues) {
        if(marginalIndices.size() != marginalValues.size()){
            System.out.println("Error, mismatched ArrayLists in marginalProbability()");
            return null;
        }

        // fetch the data points that satisfy the condition
        ArrayList<DataPoint> slice = new ArrayList<>();
        for(DataPoint point: data.dataPoints) {
            boolean match = true;
            for (int i = 0; i < marginalIndices.size(); i++) {
                match &= (point.attributes[marginalIndices.get(i)].equals(marginalValues.get(i)));
            }
            if(match) {
                slice.add(point);
            }
        }
        if(slice.size() == 0) { // the condition is never met, so returning the non-marginal probability
            return probability(data, attributeIndex, value);
        }
        return probability(slice, attributeIndex, value);
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
                "\t\tNUM-fold cross validation\n" +
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
        return output;
    }

    public static Double functionToMaximize(int currentIndex, ArrayList<Integer> parentIndices) {
        return null;
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
    }
}
