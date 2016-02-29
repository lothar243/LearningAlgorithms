import com.opencsv.CSVReader;

import java.io.FileReader;

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
                    data.addDataPoint(parseAttributes(line), line[line.length - 1]);
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


    private static DataPoint parseAttributes(String[] line) {
        // each of the arguments in the first n-1 columns is a double
        double [] attributes = new double[line.length - 1];
        for (int i = 0; i < line.length - 1; i++) {
            attributes[i] = Double.parseDouble(line[i]);
        }
        // the entry in the last column is the classification, so pass it into the constructor
        return new DataPoint(attributes);
    }


}
