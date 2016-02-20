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

    public static String leftPad(String string, int targetSize) {
        if(string == null) {
            string = "";
        }
        while(string.length() < targetSize) {
            string = " " + string;
        }
        return string;
    }



}
