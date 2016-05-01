import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.*;
import java.util.*;

/**
 * Genetic algorithm for the knapsack problem with max weight 200
 */
public class Genetic {
    static final int TOO_MANY_ITEMS = 1000;
    static Random random = new Random();
    static ItemCollection bestCollection;



    public static void main(String[] args) {

        geneticAlgorithm(args);
    }

    public static void geneticAlgorithm(String[] args) {
        int numGenerations = 200;
        int numOffspringPerGeneration = 1000;
        int populationSize = 1000;
        int tournamentSize = 10;
        int numElitesToPreserve = 10;
        int numGenerationsPerUpdate = 1;
        int avgMutations = 1;

        for (int argNum = 0; argNum < args.length; argNum++) {
            switch (args[argNum].toLowerCase()) {
                case "-numgenerations":
                    numGenerations = Integer.parseInt(args[++argNum]);
                    break;
                case "-numoffspring":
                    numOffspringPerGeneration = Integer.parseInt(args[++argNum]);
                    break;
                case "-populationsize":
                    populationSize = Integer.parseInt(args[++argNum]);
                    break;
                case "-tournamentsize":
                    tournamentSize = Integer.parseInt(args[++argNum]);
                    break;
                case "-numElites":
                    numElitesToPreserve = Integer.parseInt(args[++argNum]);
                    break;
                case "-avgMutations":
                    avgMutations = Integer.parseInt(args[++argNum]);
                    break;
            }
        }


        ItemCollection.items = readItemsFromFile("items.csv");
        if(ItemCollection.items.size() > 64) {
            System.out.println("Too many items for this implementation");
            System.exit(TOO_MANY_ITEMS);
        }
        Geometric.setChanceOfMutation(random, (double)avgMutations/50);
        ItemCollection.random = random;

        ArrayList<ItemCollection> itemCollections = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            ItemCollection currentCollection = new ItemCollection(random.nextLong());
            itemCollections.add(currentCollection);
        }
        ArrayList<long[]> generationalStatsList = new ArrayList<>();

