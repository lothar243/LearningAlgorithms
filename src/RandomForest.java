import java.util.ArrayList;

/**
 * Created by MTLC on 5/12/2016.
 */
public class RandomForest {
    /////////////////////////////////// non static methods //////////////////////////////////////////////

    ArrayList<DecisionTree.Node> rootNodes;
    public RandomForest(Data trainingData, int sizeOfForest, double sufficientEntropy) {
        rootNodes = new ArrayList<>(sizeOfForest);
        trainingData.initializeDataForCrossFoldValidation(sizeOfForest);

        // create the trees
        for (int treeNum = 0; treeNum < sizeOfForest; treeNum++) {
            rootNodes.add(new DecisionTree.Node(trainingData.getCrossFoldTrainingData(treeNum), sufficientEntropy));
            System.out.println("Treenum " + treeNum + ", " + rootNodes.get(treeNum).displayTree(trainingData.classifications, trainingData.attributeNames));
        }
    }

    public ArrayList<Integer> predictClassification(DataPoint testPoint, int numberOfClassifications) {
        int[] classCounts = new int[numberOfClassifications];
        for(DecisionTree.Node node: rootNodes) {
            Integer prediction = node.predictClassIndex(testPoint);
            classCounts[prediction]++;
        }
        ArrayList<Integer> bestIndices = new ArrayList<>();
        int bestNumVotes = -1;
        for (int i = 0; i < numberOfClassifications; i++) {
            if(bestNumVotes < classCounts[i]) {
                bestNumVotes = classCounts[i];
                bestIndices = new ArrayList<>();
                bestIndices.add(i);
            }
            else if(bestNumVotes == classCounts[i]) {
                bestIndices.add(i);
            }
        }
        return bestIndices;
    }

    ///////////////////////////////////// static methods ////////////////////////////////////////////////

    public static void main(String[] args) {
        String fileName = "forestFireData.csv";
        int numFolds = 10;
        int sizeOfForest = 10;
        double sufficientEntropy = .2;


        Data fullData = new Data();
        FileIO.readFromFile(fileName, fullData);
        fullData.bootstrapToBalanceClasses();
        fullData.initializeDataForCrossFoldValidation(numFolds);

        for (int foldNum = 0; foldNum < numFolds; foldNum++) {
            Data trainingData = fullData.getCrossFoldTrainingData(foldNum);
            Data testData = fullData.getCrossFoldTestData(foldNum);
            RandomForest forest = new RandomForest(trainingData, sizeOfForest, sufficientEntropy);
            int numMultiplePredictions = 0;
            double numCorrectPredictions = 0;
            for(DataPoint testPoint: testData.dataPoints) {
                ArrayList<Integer> predictions = forest.predictClassification(testPoint, testData.classifications.size());
                if(predictions.contains(testPoint.classificationIndex)) {
                    numCorrectPredictions += 1d / predictions.size();
                }
                if(predictions.size() > 1) {
                    numMultiplePredictions++;
                }
            }
            System.out.println("Fold number " + foldNum + ", accuracy: " + (numCorrectPredictions / trainingData.dataPoints.size()) + ", multiplePredictions: " + numMultiplePredictions);
        }
    }


}
