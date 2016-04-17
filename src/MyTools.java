import java.util.ArrayList;

/**
 * A class to hold some common functions
 */
public class MyTools {
    /**
     * Create a string from an array of objects, optionally pads the string from the left to reach a desired length
     * @param objects An array of to create the list from
     * @param delimiter A string to separate each of the above objects with
     * @param <Type> Any non primitive data type with a toString() method - does not need to be specified
     * @param stringLength the length of the desired string, useful when arranging data into a table
     * @return A string of the objects.toString() separated by the delimiters
     */
    public static <Type> String arrayToString(Type[] objects, String delimiter, int stringLength) {
        if(objects == null || objects.length == 0) {
            return "";
        }
        String output = "" + objects[0];
        for (int i = 1; i < objects.length; i++) {
            output += delimiter + objects[i];
        }
        return leftPad(output, stringLength);
    }
    public static <Type> String arrayToString(Type[] objects, String delimiter) {
        return arrayToString(objects, delimiter, -1);
    }
    public static <Type> String arrayToString(ArrayList<Type> objects, String delimiter) {
        if(objects == null || objects.size() == 0) {
            return "";
        }
        String output = "" + objects.get(0);
        for (int i = 1; i < objects.size(); i++) {
            output += delimiter + objects.get(i);
        }
        return output;
    }
    public static String arrayToString(int[] objects, String delimiter) {
        if(objects == null || objects.length == 0) {
            return "";
        }
        String output = "" + objects[0];
        for (int i = 1; i < objects.length; i++) {
            output += delimiter + objects[i];
        }
        return output;
    }

    public static String leftPad(String string, int targetSize) {
        if(string == null) {
            string = "";
        }
        while(string.length() < targetSize) {
            string = " " + string;
        }
        return string;
    }

    public static ArrayList<Integer> copyOf(ArrayList<Integer> original) {
        ArrayList<Integer> duplicate = new ArrayList<>();
        for(Integer value: original) {
            duplicate.add(value);
        }
        return duplicate;
    }

    public static void incrementAtIndex(ArrayList<Integer> list, int index) {
        list.set(index, list.get(index) + 1);
    }


    public static double roundTo(double num, int places) {
        double powerOfTen = Math.pow(10, places);
        return Math.round(num * powerOfTen) / powerOfTen;
    }


    /**
     * Create a string make the confusion matrix human readable
     * @param classLabels used for column and row titles
     * @param confusionMatrix the matrix of values to be shown
     * @return a human readable string
     */
    public static String confusionMatrixString(ArrayList<String> classLabels, int[][] confusionMatrix) {
        /**
         * Example output:
         *  t    t    t    t    t    t (tab locations)
         * "               Predicted
         * "               "0"  "1"
         * "Actual    "0"  48   2
         * "          "1"  1    49
         */



        String output = "      \t\tPredicted\n";
        int numClassifications = classLabels.size();
        output += "      \t";
        // row headers
        for (int i = 0; i < numClassifications; i++) {
            output += "\t\"" + classLabels.get(i) + "\"";
        }
        output += "\n";
        for (int row = 0; row < numClassifications; row++) {
            if(row == 0)
                output += "Actual\t";
            else
                output += "      \t";
            output += "\"" + classLabels.get(row) + "\"";
            for(int col = 0; col < numClassifications; col++) {
                output += "\t" + confusionMatrix[row][col];
            }
            output += "\n";
        }
        return output;
    }
}
