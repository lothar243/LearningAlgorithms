import java.util.ArrayList;
import java.util.Arrays;

/**
 * Creates a decision tree using the ID3 algorithm - a greedy algorithm that maximizes
 * information gain per node in the tree. It then uses this decision tree to predict the
 * class of the test data
 *
 * The pseudocode for a version of the ID3 algorithm follows, but this was implemented
 * id3 (S, attributes yet to be processed)
 * create a root node for the tree
 * base case:
 *   if S are all same class, return the single node tree root with that label
 *   if attributes is empty return r node with label equal to most common class
 * otherwise:
 *   find attributes with greatest information gain
 *   set decision attribute for root
 *   for each value of the chosen attribute
 *   add a new branch below root
 *   determine S_v for that value
 *   if S_v is empty
 *   add a leaf with label of most common class
 *   else
 *   add subtree to this branch: id3(S_v, attributes - this attribute)
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
                "\t-e FLOAT" +
                "\t\tspecify a sufficient entropyOf, range 0 - 1 (Default 0: Completely homogeneous data)";
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

        Node rootNode = new Node(trainingData, 0);
        System.out.println(rootNode.displayTree());


        Data testData = new Data(trainingData.attributeNames, trainingData.classifications);
        FileIO.readFromFile(args[1], testData);


    }

    /**
     * Calculate entropyOf
     * @param dataPoints the datapoints in the current set
     * @return the entropyOf of a given set of dataPoints
     */
    public static double entropyOf(ArrayList<DataPoint> dataPoints, int numClasses) {
        Integer[] classCounts = tallyClasses(dataPoints, numClasses);
        return entropyOf(classCounts);
    }
    public static double entropyOf(Integer[] numPointsPerClass) {
        double entropy = 0;
        // determine the total number of points to use for probabilities
        int totalPoints = 0;
        for(int numPoints: numPointsPerClass) {
            totalPoints += numPoints;
        }
        if(totalPoints == 0) {
            // no points to judge from means highest entropyOf
            return 1;
        }
        int numClasses = numPointsPerClass.length;
        for(int numInClass: numPointsPerClass) {
            double probabilityOfClass = (double)numInClass / totalPoints;
            // entropyOf is the opposite of the sum of the probabilities * the log of the probabilities
            if(probabilityOfClass != 0)
                entropy += - probabilityOfClass * Math.log(probabilityOfClass);
        }
        if(numClasses == 1) {
            // if there's only one class, the data is already homogeneous
            return 0;
        }
        return entropy / Math.log(numClasses); // dividing by this log converts the base of all the above logs
    }

    /**
     * Tally the number of points in each of the classes for a given set
     * @param dataPoints the set of points to look through
     * @return an integer array of the number of dataPoints in each of the classes
     */
    public static Integer[] tallyClasses(ArrayList<DataPoint> dataPoints, int numClasses) {
        Integer[] classCounts = new Integer[numClasses];
        Arrays.fill(classCounts, 0);
        for(DataPoint dataPoint: dataPoints) {
            classCounts[dataPoint.classificationIndex]++;
        }
        return classCounts;
    }


    static class Node {
        private ArrayList<Node> childNodes;
        private Integer[] numPointsPerClass;
        private String[] classValues;
        final double entropy;
        int splitAttribute = -1;
        private ArrayList<Double> splitAttributeValue;

        /**
         * Constructor for root node
         * @param data dataset to build the decision tree from
         */
        public Node(Data data, double sufficientEntropy) {
            this.entropy = entropyOf(data.dataPoints, data.classifications.size());

            // copy the number of points in each class from the original data set
            numPointsPerClass = new Integer[data.classificationCounts.size()];
            for (int i = 0; i < data.classificationCounts.size(); i++) {
                numPointsPerClass[i] = data.classificationCounts.get(i);
//                System.out.println("adding " + numPointsPerClass[i] + " points");
            }
            classValues = new String[data.classifications.size()];
            for (int i = 0; i < data.classifications.size(); i++) {
                classValues[i] = data.classifications.get(i);
            }
            ArrayList<Integer> remainingAttributes = new ArrayList<>();
            for (int i = 0; i < data.numAttributes; i++) {
                remainingAttributes.add(i);
            }
            generateChildNodes(data.dataPoints, remainingAttributes, sufficientEntropy);


        }

        /**
         * Constructor for child nodes
         */
        public Node(ArrayList<DataPoint> dataPoints, String[] classValues) {
            this.classValues = classValues;
            this.numPointsPerClass = tallyClasses(dataPoints, classValues.length);
            this.entropy = entropyOf(dataPoints, classValues.length);
        }

        public void generateChildNodes(ArrayList<DataPoint> dataPoints, ArrayList<Integer> remainingAttributes, double sufficientEntropy) {
            if(dataPoints == null || dataPoints.size() == 0 || remainingAttributes.size() == 0 || entropy <= sufficientEntropy) return;
            double lowestEntropy = 2;
            int bestAttribute = -1;
            ArrayList<Integer> bestChildRemainingAttributes = new ArrayList<>();
            ArrayList<ArrayList<DataPoint>> bestDataSplit = new ArrayList<>();
            for (int i = 0; i < remainingAttributes.size(); i++) {
                ArrayList<Integer> childRemainingAttributes = MyTools.copyOf(remainingAttributes);
                int splitAttribute = childRemainingAttributes.get(i);
                childRemainingAttributes.remove(i);
                ArrayList<ArrayList<DataPoint>> dataPointsAfterSplit = splitOnAttribute(dataPoints, splitAttribute);
                double childEntropy = weightedAverageOfEntropies(dataPointsAfterSplit, classValues.length);
                if(childEntropy < lowestEntropy) {
                    lowestEntropy = childEntropy;
                    bestAttribute = splitAttribute;
                    bestChildRemainingAttributes = childRemainingAttributes;
                    bestDataSplit = dataPointsAfterSplit;
                }
            }

            splitAttribute = bestAttribute;
            childNodes = new ArrayList<>();
            splitAttributeValue = new ArrayList<>();
            for(ArrayList<DataPoint> childDataPoints: bestDataSplit) {
                splitAttributeValue.add(childDataPoints.get(0).attributes[splitAttribute]);

                Node childNode = new Node(childDataPoints, classValues);
                childNode.generateChildNodes(childDataPoints, bestChildRemainingAttributes, sufficientEntropy);
                childNodes.add(childNode);
            }
        }



        public static ArrayList<ArrayList<DataPoint>> splitOnAttribute(ArrayList<DataPoint> dataPoints, int splitAttribute) {
            ArrayList<Double> splitAttributeValue = new ArrayList<>();
            ArrayList<ArrayList<DataPoint>> dataPointsAfterSplit = new ArrayList<>();
            // survey the current dataPoints and separate them based on the value of the given attribute
            for(DataPoint dataPoint: dataPoints) {
                int indexOfValue = splitAttributeValue.indexOf(dataPoint.attributes[splitAttribute]);
                if(indexOfValue == -1) {
                    // the value hasn't been seen yet
                    splitAttributeValue.add(dataPoint.attributes[splitAttribute]);
                    indexOfValue = splitAttributeValue.size() - 1;
                    dataPointsAfterSplit.add(new ArrayList<DataPoint>());
                }
                // add the dataPoint to the appropriate arrayList
                dataPointsAfterSplit.get(indexOfValue).add(dataPoint);
            }
            return dataPointsAfterSplit;
        }

        public String toString() {
            String output = toStringSummary();
            if(childNodes != null) {
                output += "Entropy after split: " + weightedAverageOfChildNodes(childNodes) + "\n";
                output += "Details of child nodes: \n\n";
                for (int i = 0; i < childNodes.size(); i++) {
                    output += "Child node, attribute " + splitAttribute + " with value " + splitAttributeValue.get(i) + "\n";
                    output += childNodes.get(i).toStringSummary() + "\n";
                }
            }
            return output;
        }

        public String toStringSummary() {
            String output = "";
            for (int i = 0; i < numPointsPerClass.length; i++) {
                output += i + "-" + numPointsPerClass[i] + " ";
            }
            output += "Entropy: " + entropy;
            return output;


        }

        public int totalClassifiedPoints() {
            if(numPointsPerClass == null) return 0;
            int totalPoints = 0;
            for(int numPoints: numPointsPerClass) {
                totalPoints += numPoints;
            }
            return totalPoints;
        }

        public static double weightedAverageOfEntropies(ArrayList<ArrayList<DataPoint>> listOfLists, int numClasses) {
            if(listOfLists == null) return 1;
            double totalEntropy = 0;
            int numPointsTotal = 0;
            for(ArrayList<DataPoint> dataPoints: listOfLists) {
                int numPointsInNode = dataPoints.size();
                totalEntropy += entropyOf(dataPoints, numClasses) * numPointsInNode;
                numPointsTotal += numPointsInNode;
            }
            if(numPointsTotal == 0) return 1;
            return totalEntropy / numPointsTotal;
        }
        public static double weightedAverageOfChildNodes(ArrayList<Node> nodes) {
            if(nodes == null || nodes.size() == 0) return 1;
            double totalEntropy = 0;
            int numPointsTotal = 0;
            for(Node node: nodes) {
                int numPointsInNode = node.totalClassifiedPoints();
                totalEntropy += entropyOf(node.numPointsPerClass) * numPointsInNode;
                numPointsTotal += numPointsInNode;
            }
            if(numPointsTotal == 0) return 1;
            return totalEntropy / numPointsTotal;
        }

        public String displayTree() {
            String output = toStringSummary() + "\n";
            if(childNodes == null) return output;
            for(Node childNode: childNodes) {
                output += childNode.displayTree("\t");
            }
            return output;
        }

        public String displayTree(String linePrefix) {
            String output = linePrefix + toStringSummary() + "\n";
            if(childNodes == null) return output;
            for(Node childNode: childNodes) {
                output += childNode.displayTree(linePrefix + "\t");
            }
            return output;
        }

    }

    

}
