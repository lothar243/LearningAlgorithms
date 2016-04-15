/**
 * Created by jeff on 3/31/16.
 */
class BayesNetTest extends GroovyTestCase {
    static final double EPSILON = 0.0001d;


    public Data createTestData() {
        DataPoint first = new DataPoint(createDoubleArray(1, 1, 0));
        DataPoint second = new DataPoint(createDoubleArray(1, 0, 1));
        DataPoint third = new DataPoint(createDoubleArray(0, 1, 0));

        Data data = new Data();
        data.initializeForBinaryData("0");
        String[] attributeNames = ["x_1", "x_2", "x_3", "class"]
        data.setAttributeNames(attributeNames);

        data.addDataPoint(first, "1");
        data.addDataPoint(second, "0");
        data.addDataPoint(third, "1");
        return data;
    }

    public Double[] createDoubleArray(Double... args) {
        Double[] output = new Double[args.length];
        for (int i = 0; i < args.length; i++) {
            output[i] = args[i];
        }
        return output;
    }
    public ArrayList<Integer> createIntArrayList(Integer... args) {
        ArrayList<Integer> output = new ArrayList<>();
        for(Integer arg: args) {
            output.add(arg);
        }
        return output;
    }
    public ArrayList<AttributeValue> createAttList(Double... args) {
        ArrayList<AttributeValue> output = new ArrayList<>();
        for(Double arg: args) {
            output.add(new AttributeValue(arg));
        }
        return output;
    }
    public DataPoint createDataPoint(Double[] args) {
        return new DataPoint(createAttList(args))
    }
    public Double createDouble(double a) {
        Double output = a;
        return output;
    }

    public void testLogFact() {
        assertEquals(Math.log(1), BayesNet.logFact(1), EPSILON);
        assertEquals(Math.log(2), BayesNet.logFact(2), EPSILON);
        assertEquals(Math.log(6), BayesNet.logFact(3), EPSILON);
        assertEquals(Math.log(24), BayesNet.logFact(4), EPSILON);
    }

    public Data createK2TestData() {
        Data data = new Data();
        String[] attributeNames = ["x_1", "x_2", "x_3", "class"]
        data.setAttributeNames(attributeNames);
        data.initializeForBinaryData("0"); // making a positive string 0 so that it matches the index
        data.addDataPoint(FileIO.parseAttributes("1 0 0".split(), 3), "1");
        data.addDataPoint(FileIO.parseAttributes("1 1 1".split(), 3), "1");
        data.addDataPoint(FileIO.parseAttributes("0 0 1".split(), 3), "0");
        data.addDataPoint(FileIO.parseAttributes("1 1 1".split(), 3), "1");
        data.addDataPoint(FileIO.parseAttributes("0 0 0".split(), 3), "0");
        data.addDataPoint(FileIO.parseAttributes("0 1 1".split(), 3), "0");
        data.addDataPoint(FileIO.parseAttributes("1 1 1".split(), 3), "1");
        data.addDataPoint(FileIO.parseAttributes("0 0 0".split(), 3), "0");
        data.addDataPoint(FileIO.parseAttributes("1 1 1".split(), 3), "1");
        data.addDataPoint(FileIO.parseAttributes("0 0 0".split(), 3), "0");
//        System.out.println(data.toString());
        return data;
    }
    public void testK2Function() {
        Data data = createK2TestData();

        System.out.println("Test 1");
        assertEquals(Math.log(1.0/2772),
                BayesNet.k2Formula(
                        data.dataPoints,
                        data.inferPossibleAttributeValues(),
                        0,
                        new ArrayList<Integer>()),
                EPSILON);
        System.out.println("\nTest 2");
        assertEquals(Math.log(1.0/2772),
                BayesNet.k2Formula(
                        data.dataPoints,
                        data.inferPossibleAttributeValues(),
                        1,
                        new ArrayList<Integer>()),
                EPSILON);
        System.out.println("\nTest 3");
        ArrayList<Integer> parentIndices = new ArrayList<>();
        parentIndices.add(0);
        assertEquals(Math.log(1.0/900),
                BayesNet.k2Formula(
                        data.dataPoints,
                        data.inferPossibleAttributeValues(),
                        1,
                        parentIndices),
                EPSILON);
        System.out.println("\nTest 4");
        assertEquals(Math.log(1.0/2310),
                BayesNet.k2Formula(
                        data.dataPoints,
                        data.inferPossibleAttributeValues(),
                        2,
                        new ArrayList<Integer>()),
                EPSILON);

        System.out.println("\nTest 5");
        parentIndices = new ArrayList<>();
        parentIndices.add(0);
        assertEquals(Math.log(1.0/1800),
                BayesNet.k2Formula(
                        data.dataPoints,
                        data.inferPossibleAttributeValues(),
                        2,
                        parentIndices),
                EPSILON);

        System.out.println("\nTest 6");
        parentIndices = new ArrayList<>();
        parentIndices.add(1);
        assertEquals(Math.log(1.0/180),
                BayesNet.k2Formula(
                        data.dataPoints,
                        data.inferPossibleAttributeValues(),
                        2,
                        parentIndices),
                EPSILON);

        System.out.println("\nTest 7");
        parentIndices = new ArrayList<>();
        parentIndices.add(0);
        parentIndices.add(1);
        assertEquals(Math.log(1.0/400),
                BayesNet.k2Formula(
                        data.dataPoints,
                        data.inferPossibleAttributeValues(),
                        2,
                        parentIndices),
                EPSILON);

    }

