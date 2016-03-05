import java.util.ArrayList;
import java.util.Arrays;

/**
 * Creates a decision tree using the ID3 algorithm - a greedy algorithm that maximizes
 * information gain per node in the tree. It then uses this decision tree to predict the
 * class of the test data. My program isn't exactly the same, but follows the same logic
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
        boolean showDecisionTree = false;
        float sufficientEntropy = 0;
        final String helpString = "\nUsage: ./DecisionTree.sh trainingData.csv testData.csv <optional arguments>\n\n" +
                "Decision Tree implementation: Uses ID3, a greedy algorithm that prefers questions that maximize" +
                "information gain.\n\n" +
                "Optional Arguments: \n" +
                "\t-v, --verbose\n" +
                "\t\tverbose - show more information\n" +
                "\t-tree\n" +
                "\t\tshow full decision tree" +
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
                    case "-v":
                    case "--verbose":
                        verbose = true;
                        break;
                    case "-tree":
                        showDecisionTree = true;
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

        // read in training data from file
        Data trainingData = new Data();
        FileIO.readFromFile(args[0], trainingData);

        // build the decision tree
        Node rootNode = new Node(trainingData, sufficientEntropy);
        if(showDecisionTree)
            System.out.println(rootNode.displayTree(trainingData.classifications, trainingData.attributeNames));

        // read in test data from file
        Data testData = new Data(trainingData.attributeNames, trainingData.classifications);
        FileIO.readFromFile(args[1], testData);

        // predict classes of test data and report the accuracy
        int numPointsTested = 0;
        int numPointsCorrectlyClassified = 0;
        for(DataPoint testPoint: testData.dataPoints) {
            numPointsTested++;
            int predictedIndex = rootNode.predictClassIndex(testPoint);
            if(predictedIndex == testPoint.classificationIndex)
                numPointsCorrectlyClassified++;
            else if(verbose)
                System.out.println("Item wrongly classified as " + trainingData.classifications.get(predictedIndex)
                        + " (" + testPoint.toString() + ": " + trainingData.classifications.get(testPoint.classificationIndex) + ")");

        }
        double accuracy = 100f * numPointsCorrectlyClassified / numPointsTested;
        System.out.println("Accuracy: " + accuracy + "%");


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

        /**
         * Calculate entropy of a set of points
         * @param dataPoints the datapoints in the current set
         * @return the entropy of a given set of dataPoints
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

        public static double informationGain(double startingEntropy, ArrayList<ArrayList<DataPoint>> listsAfterSplit, int numClasses) {
            return startingEntropy - weightedAverageOfEntropies(listsAfterSplit, numClasses);
        }

        public static double informationGain(Node node) {
            return node.entropy - weightedAverageOfChildNodes(node.childNodes);
        }

        /**
         * Looks through all of the remaining attributes and chooses the one that results in the lowest weighted
         * average of entropies of it's child nodes - this is equivalent to the largest information gain
         * @param dataPoints The datapoints of the current node
         * @param remainingAttributes The attributes that haven't yet been selected on
         * @param sufficientEntropy
         */
        public void generateChildNodes(ArrayList<DataPoint> dataPoints, ArrayList<Integer> remainingAttributes, double sufficientEntropy) {
            if(dataPoints == null || dataPoints.size() == 0 || remainingAttributes.size() == 0 || entropy <= sufficientEntropy) return;
            double lowestEntropy = 2; // the lowest entropy of a split corresponds to the highest information gain
            int bestAttribute = -1;
            ArrayList<Integer> bestChildRemainingAttributes = new ArrayList<>();
            ArrayList<ArrayList<DataPoint>> bestDataSplit = new ArrayList<>();
            for (int i = 0; i < remainingAttributes.size(); i++) {
                // first create a copy of the arrayList, then remove the attribute we're about to split on from it
                ArrayList<Integer> childRemainingAttributes = MyTools.copyOf(remainingAttributes);
                int splitAttribute = childRemainingAttributes.get(i);
                childRemainingAttributes.remove(i);

                // now actually perform the split of data points based on their value in this particular attribute
                ArrayList<ArrayList<DataPoint>> dataPointsAfterSplit = splitOnAttribute(dataPoints, splitAttribute);
                double childEntropy = weightedAverageOfEntropies(dataPointsAfterSplit, classValues.length);
                // if this split results in a better entropy, remember it
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
                // remember the attribute value for each of the groups of data
                splitAttributeValue.add(childDataPoints.get(0).attributes[splitAttribute].getDouble());

                // continue splitting up the data in the resulting nodes
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
                int indexOfValue = splitAttributeValue.indexOf(dataPoint.attributes[splitAttribute].getDouble());
                if(indexOfValue == -1) {
                    // the value hasn't been seen yet
                    splitAttributeValue.add(dataPoint.attributes[splitAttribute].getDouble());
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

        /**
         * Generate a one line string that gives data about the current node
         * @return the summary
         */
        public String toStringSummary() {
            String output = "";
            for (int i = 0; i < numPointsPerClass.length; i++) {
                output += i + ":" + numPointsPerClass[i] + ", ";
            }
            output += "Entropy: " + MyTools.roundTo(entropy, 4);
            return output;
        }
        // this is overloaded, so I can optionally specify the class names to make it more human readable
        public String toStringSummary(ArrayList<String> classNames) {
            String output = "";
            for (int i = 0; i < numPointsPerClass.length; i++) {
                output += classNames.get(i) + ":" + numPointsPerClass[i] + ", ";
            }
            output += "Entropy: " + MyTools.roundTo(entropy, 4);
            return output;
        }

        /**
         * Count the number of points that created this node
         * @return the number of points
         */
        public int totalClassifiedPoints() {
            if(numPointsPerClass == null) return 0;
            int totalPoints = 0;
            for(int numPoints: numPointsPerClass) {
                totalPoints += numPoints;
            }
            return totalPoints;
        }

        /**
         *
         * @param listOfLists
         * @param numClasses
         * @return
         */
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

        public String displayTree(ArrayList<String> classificationNames, String[] attributeNames) {
            String output = "Class counts: " + toStringSummary(classificationNames);
            if(splitAttribute >= 0) {
                output += " split on attribute: \"" + attributeNames[this.splitAttribute] + "\" for an IG of " +
                        MyTools.roundTo(informationGain(this), 4) + "\n";
            }
            else
                output += "\n";
            if(childNodes == null) return output;
            for (int i = 0; i < childNodes.size(); i++) {
                output += childNodes.get(i).displayTree(".\t", splitAttributeValue.get(i), classificationNames, attributeNames);
            }
            return output;
        }

        public String displayTree(String repeatedLinePrefix, double attributeValue,
                                  ArrayList<String> classificationNames, String[] attributeNames) {
            String output = repeatedLinePrefix + "Value of attribute: " + attributeValue + ", " + "Class counts: " +
                    toStringSummary(classificationNames);
            if(splitAttribute >= 0)
                output += " split on attribute: \"" + attributeNames[this.splitAttribute] + "\" for an IG of " +
                          MyTools.roundTo(informationGain(this), 4) + "\n";
            else
                output += "\n";
            if(childNodes == null) return output;
            for (int i = 0; i < childNodes.size(); i++) {
                output += childNodes.get(i).displayTree(repeatedLinePrefix + ".\t", splitAttributeValue.get(i),
                        classificationNames, attributeNames);
            }
            return output;
        }

        /**
         * Predict the class of test data after the tree has been generated
         * @param dataPoint a test point
         * @return the index of the predicted class
         */
        public int predictClassIndex(DataPoint dataPoint) {
            if(childNodes == null) return voteByCount(); // there are no further child nodes, take a popular vote
            int childNodeIndex = splitAttributeValue.indexOf(dataPoint.attributes[splitAttribute].getDouble());
            if(childNodeIndex == -1) return voteByCount(); // this value wasn't seen in the training data, take a popular vote
            return childNodes.get(childNodeIndex).predictClassIndex(dataPoint);
        }

        /**
         * Returns the most common class of the current node, this is usually only called for leaf nodes or nodes
         * that never saw a particular value of an attribute
         * @return the index of the most common class
         */
        public int voteByCount() {
            int predictedClassIndex = 0;
            int numVotes = 0;
            for(int i = 0; i < numPointsPerClass.length; i++) {
                if(numPointsPerClass[i] > numVotes) {
                    numVotes = numPointsPerClass[i];
                    predictedClassIndex = i;
                }
            }
            return predictedClassIndex;
        }

    }


}
