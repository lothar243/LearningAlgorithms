import java.util.ArrayList;
import java.util.Arrays;

/**
 * Runs the K nearest neighbor algorithm and returns the prediction accuracy
 */
public class KNN {
    public static void main(String[] args) {
        if(args.length < 3) {
            System.out.println("Usage: java KNN trainingData.csv testData.csv numNeighbors");
            System.out.println("Use -v for verbose");
            System.exit(0);
        }
        boolean verbose = false;
        if(args.length > 3 && args[3].equals("-v")) {
            verbose = true;
        }

        Data trainingData = new Data();
        FileIO.readFromFile(args[0], trainingData);
        trainingData.determineExtremes();
        trainingData.normalizeData();

        Data testData = new Data(trainingData.attributeNames, trainingData.classifications);
        FileIO.readFromFile(args[1], testData);
        testData.setExtremes(trainingData.minValues, trainingData.maxValues);
        testData.normalizeData();

        int numNeighbors = Integer.parseInt(args[2]);
        
        
        int numPointsTested = 0, numPredictionsCorrect = 0;
        if(numNeighbors > 0) {
            if(verbose) System.out.println("Performing unweighted voting on " + numNeighbors + " DataPoints");
            // allow unweighted voting of the k nearest neighbors
            for (DataPoint testPoint : testData.dataPoints) {
                ArrayList<DataPoint> nearestNeighbors = nearestNeighbors(trainingData.dataPoints, testPoint, numNeighbors);
                int predictedClassIndex = unweightedPrediction(nearestNeighbors, trainingData.classifications.size());
                numPointsTested++;
                if (predictedClassIndex == testPoint.classificationIndex) {
                    numPredictionsCorrect++;
                }
                else if(verbose) {
                    System.out.println("Item wrongly classified as " + trainingData.classifications.get(predictedClassIndex)
                            + " (" + testPoint.toString() + ": " + trainingData.classifications.get(testPoint.classificationIndex) + ")");

//                    System.out.println("Item wrongly classified as " + predictedClassification + " (" + testPoint.toString() + ")");
                }
            }
        }
        else {
            if(verbose) System.out.println("Performing weighted voting on all DataPoints");
            // weighted voting of all points
            for(DataPoint testPoint : testData.dataPoints) {
                int predictedClassIndex = weightedPrediction(
                        trainingData.dataPoints, testPoint, testData.classifications.size());
                numPointsTested++;
                if (predictedClassIndex == testPoint.classificationIndex) {
                    numPredictionsCorrect++;
                }
                else if(verbose) {
                    System.out.println("Item wrongly classified as " + trainingData.classifications.get(predictedClassIndex)
                            + " (" + testPoint.toString() + ": " + trainingData.classifications.get(testPoint.classificationIndex) + ")");
//                    System.out.println("Item wrongly classified as " + predictedClassification + " (" + testPoint.toString() + ")");
                }

            }
        }
        double percentCorrect = ((double)numPredictionsCorrect) / numPointsTested * 100;
        System.out.println("Correct predictions: " + numPredictionsCorrect + " out of " + numPointsTested + " for an accuracy of " + percentCorrect + "%");

    }

    public static ArrayList<DataPoint> nearestNeighbors(ArrayList<DataPoint> trainingDataPoints, DataPoint testPoint, int numNeighbors) {
        if(numNeighbors >= trainingDataPoints.size())
            return trainingDataPoints;
        // start by finding the distances to each existing data point
        double[] distances = new double[trainingDataPoints.size()];
        for(int i = 0; i < trainingDataPoints.size(); i++) {
            distances[i] = DataPoint.distance(testPoint, trainingDataPoints.get(i));
        }

        ArrayList<DataPoint> closestPoints = new ArrayList<DataPoint>();
        // now determine which ones are the closest
        for (int i = 0; i < numNeighbors; i++) {
            Double smallestDistance = Double.MAX_VALUE;
            int indexOfClosest = -1;
            for(int j = 0; j < distances.length; j++) {
                if(distances[j] < smallestDistance) {
                    smallestDistance = distances[j];
                    indexOfClosest = j;
                }
            }
            // remember the closest point and start looking for the next
            closestPoints.add(trainingDataPoints.get(indexOfClosest));
            // forget the distance to this point so we'll see the next one
            distances[indexOfClosest] = Double.MAX_VALUE;
        }
        return closestPoints;

    }

    public static int unweightedPrediction(ArrayList<DataPoint> closestPoints, int numClassifications) {
        int[] tallies = new int[numClassifications];
        Arrays.fill(tallies, 0);
        // take a tally of the classifications
        for(DataPoint dataPoint: closestPoints) {
            tallies[dataPoint.classificationIndex] ++;
        }
        // determine which classification is the most common
        int maxTallyIndex = -1;
        int maxTallyCount = -1;
        for(int i = 0; i < tallies.length; i++) {
            if(tallies[i] > maxTallyCount) {
                maxTallyIndex = i;
                maxTallyCount = tallies[i];
            }
        }
        return maxTallyIndex;
    }

    public static double convertDistanceToWeight(double distance) {
        final double MIN_DISTANCE = .000001;
        // if distance is too close, set to min distance, this will prevent exceptionally large weights
        // distance = Double.max(distance, MIN_DISTANCE); // for java 1.8, changed for 1.7 compatibility
        if(distance < MIN_DISTANCE) {
            distance = MIN_DISTANCE;
        }
        return 1 / (distance * distance);
    }

    public static int weightedPrediction(ArrayList<DataPoint> dataPoints, DataPoint testPoint, int numClassifications) {
        double[] weights = new double[numClassifications];
        Arrays.fill(weights, 0.0f);
        // create the array to hold the class counts
        for(DataPoint dataPoint: dataPoints) {
            // for each point, increase the existing weight
            weights[dataPoint.classificationIndex] += convertDistanceToWeight(DataPoint.distance(dataPoint, testPoint));
        }
        // determine which classification has the highest weight
        int maxWeightIndex = -1;
        double maxWeightValue = -1;
        for(int i = 0; i < weights.length; i++) {
            if(weights[i] > maxWeightValue) {
                maxWeightIndex = i;
                maxWeightValue = weights[i];
            }
        }
        return maxWeightIndex;
    }

}
