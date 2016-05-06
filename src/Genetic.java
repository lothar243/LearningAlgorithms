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
    static final int DECIMAL_ADJUST = 1; // move the decimal so that integers can approximate floating point calculations
    static int numItems = 50;


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
        double avgMutations = 1;
        boolean verbose = false;
        boolean repair = false;

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
                case "-numelites":
                    numElitesToPreserve = Integer.parseInt(args[++argNum]);
                    break;
                case "-avgmutations":
                    avgMutations = Double.parseDouble(args[++argNum]);
                    break;
                case "-updatefrequency":
                    numGenerationsPerUpdate = Integer.parseInt(args[++argNum]);
                    break;
                case "-verbose":
                    verbose = true;
                    break;
                case "-h":
                case "-help":
                    printHelpString();
                    System.exit(0);
                    break;
                case "-repair":
                    repair = true;
                    break;
                case "-greedy":
                    greedy();
                    System.exit(0);
                    break;
                default:
                    System.out.println("Unknown command encountered: " + args[argNum]);
                    System.exit(1);
            }
        }


        ItemCollection.items = readItemsFromFile("items.csv");
        numItems = ItemCollection.items.size();
        if(numItems > 64) {
            System.out.println("Too many items for this implementation");
            System.exit(TOO_MANY_ITEMS);
        }
        Geometric.setChanceOfMutation(random, (double)avgMutations/50);
        ItemCollection.random = random;

        ArrayList<ItemCollection> itemCollections = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            ItemCollection currentCollection = new ItemCollection(random.nextLong(), repair);
            itemCollections.add(currentCollection);
        }
        ArrayList<long[]> generationalStatsList = new ArrayList<>();



        for (int generationNumber = 0; generationNumber < numGenerations; generationNumber++) {
            for (int offspringNum = 0; offspringNum < numOffspringPerGeneration; offspringNum++) {
                itemCollections.add(new ItemCollection(randomParents(itemCollections, tournamentSize),
                        randomParents(itemCollections, tournamentSize), repair));
            }
            itemCollections = cullPopulation(itemCollections, numElitesToPreserve, populationSize);
            if(generationNumber % numGenerationsPerUpdate == 0) {
                long[] genStats = generationalStats(itemCollections);
                generationalStatsList.add(genStats);
                if(verbose) System.out.println("Generation " + generationNumber + ", " + Arrays.toString(genStats) + ", best: " + bestCollection.toString());
            }
        }
        System.out.println("Ending stats: " + ", " + Arrays.toString(generationalStatsList.get(generationalStatsList.size() - 1)) +
                ", best: " + bestCollection.toString() + ", total price: " + (bestCollection.totalPrice / DECIMAL_ADJUST));
        writeStatsToFile("genStats.csv", generationalStatsList);
    }

    public static void printHelpString() {
        System.out.println("Usage:\n" +
                "-numgenerations NUM\n" +
                "\tThe number of generations to run for\n" +
                "-numoffspring NUM\n" +
                "\tThe number of offspring to spawn during each generation\n" +
                "-populationsize NUM\n" +
                "\tThe population size to maintain between generations\n" +
                "-tournamentsize NUM\n" +
                "\tThe size of random parents from which to create tournaments\n" +
                "-numElites NUM\n" +
                "\tThe number the best members to make immune to population culling\n" +
                "-avgMutations NUM\n" +
                "\tThe expected number of mutations in a given bit vector (default 1)\n" +
                "-updateFrequency NUM\n" +
                "\tThe number of generations between logging the results (default 1)\n" +
                "-verbose\n" +
                "\tPrint the logged results to the screen\n" +
                "-repair\n" +
                "\tRandomly set members of the bit vector to 0 when it's overweight\n" +
                "-help\n" +
                "\tPrint this help text");
    }

    /**
     * I wrote the brute force algorithm and let it run for a few minutes. It was unable to find
     * an optimal solution in this time.
     */
    public static void bruteForce() {
        // best found by brute force: 1209076421 (bit vector as a long)
        ItemCollection.items = readItemsFromFile("items.csv");
        if(ItemCollection.items.size() > 64) {
            System.out.println("Too many items for this implementation");
            System.exit(TOO_MANY_ITEMS);
        }

        long bestValue = -Long.MAX_VALUE;
        long bestBitVector;
        ItemCollection itemCollection = new ItemCollection(0, false);
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

    /**
     * Find a fairly good solution quite quickly by selecting the item with the highest value repeatedly. If an item
     * will bring the weight too high, it moves on to the next item. The results are as follows
     *
     * 00100 00100 00100 01001 00100 00000 00010 00010 10110 00110 , totalPrice: 1061, totalWeight: 200, fitness: 1061

     */
    public static void greedy() {


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
        ItemCollection collection = new ItemCollection(0, false);
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

    /**
     * Determines the best, average and worst fitness for all of the members of the population
     * @param itemCollections The population
     * @return The specified fitness
     */
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

    /**
     * Given a population, select members to be used in a tournament
     * @param itemCollections The population
     * @param tournamentSize The number of members to select
     * @return The selected members
     */
    public static ArrayList<ItemCollection> randomParents(ArrayList<ItemCollection> itemCollections, int tournamentSize) {
        ArrayList<Integer> indices = randomSample(itemCollections.size(), tournamentSize);
        ArrayList<ItemCollection> parents = new ArrayList<>(tournamentSize);
        for(Integer index: indices) {
            parents.add(itemCollections.get(index));
        }
        return parents;
    }

    /**
     * Using a uniform random sampling, choose integers between 0 and a number (no repetition)
     * @param numCollections The upper limit of possible numbers + 1
     * @param sampleSize The number of integers to choose
     * @return The list of integers
     */
    private static ArrayList<Integer> randomSample(int numCollections, int sampleSize) {
        ArrayList<Integer> sample = new ArrayList<>();
        while(sample.size() < sampleSize) {
            int index = (int) (numCollections * random.nextDouble());
            if(!sample.contains(index)) sample.add(index);
        }
        return sample;
    }

    /**
     * Select an index of the itemCollections with selection probability proportional to their fitness
     * @param itemCollections The items to get the fitness values from
     * @return The randomly chosen index
     */
    public static int randomProportionalToFitness(ArrayList<ItemCollection> itemCollections) {
        long sum = 0;
        for(ItemCollection itemCollection: itemCollections) {
            sum += itemCollection.fitness;
        }
        long targetValue = (long) (random.nextDouble()*sum);
        for (int i = 0; i < itemCollections.size(); i++) {
            targetValue -= itemCollections.get(i).fitness;
            if(targetValue <= 0) {
//                System.out.println("index " + i);
                return i;
            }
        }
        return 0;
    }

    /**
     * Given a list of itemCollections, reduces the population size by randomly choosing survivors with probability
     * proportional to their fitness
     * @param itemCollections The starting populations
     * @param numEliteToPreserve Preserve a certain number of members that have the highest fitness
     * @param targetPopulationSize The population size to reduce to
     * @return The newly reduced list of itemCollections
     */
    public static ArrayList<ItemCollection> cullPopulation(ArrayList<ItemCollection> itemCollections, int numEliteToPreserve, int targetPopulationSize) {
        ArrayList<ItemCollection> culledPopulation = new ArrayList<>(targetPopulationSize);
        bestCollection = itemCollections.get(indexOfMax(itemCollections));

        for (int i = 0; i < numEliteToPreserve; i++) {
            int maxIndex = indexOfMax(itemCollections);
            culledPopulation.add(itemCollections.get(maxIndex));
            itemCollections.remove(maxIndex);
        }


        for (int i = culledPopulation.size(); i < targetPopulationSize; i++) {
            int indexToAdd = randomProportionalToFitness(itemCollections);
            culledPopulation.add(itemCollections.get(indexToAdd));
            itemCollections.remove(indexToAdd);
        }
        return culledPopulation;
    }

    /**
     * Find the item with the highest fitness
     * @param itemCollections The list to look through
     * @return The index of the item
     */
    private static int indexOfMax(ArrayList<ItemCollection> itemCollections) {
        int bestIndex = 0;
        long maxValue = -Long.MAX_VALUE;
        for (int i = 0; i < itemCollections.size(); i++) {
            long currentValue = itemCollections.get(i).getFitness();
            if(maxValue < currentValue) {
                maxValue = currentValue;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    /**
     * The class used to wrap each bit vector, storing the fitness, weight, and price
     */
    public static class ItemCollection implements Comparable<ItemCollection>{
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

        /**
         * Constructor - given a specified bit vector
         * @param bitVector
         */
        public ItemCollection(long bitVector, boolean repair) {
            this.bitVector = bitVector;
            calcAll();
            if(repair) repair();
        }

        /**
         * Constructor - given two tournaments, a winner is chosen proportional to fitness, then the two parents
         * create a new bit vector using uniform crossover
         * @param firstParentPool
         * @param secondParentPool
         */
        public ItemCollection(ArrayList<ItemCollection> firstParentPool, ArrayList<ItemCollection> secondParentPool,
                              boolean repair) {
            // perform a tournament to determine parents, then use them to create a child
            int firstParentIndex = randomProportionalToFitness(firstParentPool);
            ItemCollection firstParent = firstParentPool.get(firstParentIndex);
            int secondParentIndex = randomProportionalToFitness(secondParentPool);
            ItemCollection secondParent = secondParentPool.get(secondParentIndex);

            long crossoverBits = random.nextLong();
            bitVector = (firstParent.bitVector & crossoverBits) | (secondParent.bitVector & (~ crossoverBits) );
            mutate();
            if(repair) repair();
        }

        /**
         * Randomly drops items until the weight gets below 200
         */
        public void repair() {
            while(totalWeight > maxWeight) {
                long swapBit = 1L << ((long)(random.nextDouble() * numItems));
//                System.out.println("swapbit: " + bitVectorToString(~swapBit) + ", bitVector: " + bitVectorToString(bitVector));
                bitVector = bitVector & ~swapBit;
                calcAll();
            }
        }

        /**
         * Mutate the bit vector with the initially specified mutation rate - uses a Geometric distribution
         */
        public void mutate() {
//            System.out.println("Before: " + toString());
            for (int bitLocation = Geometric.nextRandom(); bitLocation < 64; bitLocation += 1 + Geometric.nextRandom() ) {
                bitVector = bitVector ^ (1L << bitLocation);
            }
//            System.out.println("After: " + toString());
            calcAll();
        }

        public String toString() {
            String output = bitVectorToString(bitVector); //", bitVectorValue: " + bitVector +
            output += ", totalPrice: " + totalPrice + ", totalWeight: " + totalWeight + ", fitness: " + fitness;
            return output;
        }

        public String bitVectorToString(long vector) {
            String output = "";
            for (int bit = numItems - 1; bit >= 0; bit--) {
                output += (vector & (1L << bit)) > 0 ? 1 : 0;
                if(bit % 5 == 0) output += " ";
            }
            return output;
        }

        /**
         * Calculate all of the relevant stats for the current member (weight, price, fitness)
         */
        private void calcAll() {
            totalWeight = 0;
            totalPrice = 0;
            for (int i = 0; i < items.size(); i++) {
                long bitMask = 1L << i;
                totalWeight += (bitVector & bitMask) != 0 ? items.get(i).weight: 0;
                totalPrice += (bitVector & bitMask) != 0 ? items.get(i).price: 0;
            }
            totalPrice *= DECIMAL_ADJUST;
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

    /**
     * Fetch the weight/price combinations from the file
     * @param fileName The items.csv file
     * @return A list of items
     */
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

    /**
     * Record generational stats to a file to be graphed
     * @param fileName The file to save the stats in
     * @param stats The actual stats to be saved
     */
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

    /**
     * An individual object that can be considered for the knapsack problem
     */
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

    /**
     * A geometric distribution, used to step along the genome and determine where the next mutation should be
     */
    public static class Geometric {
        static Random random = new Random(); // uniform random
        static double probOfMutation = .05;

        /**
         * Takes a uniform distribution and converts it into a geometric distribution
         * @return
         */
        public static int nextRandom() {
            int stepSizeToNextMutation = (int)(Math.log(1-random.nextDouble())/Math.log(1-probOfMutation));

//            System.out.println("Step size: " + stepSizeToNextMutation + ", probOfMutation: " + probOfMutation);
            return stepSizeToNextMutation;
        }
        public static void setChanceOfMutation(Random _random, double prob) {
            random = _random;
            probOfMutation = prob;
        }
    }
}
