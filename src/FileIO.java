import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Read fruit data from file
 */
public class FileIO {



    /**
     * file example:
     * Redness,Yellowness,Mass,Volume,Class
     * 4.81647192,2.347954131,125.5081887,25.01441448,apple
     * 4.327248484,3.322961013,118.4265761,19.07534923,peach
     */
    public static boolean readFromFile(String fileName, Data data) {

        try {
            CSVReader reader = new CSVReader(new FileReader(fileName));
            // begin by reading in the attribute names
            String [] attributeNames = reader.readNext();
            data.setAttributeNames(attributeNames);

            // now read the file in a line at a time and add the entry
            String [] line;
            // keep reading until we reach the end of the file
            while((line = reader.readNext()) != null) {
                // ignore blank lines
                if(line.length != 0) {
                    if(line.length != attributeNames.length) {
                        System.out.println("Read a line with the wrong number of entries");
                        System.exit(0);
                    }
                    data.addDataPoint(parseAttributes(line, line.length - 1), line[line.length - 1]);
                }

            }
            return true;
            

        }
        catch (Exception e) {
            System.out.println("Error reading file - " + fileName);
            e.printStackTrace();
        }
        System.exit(0);
        return false;
    }

    public static ArrayList<DataPoint> readRawPoints(String fileName) {
        try {
            CSVReader reader = new CSVReader(new FileReader(fileName));
            // begin by reading in the attribute names
            String [] attributeNames = reader.readNext();

            // now read the file in a line at a time and add the entry
            String [] line;
            ArrayList<DataPoint> dataPoints = new ArrayList<>();
            // keep reading until we reach the end of the file
            while((line = reader.readNext()) != null) {
                // ignore blank lines
                if(line.length != 0) {
                    if(line.length != attributeNames.length) {
                        System.out.println("Read a line with the wrong number of entries");
                        System.exit(0);
                    }
                    dataPoints.add(parseAttributes(line, line.length));
                }

            }
            return dataPoints;


        }
        catch (Exception e) {
            System.out.println("Error reading file - " + fileName);
            e.printStackTrace();
        }
        System.exit(0);
        return null;
    }


    public static DataPoint parseAttributes(String[] line, int lastIndex) {
        // each of the arguments in the first n-1 columns is a Double
        Object [] attributes = new Object[lastIndex];
        for (int i = 0; i < lastIndex; i++) {
            try {
                attributes[i] = Double.parseDouble(line[i]);
            }
            catch(Exception e) {
                attributes[i] = line[i];
            }
        }
        // the entry in the last column is the classification, so pass it into the constructor
        return new DataPoint(attributes);
    }

    public static void writeToFile(String filename, String[] headers, List<String[]> values) {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filename));
            // begin by writing the headers
            writer.writeNext(headers);
            writer.writeAll(values);
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


}
