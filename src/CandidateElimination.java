import java.util.ArrayList;
import java.util.Arrays;

public class CandidateElimination {

    public static void main(String[] args) {
        boolean verbose = false;
        boolean showDecisionTree = false;
        float sufficientEntropy = 0;
        String trainingDataFile = null;
        String testDataFile = null;
        int crossFoldNumFolds = -1;
        String positiveString = "1";

        // read in optional arguments
        try {
            for (int argNum = 0; argNum < args.length; argNum++) {
                switch (args[argNum]) {
                    case "-t":
                        trainingDataFile = args[argNum + 1];
                        System.out.println("Using training data file: " + trainingDataFile);
                        argNum++;
                        break;
                    case "-T":
                        testDataFile = args[argNum + 1];
                        argNum++;
                        break;
                    case "-v":
                    case "--verbose":
                        verbose = true;
                        break;
                    case "-x":
                        crossFoldNumFolds = Integer.parseInt(args[argNum + 1]);
                        argNum++;
                        break;
                    case "-h":
                    case "-help":
                        printHelpString();
                        break;
                    case "-p":
                        positiveString = args[argNum + 1];
                        argNum++;
                        break;
                    default:
                        System.out.println("Unknown argument encountered: " + args[argNum] + " - use -h for help");
                        System.exit(0);
                }
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(0);

        }

        if(trainingDataFile == null) {
            System.out.println("You must specify a training file");
            System.exit(1);
        }
        if((testDataFile == null && crossFoldNumFolds == -1) || (testDataFile != null && crossFoldNumFolds != -1)) {
            System.out.println("You must specify a test file or use cross fold validation (but not both)");
            System.exit(1);
        }

        // read in training data from file
        Data data = new Data();
        data.initializeForBinaryData(positiveString);
        FileIO.readFromFile(trainingDataFile, data);
        final int numAttributes = data.numAttributes;
        for(DataPoint point: data.dataPoints) {
            System.out.println(point);
        }

        ArrayList<ArrayList<AttributeValue>> possibleValues = data.inferPossibleAttributeValues();

        if(crossFoldNumFolds > 0) {
            double overallAccuracy = 0;
            data.initializeDataForCrossFoldValidation(crossFoldNumFolds);
            for (int foldNumber = 0; foldNumber < crossFoldNumFolds; foldNumber++) {
                ArrayList<DataPoint> trainingPoints = data.getCrossFoldTrainingData(foldNumber);
                ArrayList<Expression> testRules = generateTestRules(trainingPoints, possibleValues, numAttributes);
                double accuracyOfCurrentRules = 100 * determineAccuracy(data.getCrossFoldTestData(foldNumber), testRules, verbose);
                System.out.println("Accuracy " + MyTools.roundTo(accuracyOfCurrentRules, 2));
                if(verbose) {
                    System.out.println("Expressions: " + testRules);
                }
                overallAccuracy += accuracyOfCurrentRules;
            }
            overallAccuracy /= crossFoldNumFolds;
            System.out.println("Overall accuracy: " + MyTools.roundTo(overallAccuracy, 2));
        }
        else { // a specific testing file has been specified
            Data testData = new Data();
            testData.initializeForBinaryData(positiveString);
            FileIO.readFromFile(testDataFile, testData);

            ArrayList<Expression> testRules = generateTestRules(data.dataPoints, possibleValues, numAttributes);
            double accuracyOfRules = 100 * determineAccuracy(testData.dataPoints, testRules, verbose);
            System.out.println("Accuracy " + MyTools.roundTo(accuracyOfRules, 2));
            if(verbose) {
                System.out.println("Expressions: " + testRules);
            }
        }

    }

    public static ArrayList<Expression> generateTestRules(ArrayList<DataPoint> trainingData, ArrayList<ArrayList<AttributeValue>> possibleValues,
                                                             int numAttributes) {
        ArrayList<Expression> generalBoundary = Expression.initialGeneralBoundary(numAttributes);
        ArrayList<Expression> specificBoundary = Expression.initialSpecificBoundary();


        for(DataPoint point: trainingData) {
            if(point.classificationIndex == 0) { // positive example
                Expression.removeInconsistentExpressions(generalBoundary, point);
                Expression.minimallyGeneralize(specificBoundary, point);
                Expression.removeMoreGeneralExpressions(specificBoundary);
            }
            else { // negative example
                Expression.removeInconsistentExpressions(specificBoundary, point);
                Expression.minimallySpecify(generalBoundary, point, possibleValues);
                Expression.removeMoreSpecificExpressions(generalBoundary);
            }

        }
        // combine the boundaries to get a complete list of expressions
        generalBoundary.addAll(specificBoundary);
        return generalBoundary;
    }
    public static double determineAccuracy(ArrayList<DataPoint> testPoints, ArrayList<Expression> rules, boolean verbose) {
        int numPointsTested = 0;
        int numPointsCorrect = 0;
        System.out.println("Test Points:");
        for(DataPoint point: testPoints) {
            System.out.println(point);
        }
        for(DataPoint point: testPoints) {
            boolean classifiedAsPositive = Expression.classifiedAsPositive(rules, point);
            boolean correctClassification = (classifiedAsPositive && point.classificationIndex == 0) ||
                    (!classifiedAsPositive && point.classificationIndex != 0);
            if(correctClassification) {
                numPointsCorrect++;
            }
            else {
                System.out.println(point + " was classified incorrectly");
            }
            numPointsTested++;
        }
        return (double)numPointsCorrect / numPointsTested;
    }

    public static void printHelpString() {
        final String helpString = "\nUsage: ./CandidateElimination.sh -t trainingData.csv <optional arguments>\n\n" +
                "Decision Tree implementation: Uses ID3, a greedy algorithm that prefers questions that maximize" +
                "information gain.\n\n" +
                "Optional Arguments: \n" +
                "\t-T testData.csv\n" +
                "\t\tSpecify which data to use as a test set\n" +
                "\t-x NUM\n" +
                "\t\tNUM-fold cross validation\n" +
                "\t-v\n" +
                "\t\tVerbose - show expressions";

        System.out.println(helpString);
        System.exit(1);

    }
}


class Expression {

