/**
 * Created by jeff on 4/30/16.
 */

import com.opencsv.CSVReader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class GeneticGraph extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        ScatterChart<Number, Number> scatterChart = createChart();

        Scene scene  = new Scene(scatterChart,800,600);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private ScatterChart<Number, Number> createChart() {
        ArrayList<long[]> rows = readGenStats("genStats.csv");
        if(rows == null) {
            System.out.println("Error reading file");
            System.exit(1);
        }


        final NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Generation Number");
        final NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Fitness");
        final ScatterChart<Number, Number> scatterChart = new ScatterChart<Number, Number>(xAxis, yAxis);

        XYChart.Series maxVals = new XYChart.Series();
        maxVals.setName("Best member");
        XYChart.Series avgVals = new XYChart.Series();
        avgVals.setName("Average member");
        XYChart.Series minVals = new XYChart.Series();
        minVals.setName("Worst member");
        ArrayList<Long> maxLongs = new ArrayList<>();
        ArrayList<Long> avgLongs = new ArrayList<>();
        ArrayList<Long> minLongs = new ArrayList<>();

        for (int currentGeneration = 0; currentGeneration < rows.size(); currentGeneration++) {
            long[] row = rows.get(currentGeneration);
            maxVals.getData().add(new XYChart.Data<>(currentGeneration, row[0]));
            avgVals.getData().add(new XYChart.Data<>(currentGeneration, row[1]));
            minVals.getData().add(new XYChart.Data<>(currentGeneration, row[2]));
        }
        scatterChart.getData().add(maxVals);
        scatterChart.getData().add(avgVals);
        scatterChart.getData().add(minVals);
        return scatterChart;
    }

    public ArrayList<long[]> readGenStats(String fileName) {
        try {
            CSVReader reader = new CSVReader(new FileReader(fileName));
            List<String[]> lines = reader.readAll();
            reader.close();
            ArrayList<long[]> genStats = new ArrayList<>(lines.size() - 1);
            for (int i = 1; i < lines.size(); i++) {
                String[] currentLine = lines.get(i);
                long[] currentGen = new long[3];
                for (int j = 0; j < 3; j++) {
                    currentGen[j] = Long.parseLong(currentLine[j]);
                }
                genStats.add(currentGen);
            }
            return genStats;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
