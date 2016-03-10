import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * A class to store all the information regarding the data
 */
public class Data {
    public final ArrayList<DataPoint> dataPoints = new ArrayList<>();
    Double[] minValues;
    Double[] maxValues;
    int numAttributes;

    public String [] attributeNames;
    ArrayList<String> classifications = new ArrayList<>();
    ArrayList<Integer> classificationCounts = new ArrayList<>();

    private int crossFoldTestSize = 0;
    private int crossFoldNumFolds;

    public void initializeDataForCrossFoldValidation(int numFolds) {
        crossFoldNumFolds = numFolds;
        crossFoldTestSize = dataPoints.size() / numFolds;
        Collections.shuffle(dataPoints);
    }

    public ArrayList<DataPoint> getCrossFoldTestData(int foldNumber) {
        ArrayList<DataPoint> testData = new ArrayList<>();
        if(foldNumber >= crossFoldNumFolds) return testData;

        int testDataFrom = foldNumber * crossFoldTestSize;
        int testDataTo = (foldNumber + 1) * crossFoldTestSize;
        for(int i = testDataFrom; i < testDataTo; i++) {
            testData.add(dataPoints.get(i).copyOf());
        }
        return testData;
    }

    public ArrayList<DataPoint> getCrossFoldTrainingData(int foldNumber) {
        ArrayList<DataPoint> trainingData = new ArrayList<>();
        if(foldNumber >= crossFoldNumFolds) return trainingData;

        int testDataFrom = foldNumber * crossFoldTestSize;
        int testDataTo = (foldNumber + 1) * crossFoldTestSize;
        // add the data before and after the test data to the training set
        for (int i = 0; i < testDataFrom; i++) {
            trainingData.add(dataPoints.get(i).copyOf());
        }
        for(int i = testDataTo; i < dataPoints.size(); i++) {
            trainingData.add(dataPoints.get(i).copyOf());
        }
        return trainingData;
    }

    public Data() {
    }
    public Data(String[] attributeNames, ArrayList<String> classifications) {
        this.attributeNames = attributeNames;
        this.classifications = classifications;
        while(classificationCounts.size() < classifications.size()) {
            classificationCounts.add(0);
        }
    }
    public void initializeForBinaryData(String positiveString) { // index zero is always the positive example
        classifications.add(positiveString);
        classificationCounts.add(0);
    }

    public void setAttributeNames(String[] attributeNames) {
        this.attributeNames = attributeNames;
        this.numAttributes = attributeNames.length - 1;
    }

    /**
     * Introduce new dataPoints to the collection
     * @param dataPoint The point to be introduced
     */
    public void addDataPoint(DataPoint dataPoint, String classification) {
        dataPoints.add(dataPoint);
        boolean categoryAlreadySeen = false;
        for (int i = 0; i < classifications.size(); i++) {
            if(classifications.get(i).equals(classification)) {
                // increment the number of times this category was encountered
                classificationCounts.set(i, classificationCounts.get(i) + 1);
                dataPoint.classificationIndex = i;
                categoryAlreadySeen = true;
            }
        }
        if(!categoryAlreadySeen) {
            // if the classification hasn't been seen, add it to the list
            classifications.add(classification);
            classificationCounts.add(1);
            dataPoint.classificationIndex = classifications.size() - 1;
        }
    }

    public String toString() {
        String output = attributeNames[0];
        for (int i = 1; i < attributeNames.length; i++) {
            output += ", " + attributeNames[i];
        }
        output += "\n";
        for(DataPoint datapoint: dataPoints) {
            output += datapoint.toString();
        }
        return output;
    }

    public ArrayList<ArrayList<AttributeValue>> inferPossibleAttributeValues() {
        ArrayList<ArrayList<AttributeValue>> possibleValues = new ArrayList<>();
        for (int i = 0; i < numAttributes; i++) {
            ArrayList<AttributeValue> currentAttributeValues = new ArrayList<>();
            for(DataPoint point: dataPoints) {
                AttributeValue currentValue = point.attributes[i];
                if(!currentAttributeValues.contains(currentValue)) {
                    currentAttributeValues.add(currentValue);
                }
            }
            possibleValues.add(currentAttributeValues);
        }
        return possibleValues;
    }