    final AttributeValue[] values;
    final boolean nullExpression;

    /**
     * Constructor for most expressions
     * @param values Values required for a point to be classified as a positive example
     */
    public Expression(AttributeValue[] values) {
        this.values = values;
        nullExpression = false;
    }

    /**
     * Constructor for a null expression
     */
    public Expression() {
        values = null;
        nullExpression = true;
    }

    public boolean isMoreGeneralThan(Expression other) {
        if(nullExpression || this.values == null) return false;
        if(other.values == null) return true;
        boolean moreBlanks = false;
        if(this.values.length != other.values.length) {
            return false;
        }
        for (int i = 0; i < this.values.length; i++) {
            // return false if there is a more specific attribute
            if(other.values[i].isNotWildcard()) {
                if(this.values[i].isNotWildcard()) {
                    // neither values is a wildcard, so they must match
                    if(!this.values[i].equals(other.values[i])) return false;
                    // if they do match, nothing changes
                }
                else {
                    // the other value is not null but this value is, so this attribute is more general
                    moreBlanks = true;
                }
            }
            else {
                if(this.values[i] != null && this.values[i].getValue() != null) {
                    // the other value is null but this value isn't, so this attribute is more specific
                    return false;
                }
                else {
                    // both values are null, so no change
                }
            }
        }
        return moreBlanks;
    }
    public boolean isMoreSpecificThan(Expression other) {
        return other.isMoreGeneralThan(this);
    }

