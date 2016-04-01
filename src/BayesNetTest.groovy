/**
 * Created by jeff on 3/31/16.
 */
class BayesNetTest extends GroovyTestCase {
    static final double EPSILON = 0.0001d;

    public void testProbability() {
        Data data = createTestData();
        assertEquals(1, BayesNet.marginalProbability(data, 0, new AttributeValue(1), createIntArrayList(1), createAttList(0)), EPSILON);
        assertEquals(0.5, BayesNet.marginalProbability(data, 1, new AttributeValue(1), createIntArrayList(0), createAttList(1)), EPSILON);
        assertEquals(1, BayesNet.marginalProbability(data, 1, new AttributeValue(1), createIntArrayList(0, 2), createAttList(1, 0)), EPSILON);
        assertEquals(2/3, BayesNet.probability(data, 0, new AttributeValue(1)), EPSILON);
        assertEquals(1/3, BayesNet.probability(data, 0, new AttributeValue(0)), EPSILON);
        assertEquals(2/3, BayesNet.marginalProbability(data, 0, new AttributeValue(1), createIntArrayList(1, 2), createAttList(1, 1)), EPSILON);
    }

    public Data createTestData() {
        DataPoint first = new DataPoint(createDoubleArray(1, 1, 0));
        DataPoint second = new DataPoint(createDoubleArray(1, 0, 1));
        DataPoint third = new DataPoint(createDoubleArray(0, 1, 0));

        Data data = new Data();
        data.initializeForBinaryData("0");
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
}