    /**
     * Compute and store the min and max value of the data for each attribute
     */
    public void determineExtremes() {
        minValues = new Double[attributeNames.length - 1];
        maxValues = new Double[attributeNames.length - 1];
        Arrays.fill(minValues, Double.MAX_VALUE);
        Arrays.fill(maxValues, Double.MIN_VALUE);

        // first loop through all of the data to find the min and max values of each attribute
        for (DataPoint dataPoint : dataPoints) {
            for (int i = 0; i < attributeNames.length - 1; i++) {
                if (dataPoint.attributes[i].isLessThan(minValues[i])) {
                    minValues[i] = (Double)dataPoint.attributes[i].getValue();
                }
                if (dataPoint.attributes[i].isGreaterThan(maxValues[i])) {
                    maxValues[i] = (Double)dataPoint.attributes[i].getValue();
                }
            }
        }
    }

    /**
     * Manually set extremes - typically preparing to rescale test data based on training data
     * @param minValues The min values of each of the attributes
     * @param maxValues The max values of each of the attributes
     */
    public void setExtremes(Double[] minValues, Double[] maxValues) {
        this.minValues = minValues;
        this.maxValues = maxValues;
    }
    public void normalizeData() {

        // now normalize the data to be from 0 to 1
        for(DataPoint dataPoint: dataPoints) {
            for(int i = 0; i < attributeNames.length - 1; i++) {
                if(minValues[i] != maxValues[i]) {
                    // ensure min and max are not equal to avoid a division by zero error
                    dataPoint.attributes[i].setValue(normalizeDataPoint((Double)dataPoint.attributes[i].getValue(),
                            minValues[i], maxValues[i]));
                }
            }
        }
    }

    /**
     * Rescale a value so that the interval [minValue, maxValue] is mapped to [0, 1]
     * @param originalValue The value to be mapped
     * @param minValue The left endpoint of the interval
     * @param maxValue The right endpoint of the interval
     * @return The new value in the interval [0, 1]
     */
    public static Double normalizeDataPoint(Double originalValue, Double minValue, Double maxValue) {
        return (originalValue - minValue) / (maxValue - minValue);
    }



}

class DataPoint {
    int classificationIndex;
    public final AttributeValue[] attributes;

    public DataPoint(Object[] doubleAttributes) {
        this.attributes = AttributeValue.createArray(doubleAttributes);
        this.classificationIndex = -1;
    }
    public DataPoint(Object[] doubleAttributes, int classificationIndex) {
        this.attributes = AttributeValue.createArray(doubleAttributes);
        this.classificationIndex = classificationIndex;
    }
    public DataPoint(AttributeValue[] attributes, int classificationIndex) {
        this.attributes = attributes;
        this.classificationIndex = classificationIndex;

    }

    public boolean equals(Object otherObject) {
        if(!otherObject.getClass().equals(DataPoint.class)) return false;
        DataPoint other = (DataPoint)otherObject;
        if(this.classificationIndex != other.classificationIndex) return false;
        if(this.attributes == null) {
            return other.attributes == null;
        }
        if(this.attributes.length != other.attributes.length) return false;
        for (int i = 0; i < this.attributes.length; i++) {
            if(!this.attributes[i].equals(other.attributes[i])) return false;
        }
        return true;
    }

    public String toString() {
        String output = "" + attributes[0];
        for (int i = 1; i < attributes.length; i++) {
            output += ", " + attributes[i];
        }
        output += " Class index: " + classificationIndex;
        return output;
    }

    public DataPoint copyOf() {
        AttributeValue[] copiedValues = new AttributeValue[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            copiedValues[i] = attributes[i].copyOf();
        }
        return new DataPoint(copiedValues, classificationIndex);
    }

    public static Double distanceSquared(DataPoint first, DataPoint second) {
        Double distanceSquared = 0d;
        for (int i = 0; i < first.attributes.length; i++) {
            distanceSquared += Math.pow(first.attributes[i].minus(second.attributes[i]), 2);
        }
        return distanceSquared;
    }

    public static Double distance(DataPoint first, DataPoint second) {
        return Math.sqrt(distanceSquared(first, second));
    }

}