    public static boolean classifiedAsPositive(ArrayList<Expression> expressions, DataPoint point) {
        for(Expression expression: expressions) {
            if(expression.classifyAsPositive(point)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSatisfiedBy(DataPoint point) {
        if(classifyAsPositive(point)) {
//            System.out.println("classified as positive");
            return point.classificationIndex == 0; // true if this is a positive example
        }
//        System.out.println("classified as negative");
        return point.classificationIndex != 0; // true if this is a negative example
    }

    public boolean classifyAsPositive(DataPoint point) {
        if(nullExpression || values == null) return false;
        for (int i = 0; i < point.attributes.length; i++) {
            if(!(values[i].isWildcard() || values[i].equals(point.attributes[i]))) {
                return false; // something didn't match
            }
        }
        return true; // we looked through each attribute, and they all matched or were wildcards
    }

    public static ArrayList<Expression> initialGeneralBoundary(int numAttributes) {
        AttributeValue[] attributeValues = new AttributeValue[numAttributes];
        for (int i = 0; i < numAttributes; i++) {
            attributeValues[i] = new AttributeValue(null);
        }
        ArrayList<Expression> output = new ArrayList<>();
        Expression initialExpression = new Expression(attributeValues) ;
        output.add(initialExpression);
        return output;
    }

    public Expression copyWithWildcardAtPosition(int position) {
        Expression modifiedCopy = this.copyOf();
        if(modifiedCopy.nullExpression || modifiedCopy.values == null) {
            System.out.println("Error, can't give a wildcard in positions to a null expression");
            System.exit(2);
        }
        modifiedCopy.values[position].setToWildcard();
        return modifiedCopy;
    }
    public Expression copyWithValueAtPosition(int position, AttributeValue value) {
        Expression modifiedCopy = this.copyOf();
        if(modifiedCopy.nullExpression || modifiedCopy.values == null) {
            System.out.println("Error, can't specify a null expression");
            System.exit(5);
        }
        modifiedCopy.values[position] = new AttributeValue(value.getValue());
        return modifiedCopy;
    }
    public Expression copyOf() {
        // performing a deep copy
        if(this.values == null || this.nullExpression) {
            return new Expression();
        }
        AttributeValue[] attributeValues = new AttributeValue[this.values.length];
        for (int i = 0; i < attributeValues.length; i++) {
            attributeValues[i] = this.values[i].copyOf();

        }
        return new Expression(attributeValues);
    }

    public static ArrayList<Expression> initialSpecificBoundary() {
        ArrayList<Expression> output = new ArrayList<>();
        output.add(new Expression());
        return output;
    }

    public ArrayList<Expression> minimalGeneralizations(DataPoint point) {
        if(point.classificationIndex != 0) return null; // this should only be used with positive examples
        if(this.isSatisfiedBy(point)) {
            // no generalization is necessary, the point already satisfies the expression
            ArrayList<Expression> trivialList = new ArrayList<>();
            trivialList.add(this);
            return trivialList;
        }
        ArrayList<Expression> output = new ArrayList<>();
        // if the specific boundary is currently the null expression, set it to accept only return true for the current point
        if(this.nullExpression || this.values == null) {
            output.add(new Expression(point.attributes));
            return output;
        }
        // try adding a wildcard to one position that doesn't have one
        // if the resulting expression still isn't satisfied by the point, recurse
        for (int i = 0; i < point.attributes.length; i++) {
            if(this.values[i].isNotWildcard()) {
                Expression newExpression = this.copyWithWildcardAtPosition(i);
                if(!newExpression.isSatisfiedBy(point)) {
                    output.addAll(newExpression.minimalGeneralizations(point));
                }
                else {
                    output.add(newExpression);
                }
            }
        }
        // so far, this is just a bunch of generalizations. To make it minimal, we remove any that are too general
        Expression.removeMoreGeneralExpressions(output);
        return output;
    }

    public ArrayList<Expression> minimalSpecifications(DataPoint point,
                                                        ArrayList<ArrayList<AttributeValue>> possibleValues) {
        ArrayList<Expression> output = new ArrayList<>();
        // we begin by ensuring that our inputs are as expected... a negative example that does not satisfy the current
        // expression and is not the null expression
        if(point.classificationIndex == 0) return output;
        if(this.isSatisfiedBy(point)) {
            output.add(this);
            return output;
        }
        if(this.nullExpression || this.values == null) {
            System.out.println("Error, a null expression didn't satisfy a negative example");
            System.exit(4);
        }

        // for each attribute that is currently a wildcard, create expressions for each of the possible values of that attribute
        // if a resulting expression is still not satisfied by the point, recurse
        for(int i = 0; i < point.attributes.length; i++) {
            if(this.values[i].isWildcard()) {
                for(AttributeValue possibleValue: possibleValues.get(i)) {
                    Expression specifiedExpression = this.copyWithValueAtPosition(i, possibleValue);
                    if(specifiedExpression.isSatisfiedBy(point)) {
                        output.add(specifiedExpression);
                    }
                    else {
                        output.addAll(specifiedExpression.minimalSpecifications(point, possibleValues));
                    }
                }
            }
        }
//        System.out.println("Before culling: " + output);
        Expression.removeMoreSpecificExpressions(output);
//        System.out.println("After culling: " + output);
        return output;
    }

    @Override
    public String toString() {
        return "Expression{" +
                "values=" + Arrays.toString(values) +
                ", nullExpression=" + nullExpression +
                '}';
    }

    public boolean equals(Object other) {
        if(!other.getClass().equals(Expression.class)) return false;
        Expression otherExpression = (Expression) other;
        if(this.nullExpression) {
            return otherExpression.nullExpression;
        }
        if(this.values == null || otherExpression.values == null) return false;
        if(this.values.length != otherExpression.values.length) return false;
        for (int i = 0; i < this.values.length; i++) {
            if(!this.values[i].equals(otherExpression.values[i])) return false;
        }
        return true;
    }

    public static void removeInconsistentExpressions(ArrayList<Expression> boundary, DataPoint point) {
        // i'm counting down here instead of up because I'm going to be removing expressions and I don't want the
        // changing indices to cause problems
        for (int i = boundary.size() - 1; i >= 0; i--) {
            if(!boundary.get(i).isSatisfiedBy(point)) {
                boundary.remove(i);
            }
        }
    }
    public static void removeMoreGeneralExpressions(ArrayList<Expression> specificBoundary) {
        // used to remove unwanted expressions from the specific boundary (anything that is more general)
        for (int i = specificBoundary.size() - 1; i >= 0; i--) {
            Expression currentExpression = specificBoundary.get(i);
            for (int j = 0; j < specificBoundary.size(); j++) {
                if(i != j) {
                    Expression otherExpression = specificBoundary.get(j);
                    if (currentExpression.isMoreGeneralThan(otherExpression) || currentExpression.equals(otherExpression)) {
                        specificBoundary.remove(i); //remove the current expression from the list and move on to the next
                        break;
                    }
                }
            }
            for(Expression otherExpression: specificBoundary) {
            }
        }
    }
    public static void removeMoreSpecificExpressions(ArrayList<Expression> generalBoundary) {
        for(int i = generalBoundary.size() - 1; i >= 0; i--) {
            Expression currentExpression = generalBoundary.get(i);
            for (int j = 0; j < generalBoundary.size(); j++) {
                if(i != j) {
                    Expression otherExpression = generalBoundary.get(j);
                    if(currentExpression.isMoreSpecificThan(otherExpression) || currentExpression.equals(otherExpression)) {
                        generalBoundary.remove(i);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Find expressions for which the point is inconsistent, remove them from the boundary and add in all minimal
     * generalizations such that the new expressions are satisfied by the point
     * @param specificBoundary The S boundary
     * @param point A positive example (It has a class of 1)
     */
    public static void minimallyGeneralize(ArrayList<Expression> specificBoundary, DataPoint point) {
        if(point.classificationIndex != 0) {
            System.out.println("Error, negative examples should not generalize the S boundary");
            return;
        }
        for(int i = specificBoundary.size() - 1; i >= 0; i--) {
            Expression currentExpression = specificBoundary.get(i);
            if(!currentExpression.isSatisfiedBy(point)) {
                specificBoundary.remove(i);
                specificBoundary.addAll(currentExpression.minimalGeneralizations(point));
            }
        }
    }

    /**
     * Find expressions for which a negative example is inconsistent, the remove them and minimally specialize the
     * boundary so that the negative example is correctly classified
     * @param generalizedBoundary The G boundary
     * @param point A negative example (Class of 0)
     * @param possibleValues All possible values of each of the attributes - needed to generate specializations
     */
    public static void minimallySpecify(ArrayList<Expression> generalizedBoundary, DataPoint point,
                                        ArrayList<ArrayList<AttributeValue>> possibleValues) {
        if(point.classificationIndex == 0) {
            System.out.println("Error, positive examples should not be used to specialize the G boundary");
            return;
        }
        for(int i = generalizedBoundary.size() - 1; i >= 0; i--) {
            Expression currentExpression = generalizedBoundary.get(i);
            if(!currentExpression.isSatisfiedBy(point)) {
                generalizedBoundary.remove(i);
                generalizedBoundary.addAll(currentExpression.minimalSpecifications(point, possibleValues));
            }
        }
        Expression.removeMoreSpecificExpressions(generalizedBoundary);
    }


}
