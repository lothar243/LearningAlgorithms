import java.util.ArrayList;
import java.util.Arrays;

/**
 * A class to store all the information regarding the data
 */
public class Data {
    public final ArrayList<DataPoint> dataPoints = new ArrayList<DataPoint>();
    Double[] minValues;
    Double[] maxValues;
    int numAttributes;

    public String [] attributeNames;
    ArrayList<String> classifications = new ArrayList<>();
    ArrayList<Integer> classificationCounts = new ArrayList<>();

    public Data() {
    }
    public Data(String[] attributeNames, ArrayList<String> classifications) {
        this.attributeNames = attributeNames;
        this.classifications = classifications;
        while(classificationCounts.size() < classifications.size()) {
            classificationCounts.add(0);
        }
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
                if (minValues[i] > dataPoint.attributes[i]) {
                    minValues[i] = dataPoint.attributes[i];
                }
                if (maxValues[i] < dataPoint.attributes[i]) {
                    maxValues[i] = dataPoint.attributes[i];
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
                    dataPoint.attributes[i] = normalizeDataPoint(dataPoint.attributes[i], minValues[i], maxValues[i]);
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
    public static double normalizeDataPoint(double originalValue, double minValue, double maxValue) {
        return (originalValue - minValue) / (maxValue - minValue);
    }



}

class DataPoint {
    int classificationIndex;
    public final double[] attributes;

    public DataPoint(double[] attributes) {
        this.attributes = attributes;
        this.classificationIndex = -1;
    }

    public String toString() {
        String output = "" + attributes[0];
        for (int i = 1; i < attributes.length; i++) {
            output += ", " + attributes[i];
        }
        return output;
    }

    public static double distanceSquared(DataPoint first, DataPoint second) {
        double distanceSquared = 0;
        for (int i = 0; i < first.attributes.length; i++) {
            distanceSquared += Math.pow(first.attributes[i] - second.attributes[i], 2);
        }
        return distanceSquared;
    }

    public static double distance(DataPoint first, DataPoint second) {
        return Math.sqrt(distanceSquared(first, second));
    }
}
