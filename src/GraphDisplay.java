/**
 * Created by jeff on 4/19/16.
 */

import com.opencsv.CSVReader;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import java.io.FileReader;
import java.util.ArrayList;

public class GraphDisplay extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        LineChart<Number, Number> lineChart = createChart();

        Scene scene  = new Scene(lineChart,800,600);

        primaryStage.setScene(scene);
        primaryStage.show();


    }

    /**
     * Reads the ANNAccuracy.csv to create a graph
     * @return
     */
    private LineChart<Number, Number> createChart() {
        ArrayList<double[]> rows = readDoubleSpreadsheet("ANNAccuracy.csv");
        ArrayList<Integer> foldnumbers = new ArrayList<>();
        if(rows == null) {
            System.out.println("Error reading file");
            System.exit(1);
        }


        final NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Epoch number");
        final NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Errors per epoch");
        final LineChart<Number, Number> lineChart = new LineChart<Number, Number>(xAxis, yAxis);

        XYChart.Series currentSeries = null;
        int lastFoldNumber = -1;


        for (double[] row: rows) {
            int currentFoldNumber = (int)row[0];
            if(lastFoldNumber != currentFoldNumber) {
                lastFoldNumber = currentFoldNumber;
                // we're starting a new fold
                foldnumbers.add(currentFoldNumber);
                if(currentSeries != null) {
                    lineChart.getData().add(currentSeries);
                }
                currentSeries = new XYChart.Series<>();
                currentSeries.setName("Fold " + currentFoldNumber);
            }
            int epochNumber = (int)row[1];
            currentSeries.getData().add(new XYChart.Data<Number, Number>(epochNumber, row[2]));
//            System.out.println("Adding " + epochNumber + ", " + row[2]);
        }
        lineChart.getData().add(currentSeries);
        return lineChart;
    }

    /**
     * Read a particular comma separated file where all of the entries are expected to be double
     * @param filename The name of the input file
     * @return The values that were read from the file
     */
    public static ArrayList<double[]> readDoubleSpreadsheet(String filename) {
        try {
            CSVReader reader = new CSVReader(new FileReader(filename));
            reader.readNext();
            ArrayList<double[]> rows = new ArrayList<>();

            for(String[] line = reader.readNext(); line != null; line = reader.readNext()) {
                double[] rowValues = new double[line.length];
                for (int i = 0; i < rowValues.length; i++) {
                    rowValues[i] = Double.parseDouble(line[i]);
                }
                rows.add(rowValues);
            }
            return rows;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