        for (int generationNumber = 0; generationNumber < numGenerations; generationNumber++) {
            for (int offspringNum = 0; offspringNum < numOffspringPerGeneration; offspringNum++) {
                itemCollections.add(new ItemCollection(randomParents(itemCollections, tournamentSize)));
            }
            itemCollections = cullPopulation(itemCollections, numElitesToPreserve, populationSize);
            if(generationNumber % numGenerationsPerUpdate == 0) {
                long[] genStats = generationalStats(itemCollections);
                generationalStatsList.add(genStats);
                System.out.println("Generation " + generationNumber + ", " + Arrays.toString(genStats) + ", best: " + bestCollection.toString());
            }
        }
        writeStatsToFile("genStats.csv", generationalStatsList);
    }

    public static void bruteForce() {
        // best found by brute force: 1209076421
        ItemCollection.items = readItemsFromFile("items.csv");
        if(ItemCollection.items.size() > 64) {
            System.out.println("Too many items for this implementation");
            System.exit(TOO_MANY_ITEMS);
        }

        long bestValue = -Long.MAX_VALUE;
        long bestBitVector;
        ItemCollection itemCollection = new ItemCollection(0);
        for(long i = 0; i < Math.pow(2, 51); i++) {
            itemCollection.bitVector = i;
            itemCollection.calcAll();
            if(itemCollection.fitness > bestValue) {
                bestValue = itemCollection.fitness;
                bestBitVector = i;
                System.out.println("Best so far: " + bestValue + ", " + bestBitVector);
            }
        }
    }

    public static void greedy() {
        // best value found with greedy:
        // 0000 00000 00000 00100 00100 00100 01001 00100 00000 00010 00010 10110 00110 , bitVectorValue: 145282771782342, totalPrice: 10610000, totalWeight: 200, fitness: 10610000


        ItemCollection.items = readItemsFromFile("items.csv");
        if(ItemCollection.items.size() > 64) {
            System.out.println("Too many items for this implementation");
            System.exit(TOO_MANY_ITEMS);
        }
        ArrayList<Item> items = ItemCollection.items;
        double[] densities = new double[items.size()];
        for (int i = 0; i < densities.length; i++) {
            Item currentItem = items.get(i);
            densities[i] = (double)currentItem.price / currentItem.weight;
        }
        ItemCollection collection = new ItemCollection(0);
        while(collection.getWeight() <= 200) {
            int bestIndex = -1;
            double bestValue = 0;
            for (int i = 0; i < densities.length; i++) {
                if(bestValue < densities[i] && items.get(i).weight + collection.totalWeight <= 200) {
                    bestIndex = i;
                    bestValue = densities[i];
                }
            }
            densities[bestIndex] = 0;
            if(bestIndex == -1) break;
            collection.bitVector |= 1L << bestIndex;
            collection.calcAll();
            System.out.println(collection.toString());
        }



    }

    public static long[] generationalStats(ArrayList<ItemCollection> itemCollections) {
        long bestFitness = Integer.MIN_VALUE;
        long worstFitness = Integer.MAX_VALUE;
        long sum = 0;
        for(ItemCollection itemCollection: itemCollections) {
            long currentFitness = itemCollection.getFitness();
            if(bestFitness < currentFitness) bestFitness = currentFitness;
            if(worstFitness > currentFitness) worstFitness = currentFitness;
            sum += currentFitness;
        }
        int average = (int)((double)sum / itemCollections.size());
        return new long[]{bestFitness, average, worstFitness};
    }

    public static ArrayList<ItemCollection> randomParents(ArrayList<ItemCollection> itemCollections, int tournamentSize) {
//        ArrayList<Integer> indices = new ArrayList<>(itemCollections.size());
//        for (int i = 0; i < itemCollections.size(); i++) {
//            indices.add(i);
//        }
//        Collections.shuffle(indices);
//        ArrayList<ItemCollection> parents = new ArrayList<>(tournamentSize);
//        for (int i = 0; i < tournamentSize; i++) {
//            parents.add(itemCollections.get(indices.get(i)));
//        }
//        return parents;
        ArrayList<Integer> indices = randomSample(itemCollections.size(), tournamentSize);
        ArrayList<ItemCollection> parents = new ArrayList<>(tournamentSize);
        for(Integer index: indices) {
            parents.add(itemCollections.get(index));
        }
        return parents;
    }

    private static ArrayList<Integer> randomSample(int numCollections, int sampleSize) {
        ArrayList<Integer> sample = new ArrayList<>();
        while(sample.size() < sampleSize) {
            int index = (int) (numCollections * random.nextDouble());
            if(!sample.contains(index)) sample.add(index);
        }
        return sample;
    }


    public static int randomProportionalToFitness(ArrayList<ItemCollection> itemCollections) {
        long sum = 0;
        for(ItemCollection itemCollection: itemCollections) {
            sum += itemCollection.fitness;
        }
        long targetValue = (long) (random.nextDouble()*sum);
        for (int i = 0; i < itemCollections.size(); i++) {
            targetValue -= itemCollections.get(i).fitness;
            if(targetValue >= 0) return i;
        }
        return 0;
    }

    public static ArrayList<ItemCollection> cullPopulation(ArrayList<ItemCollection> itemCollections, int numEliteToPreserve, int numItems) {
        ArrayList<ItemCollection> culledPopulation = new ArrayList<>(numItems);
        Collections.sort(itemCollections);
        Collections.reverse(itemCollections);
        bestCollection = itemCollections.get(0);
        for (int i = 0; i < numEliteToPreserve; i++) {
            int maxIndex = indexOfMax(itemCollections);
            culledPopulation.add(itemCollections.get(maxIndex));
            itemCollections.remove(maxIndex);
        }

        for (int i = 0; i < numItems; i++) {
            int indexToAdd = randomProportionalToFitness(itemCollections);
            culledPopulation.add(itemCollections.get(indexToAdd));
            itemCollections.remove(indexToAdd);
        }
        return culledPopulation;
    }
    private static int indexOfMax(ArrayList<ItemCollection> itemCollections) {
        int bestIndex = 0;
        long maxValue = -1;
        for (int i = 0; i < itemCollections.size(); i++) {
            long currentValue = itemCollections.get(i).getFitness();
            if(maxValue < currentValue) {
                maxValue = currentValue;
                bestIndex = i;
            }
        }
        return bestIndex;
    }


    public static class ItemCollection implements Comparable<ItemCollection>{
        final int decimalAdjustment = 10000;
        static int maxWeight = 200;
        static ArrayList<Item> items;
        static Random random;

        long bitVector;
        long fitness;
        int totalWeight;
        long totalPrice;

        @Override
        public int compareTo(ItemCollection other) {
            long relativeFitness = fitness - other.getFitness();
            if(relativeFitness < 0) return -1;
            if(relativeFitness > 0) return 1;
            return 0;
        }

        public ItemCollection(long bitVector) {
            this.bitVector = bitVector;
            calcAll();
        }
        public ItemCollection(ArrayList<ItemCollection> parents) {
            // perform a tournament to determine parents, then use them to create a child
            int firstParentIndex = randomProportionalToFitness(parents);
            ItemCollection firstParent = parents.get(firstParentIndex);
            parents.remove(firstParentIndex);
            int secondParentIndex = randomProportionalToFitness(parents);
            ItemCollection secondParent = parents.get(secondParentIndex);

            long crossoverBits = random.nextLong();
            bitVector = (firstParent.bitVector & crossoverBits) | (secondParent.bitVector & (~ crossoverBits) );
            mutate();
        }


        public void mutate() {
//            int numChanges = 0;
            for (int bitLocation = Geometric.nextRandom(); bitLocation < 64; bitLocation += 1 + Geometric.nextRandom() ) {
//                System.out.print(bitLocation + ", ");
                bitVector = bitVector ^ (1 << bitLocation);
//                numChanges++;
            }
//            System.out.println();
//            System.out.println(toString() + ", NumChanges: " + numChanges + ", " + bitVector);
            calcAll();
        }

        public String toString() {
            String output = "";
            for (int bit = 63; bit >= 0; bit--) {
                output += (bitVector & (1L << bit)) > 0 ? 1 : 0;
                if(bit % 5 == 0) output += " ";
            } //", bitVectorValue: " + bitVector +
            output += ", totalPrice: " + totalPrice + ", totalWeight: " + totalWeight + ", fitness: " + fitness;
            return output;
        }

        private void calcAll() {
            totalWeight = 0;
            totalPrice = 0;
            for (int i = 0; i < items.size(); i++) {
                long bitMask = 1L << i;
                totalWeight += (bitVector & bitMask) != 0 ? items.get(i).weight: 0;
                totalPrice += (bitVector & bitMask) != 0 ? items.get(i).price: 0;
            }
            totalPrice *= decimalAdjustment;
            if(totalWeight <= maxWeight) fitness = totalPrice;
            else {
                int weightAboveMax = totalWeight - maxWeight;
                fitness = totalPrice / (weightAboveMax);
            }
        }

        public long getFitness() {
            return fitness;
        }
        public int getWeight() {
            return totalWeight;
        }
        public long getPrice() {
            return totalPrice;
        }

    }

    public static ArrayList<Item> readItemsFromFile(String fileName) {
        try {
            CSVReader reader = new CSVReader(new FileReader(fileName));
            reader.readNext(); // ignore the title line
            List<String[]> lines = reader.readAll();
            reader.close();

            ArrayList<Item> items = new ArrayList<>(lines.size());
            for (String[] currentLine : lines) {
                items.add(new Item(Integer.parseInt(currentLine[0]), Integer.parseInt(currentLine[1])));
            }
            return items;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static void writeStatsToFile(String fileName, ArrayList<long[]> stats) {
        ArrayList<String[]> lines = new ArrayList<>();
        lines.add(new String[]{"maxFitness","avgFitness","minFitness"});
        for (int i = 0; i < stats.size(); i++) {
            lines.add(new String[]{"" + stats.get(i)[0], "" + stats.get(i)[1], "" + stats.get(i)[2]});
        }
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(fileName));
            writer.writeAll(lines);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static class Item {
        final int weight, price;
        public Item(int weight, int price) {
            this.weight = weight;
            this.price = price;
        }

        @Override
        public String toString() {
            return "Item{" +
                    "weight=" + weight +
                    ", price=" + price +
                    '}';
        }
    }

    public static class Geometric {
        static Random random = new Random(); // uniform random
        static double probOfMutation = .05;
        public static int nextRandom() {
            return (int)(Math.log(1-random.nextDouble())/Math.log(1-probOfMutation));
        }
        public static void setChanceOfMutation(Random _random, double prob) {
            random = _random;
            probOfMutation = prob;
        }
    }
}