    public void testK2Algorithm() {
        Data data = createK2TestData();
        ArrayList<Integer> nodeOrdering = new ArrayList<>();
        nodeOrdering.add(0);
        nodeOrdering.add(1);
        nodeOrdering.add(2);

        BayesNet.k2Algorithm(data.dataPoints, nodeOrdering, 2);
    }

    public void testClassificationProbs() {
        Data data = createK2TestData();

        ArrayList<ArrayList<Integer>> parentIndicesList = new ArrayList<>();
        parentIndicesList.add(new ArrayList<Integer>());
        parentIndicesList.add(new ArrayList<Integer>());
        parentIndicesList.add(new ArrayList<Integer>());
        DataPoint testPoint = FileIO.parseAttributes("0 0 0".split(), 3);
        double[] classificationProbs = BayesNet.classificationProbs(data, parentIndicesList, testPoint);
        assertEquals(((5+1)/(5+12)) * (4 + 1)/(5 + 12) * (3+1)/(4+12), classificationProbs[0], EPSILON);
        assertEquals((0+1)/(5+12) * (1 + 1)/(5 + 12) * (1 + 1) / (4 + 12), classificationProbs[1], EPSILON);

        parentIndicesList.set(1, createIntArrayList(2));
        classificationProbs = BayesNet.classificationProbs(data, parentIndicesList, testPoint);
        /**
         * this will cull the list to: when looking at dimension 1
         * 1 0 0
         * 0 0 0
         * 0 0 0
         * 0 0 0
         */
        assertEquals((5+1)/(5+12)*(3+1)/(4+12)*(3+1)/(4+12), classificationProbs[0], EPSILON);
        assertEquals((0+1)/(5+12) * (1+1)/(4+12) * (1+1)/(4+12), classificationProbs[1], EPSILON)
    }

    public void testPredictClass() {
        Data data = createTestData();

        /**
         * 1 1 0 - 1
         * 1 0 1 - 0
         * 0 1 0 - 1
         */


        ArrayList<ArrayList<Integer>> parentIndicesLists = new ArrayList<>();
        // running naive bayes first
        parentIndicesLists.add(new ArrayList<Integer>());
        parentIndicesLists.add(new ArrayList<Integer>());
        parentIndicesLists.add(new ArrayList<Integer>());

        assertEquals(1, BayesNet.predictClassification(data, parentIndicesLists, FileIO.parseAttributes("1 1 0".split(), 3)));
        assertEquals(0, BayesNet.predictClassification(data, parentIndicesLists, FileIO.parseAttributes("1 0 1".split(), 3)));
        assertEquals(1, BayesNet.predictClassification(data, parentIndicesLists, FileIO.parseAttributes("0 1 0".split(), 3)));
        assertEquals(1, BayesNet.predictClassification(data, parentIndicesLists, FileIO.parseAttributes("0 0 0".split(), 3)));
        assertEquals(1, BayesNet.predictClassification(data, parentIndicesLists, FileIO.parseAttributes("1 1 0".split(), 3)));
        assertEquals(1, BayesNet.predictClassification(data, parentIndicesLists, FileIO.parseAttributes("1 1 1".split(), 3)));

        // now to include a hierarchy
        parentIndicesLists = new ArrayList<>();
        // running naive bayes first
        parentIndicesLists.add(new ArrayList<Integer>());
        parentIndicesLists.add(new ArrayList<Integer>());
        parentIndicesLists.add(createIntArrayList(1)); // column 2 depends on column 1

        assertEquals(1, BayesNet.predictClassification(data, parentIndicesLists, FileIO.parseAttributes("1 1 0".split(), 3)));
        assertEquals(0, BayesNet.predictClassification(data, parentIndicesLists, FileIO.parseAttributes("1 0 1".split(), 3)));
        assertEquals(1, BayesNet.predictClassification(data, parentIndicesLists, FileIO.parseAttributes("0 1 0".split(), 3)));
        assertEquals(0, BayesNet.predictClassification(data, parentIndicesLists, FileIO.parseAttributes("0 0 0".split(), 3)));
        assertEquals(1, BayesNet.predictClassification(data, parentIndicesLists, FileIO.parseAttributes("1 1 0".split(), 3)));
        assertEquals(1, BayesNet.predictClassification(data, parentIndicesLists, FileIO.parseAttributes("1 1 1".split(), 3)));
    }
}
