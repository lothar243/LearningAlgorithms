import com.sun.istack.internal.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Creates a decision tree using the ID3 algorithm - a greedy algorithm that maximizes
 * information gain per node in the tree. It then uses this decision tree to predict the
 * class of the test data
 */
public class DecisionTree {
    public static void main(String[] args) {
        boolean verbose = false;
        int maxDepth = -1;
        float sufficientEntropy = 0;
        final String helpString = "\nUsage: ./DecisionTree.sh trainingData.csv testData.csv <optional arguments>\n\n" +
                "Decision Tree implementation: Uses ID3, a greedy algorithm that prefers questions that maximize" +
                "information gain.\n\n" +
                "Optional Arguments: \n" +
                "\t-v, --verbose\n" +
                "\t\tverbose - show more information\n" +
                "\t-d NUM\n" +
                "\t\tspecify a maximum depth to build the tree to\n" +
                "\t-e FLOAT" +
                "\t\tspecify a sufficient entropy, range 0 - 1 (Default 0: Completely homogeneous data)";
        if (args.length < 2) {
            System.out.println(helpString);
            System.exit(1);
        }

        // read in optional arguments
        try {
            for (int argNum = 2; argNum < args.length; argNum++) {
                switch (args[argNum]) {
                    case "-d":
                        maxDepth = Integer.parseInt(args[argNum + 1]);
                        argNum++;
                        break;
                    case "-v":
                    case "--verbose":
                        verbose = true;
                        break;
                    case "-e":
                        sufficientEntropy = Float.parseFloat(args[argNum + 1]);
                        argNum++;
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
        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(0);

        }
        Data trainingData = new Data();
        FileIO.readFromFile(args[0], trainingData);

        ArrayList<Integer> remainingAttributes = new ArrayList<>();
        for (int i = 0; i < trainingData.numAttributes; i++) {
            remainingAttributes.add(i);
        }
        Node rootNode = new Node(trainingData.dataPoints, remainingAttributes, trainingData.classifications.size());
        System.out.println(rootNode.toString());
        System.out.println("----");
        remainingAttributes.remove(0);
        rootNode.childNodes = rootNode.splitOnAttribute(0, remainingAttributes, trainingData.classifications.size());
        System.out.println(rootNode.toString());

        Data testData = new Data(trainingData.attributeNames, trainingData.classifications);
        FileIO.readFromFile(args[1], testData);


    }

    /**
     * Calculate entropy
     * @param dataPoints the datapoints in the current set
     * @param numClasses the total number of classes in the original dataSet
     * @return the entropy of a given set of dataPoints
     */
    public static double entropy(ArrayList<DataPoint> dataPoints, int numClasses) {
        int[] classCounts = tallyClasses(dataPoints, numClasses);
        double entropy = 0;
        for(int numInClass: classCounts) {
            double probabilityOfClass = (double)numInClass / dataPoints.size();
            // entropy is the opposite of the sum of the probabilities * the log of the probabilities
            if(probabilityOfClass != 0)
                entropy += - probabilityOfClass * Math.log(probabilityOfClass);
        }
        if(numClasses == 1) {
            // not a normal circumstance, but I'm preventing potential division by zero
            return entropy;
        }
        return entropy / Math.log(numClasses); // dividing by this log converts the base of all the above logs
    }

    /**
     * Tally the number of points in each of the classes for a given set
     * @param dataPoints the set of points to look through
     * @param numClasses the total number of classes in the original dataSet
     * @return an integer array of the number of dataPoints in each of the classes
     */
    public static int[] tallyClasses(ArrayList<DataPoint> dataPoints, int numClasses) {
        int[] classCounts = new int[numClasses];
        Arrays.fill(classCounts, 0);
        for(DataPoint dataPoint: dataPoints) {
            classCounts[dataPoint.classificationIndex]++;
        }
        return classCounts;
    }

    // id3 (S, attributes yet to be processed)

    // create a root node for the tree
    // base case:
        // if S are all same class, return the single node tree root with that label
        // if attributes is empty return r node with label equal to most common class
    // otherwise:
        // find attributes with greatest information gain
        // set decision attribute for root
        // for each value of the chosen attribute
            // add a new branch below root
            // determine S_v for that value
            // if S_v is empty
                // add a leaf with label of most common class
            // else
                // add subtree to this branch: id3(S_v, attributes - this attribute)

    static class Node {
        private Node[] childNodes;
        private int[] numPointsPerClass;
        final double entropy;
        ArrayList<Integer> remainingAttributes;
        int splitAttribute = -1;
        private double[] splitAttributeValues;

        /**
         * Constructor for root node
         * @param data dataset to build the decision tree from
         */
        public Node(Data data, double sufficientEntropy) {
            this.entropy = entropy(data.dataPoints, data.classifications.size());
            numPointsPerClass = new int[data.classificationCounts.size()];
            for (int i = 0; i < data.classificationCounts.size(); i++) {
                numPointsPerClass[i] = data.classificationCounts.get(i);
            }

        }

        /**
         * Constructor for child nodes
         * @param dataPoints dataPoints that
         * @param remainingAttributes
         * @param numClasses
         */
        public Node(ArrayList<DataPoint> dataPoints, ArrayList<Integer> remainingAttributes, int numClasses) {
            entropy = entropy(dataPoints, numClasses);
            this.remainingAttributes = remainingAttributes;
        }

        

        public Node[] splitOnAttribute(ArrayList<DataPoint> dataPoints, int splitAttribute, ArrayList<Integer> remainingAttributes, int numClasses) {
            this.splitAttribute = splitAttribute;
            ArrayList<Double> possibleValues = new ArrayList<>();
            ArrayList<ArrayList<DataPoint>> dataPointsWithValue = new ArrayList<>();
            // survey the current dataPoints and separate them based on the value of the given attribute
            for(DataPoint dataPoint: dataPoints) {
                int indexOfValue = possibleValues.indexOf(dataPoint.attributes[splitAttribute]);
                if(indexOfValue == -1) {
                    // the value hasn't been seen yet
                    possibleValues.add(dataPoint.attributes[splitAttribute]);
                    indexOfValue = possibleValues.size() - 1;
                    dataPointsWithValue.add(new ArrayList<DataPoint>());
                }
                // add the dataPoint to the appropriate arrayList
                dataPointsWithValue.get(indexOfValue).add(dataPoint);
            }
            int numPossibleValues = possibleValues.size();
            Node[] output = new Node[numPossibleValues];
            for (int i = 0; i < numPossibleValues; i++) {
                output[i] = new Node(dataPointsWithValue.get(i), remainingAttributes, numClasses);
            }
            return output;
        }

        public String toString() {
            String output = toStringSummary();
            if(childNodes != null) {
                output += "Details of child nodes: \n";
                for(Node childNode: childNodes) {
                    output += childNode.toStringSummary();
                    output += childNode.listPoints() + "\n\n";
                }
            }
            return output;
        }

        public String toStringSummary() {
            int numChildNodes;
            if(childNodes == null) {
                numChildNodes = 0;
            }
            else {
                numChildNodes = childNodes.length;
            }
            return "Node:\n" +
            "Number of elements: " + dataPoints.size() + "\n" +
            "Entropy: " + entropy + "\n" +
            "Remaining attributes: " + MyTools.arrayToString(remainingAttributes, ", ") + "\n" +
            "Child Nodes: " + numChildNodes + "\n";

        }

        public String listPoints() {
            return MyTools.arrayToString(dataPoints, "\n");
        }

    }

}
