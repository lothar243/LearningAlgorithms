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
        boolean positiveStringSpecified = false;

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
                        positiveStringSpecified = true;
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
            System.out.println("You must specify a training file with \"-t FILENAME\"");
            System.exit(1);
        }
        if((testDataFile == null && crossFoldNumFolds == -1) || (testDataFile != null && crossFoldNumFolds != -1)) {
            System.out.println("You must specify a test file (with \"-T FILENAME\") or use cross fold validation (with \"-x 10\"), but not both");
            System.exit(1);
        }
        else {
            if(crossFoldNumFolds != -1) {
                System.out.println("Using " + crossFoldNumFolds + "-fold validation");
            }
            else {
                System.out.println("Using the test file " + testDataFile);
            }
        }
        if(positiveStringSpecified) {
            System.out.println("Using \"" + positiveString + "\" as the positive classification");
        }
        else {
            System.out.println("No positive classification specified, defaulting to \"1\"");
        }
        System.out.println();

        // read in training data from file
        Data data = new Data();
        data.initializeForBinaryData(positiveString);
        FileIO.readFromFile(trainingDataFile, data);
        final int numAttributes = data.numAttributes;

        ArrayList<ArrayList<AttributeValue>> possibleValues = data.inferPossibleAttributeValues();

        if(crossFoldNumFolds > 0) {
            double overallAccuracy = 0;
            data.initializeDataForCrossFoldValidation(crossFoldNumFolds);
            for (int foldNumber = 0; foldNumber < crossFoldNumFolds; foldNumber++) {
                ArrayList<DataPoint> trainingPoints = data.getCrossFoldTrainingDataPoints(foldNumber);
                ArrayList<Expression> testRules = generateTestRules(trainingPoints, possibleValues, numAttributes);
                System.out.println("Iteration: " + foldNumber);
                double accuracyOfCurrentRules = 100 * determineAccuracy(data.getCrossFoldTestDataPoints(foldNumber), testRules, verbose);
                System.out.println("Accuracy: " + MyTools.roundTo(accuracyOfCurrentRules, 2));
                if(verbose) {
                    System.out.println("Version space: " + testRules + "\n");
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

    /**
     * Generates the general and specific boundaries of the candidate elimination algorithm based on the training data
     * @param trainingData A list of DataPoints used for training
     * @param possibleValues A double indexed ArrayList, giving all possible values of each of the attributes
     * @param numAttributes The total number of attributes
     * @return The list of expressions generated after running the algorithm
     */
    public static ArrayList<Expression> generateTestRules(ArrayList<DataPoint> trainingData, ArrayList<ArrayList<AttributeValue>> possibleValues,
                                                             int numAttributes) {
        ArrayList<Expression> generalBoundary = Expression.initialGeneralBoundary(numAttributes);
        ArrayList<Expression> specificBoundary = Expression.initialSpecificBoundary();


        for(DataPoint point: trainingData) {
            if(point.classificationIndex == 0) { // positive example
                Expression.removeInconsistentExpressions(generalBoundary, point);
                Expression.minimallyGeneralize(specificBoundary, generalBoundary, point);
                Expression.removeMoreGeneralExpressions(specificBoundary);
            }
            else { // negative example
                Expression.removeInconsistentExpressions(specificBoundary, point);
                Expression.minimallySpecify(generalBoundary, specificBoundary, point, possibleValues);
                Expression.removeMoreSpecificExpressions(generalBoundary);
            }

        }
        return Expression.generateVersionSpace(specificBoundary, generalBoundary, possibleValues);
    }

    /**
     * Determine the accuracy of a particular list of expressions
     * @param testPoints Points to test the expressions against
     * @param rules The list of expressions used for prediction
     * @param verbose True to output missed classifications to the console
     * @return Number of correct predictions divided by number of total predictions - in the interval [0,1]
     */
    public static double determineAccuracy(ArrayList<DataPoint> testPoints, ArrayList<Expression> rules, boolean verbose) {
        int truePositives = 0, falsePositives = 0, trueNegatives = 0, falseNegatives = 0;
        int numPointsTested = 0;
        int numPointsCorrect = 0;
        for(DataPoint point: testPoints) {
            boolean classifiedAsPositive = Expression.atLeastOneSatisfies(rules, point);
            boolean correctClassification = (classifiedAsPositive && point.classificationIndex == 0) ||
                    (!classifiedAsPositive && point.classificationIndex != 0);
            if(correctClassification) {
                numPointsCorrect++;
                if(point.classificationIndex == 0)
                    truePositives++;
                else
                    trueNegatives++;
            }
            else {
                if(verbose) System.out.println(point + " was classified incorrectly");
                if(point.classificationIndex == 0)
                    falseNegatives++;
                else
                    falsePositives++;
            }
            numPointsTested++;
        }

        if(verbose) {
            System.out.println("Confusion Matrix: \n" +
                    "               Predicted               \n" +
                    "               \tPos\tNeg\n" +
                    "Actual     Pos \t" + truePositives + "\t" + falseNegatives + "\n" +
                    "           Neg \t" + falsePositives + "\t" + trueNegatives + "\n");
        }
        return (double)numPointsCorrect / numPointsTested;
    }

    /**
     * Print some output to help guide the user on the correct use of the command line arguments
     */
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

    /**
     * Determine if the current expression is more general than another expression
     * @param other The expression being compared against
     * @return True if the current expression has more wildcards overall, and is at least as general than the other in all attributes
     */
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

    /**
     * Determine if the current expression is more specific than another expression
     * @param other The expression being compared against
     * @return True if the current expression has fewer wildcards overall, and is at least as specific than the other in all attributes
     */
    public boolean isMoreSpecificThan(Expression other) {
        return other.isMoreGeneralThan(this);
    }

    /**
     * Tests a point against a list of expressions to determine if the point is predicted to be positive
     * @param expressions The list of expressions to test agains
     * @param point The point we're trying to predict the class of
     * @return True if the point matches any of the expressions in the list
     */
    public static boolean atLeastOneSatisfies(ArrayList<Expression> expressions, DataPoint point) {
        for(Expression expression: expressions) {
            if(expression.isSatisfiedBy(point)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if the current expression correctly classifies the given point
     * @param point The point to test
     * @return True if the point is classified as positive and actually is, or if it's classified as negative and actually is
     */
    public boolean isConsistentWith(DataPoint point) {
        if(isSatisfiedBy(point)) {
            return point.classificationIndex == 0; // true if this is a positive example
        }
        return point.classificationIndex != 0; // true if this is a negative example
    }

    /**
     * Test to see if the given point matches the pattern of the current expression
     * @param point The point to test
     * @return True if there is a match, False if not
     */
    public boolean isSatisfiedBy(DataPoint point) {
        if(nullExpression || values == null) return false;
        for (int i = 0; i < point.attributes.length; i++) {
            if(!(values[i].isWildcard() || values[i].equals(point.attributes[i]))) {
                return false; // something didn't match
            }
        }
        return true; // we looked through each attribute, and they all matched or were wildcards
    }

    /**
     * Gives a starting point for the general boundary, which is just a single expression with all wildcards
     * @param numAttributes The number of attributes of each of the dataPoints (Also, the number of wildcards)
     * @return A list containing only a full-wildcard expression
     */
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

    /**
     * Create a copy of the Expression, but replaces the value at a given position with a wildcard
     * @param position The position of the desired wildcard
     * @return The copied (and altered) expression
     */
    public Expression copyWithWildcardAtPosition(int position) {
        Expression modifiedCopy = this.copyOf();
        if(modifiedCopy.nullExpression || modifiedCopy.values == null) {
            System.out.println("Error, can't give a wildcard in positions to a null expression");
            System.exit(2);
        }
        modifiedCopy.values[position].setToWildcard();
        return modifiedCopy;
    }
    /**
     * Create a copy of the Expression, but a value at the given position
     * @param position The position to place the value
     * @return The copied (and altered) expression
     */
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

    /**
     * Generates an initial boundary for the specific side of candidate elimination
     * @return An ArrayList with only the null expression
     */
    public static ArrayList<Expression> initialSpecificBoundary() {
        ArrayList<Expression> output = new ArrayList<>();
        output.add(new Expression());
        return output;
    }

    /**
     * Generates expressions that are minimally generalizations of the current expression such that they are satisfied
     * by a given point and some member of G is more general than the expression
     * @param point The point that should satisfy the expressions
     * @return All minimally generalized expressions that are satisfied by the point
     */
    public ArrayList<Expression> minimalGeneralizations(DataPoint point, ArrayList<Expression> generalBoundary) {
        if(point.classificationIndex != 0) return null; // this should only be used with positive examples
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
                if(newExpression.isMoreSpecificThanAtLeastOne(generalBoundary)) {
                    if (newExpression.isConsistentWith(point)) {
                        // we are general enough to satisfy the point
                        output.add(newExpression);
                    } else {
                        // get more general
                        output.addAll(newExpression.minimalGeneralizations(point, generalBoundary));
                    }
                }
            }
        }
        // so far, this is just a bunch of generalizations. To make it minimal, we remove any that are too general
        Expression.removeMoreGeneralExpressions(output);
        return output;
    }

    public boolean isMoreSpecificThanAtLeastOne(ArrayList<Expression> generalBoundary) {
        for(Expression expression: generalBoundary) {
            if(this.isMoreSpecificThan(expression)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMoreGeneralThanAtLeastOne(ArrayList<Expression> specificBoundary) {
        for(Expression expression: specificBoundary) {
            if(this.isMoreGeneralThan(expression)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates expressions that are minimally more specific of the current expression such that they are satisfied
     * by a given point
     * @param point The point that should satisfy the expressions
     * @return All minimally more specific expressions that are satisfied by the point
     */
    public ArrayList<Expression> minimalSpecifications(DataPoint point,
                                                       ArrayList<ArrayList<AttributeValue>> possibleValues,
                                                       ArrayList<Expression> specificBoundary) {
        ArrayList<Expression> output = new ArrayList<>();
        // we begin by ensuring that our inputs are as expected... a negative example that does not satisfy the current
        // expression and is not the null expression
        if(point.classificationIndex == 0) return output;
        if(this.isConsistentWith(point)) {
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
                    if(specifiedExpression.isMoreGeneralThanAtLeastOne(specificBoundary)) {
                        if(specifiedExpression.isConsistentWith(point)) {
                            output.add(specifiedExpression);
                        }
                        else {
                            output.addAll(specifiedExpression.minimalSpecifications(point, possibleValues, specificBoundary));
                        }
                    }
                }
            }
        }
        Expression.removeMoreSpecificExpressions(output);
        return output;
    }

    @Override
    public String toString() {
        if(nullExpression || values == null) {
            return "Null expression";
        }
        String[] stringValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            if(values[i] == null || values[i].isWildcard()) {
                stringValues[i] = "?";
            }
            else {
                stringValues[i] = values[i].toString();
            }
        }
        return "Expression{" +
                "values=" + Arrays.toString(stringValues) + "}";
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

    /**
     * Remove all expressions from a list that are not satisfied by the given point
     * @param boundary The list of expressions
     * @param point The point that should satisfy all of the expressions
     */
    public static void removeInconsistentExpressions(ArrayList<Expression> boundary, DataPoint point) {
        // i'm counting down here instead of up because I'm going to be removing expressions and I don't want the
        // changing indices to cause problems
        for (int i = boundary.size() - 1; i >= 0; i--) {
            if(!boundary.get(i).isConsistentWith(point)) {
                boundary.remove(i);
            }
        }
    }

    /**
     * Remove any expression from a list for which there is a more specific expression
     * @param specificBoundary The list of expressions (The boundary from the specific side)
     */
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

    /**
     * Remove any expressions from a list for which there are more general expressions
     * @param generalBoundary The list of expressions (The boundary from the general side)
     */
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
    public static void minimallyGeneralize(ArrayList<Expression> specificBoundary, ArrayList<Expression> generalBoundary, DataPoint point) {
        if(point.classificationIndex != 0) {
            System.out.println("Error, negative examples should not generalize the S boundary");
            return;
        }
        for(int i = specificBoundary.size() - 1; i >= 0; i--) {
            Expression currentExpression = specificBoundary.get(i);
            if(!currentExpression.isConsistentWith(point)) {
                specificBoundary.remove(i);
                specificBoundary.addAll(currentExpression.minimalGeneralizations(point, generalBoundary));
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
    public static void minimallySpecify(ArrayList<Expression> generalizedBoundary,
                                        ArrayList<Expression> specificBoundary,
                                        DataPoint point,
                                        ArrayList<ArrayList<AttributeValue>> possibleValues) {
        if(point.classificationIndex == 0) {
            System.out.println("Error, positive examples should not be used to specialize the G boundary");
            return;
        }
        for(int i = generalizedBoundary.size() - 1; i >= 0; i--) {
            Expression currentExpression = generalizedBoundary.get(i);
            if(!currentExpression.isConsistentWith(point)) {
                generalizedBoundary.remove(i);
                generalizedBoundary.addAll(currentExpression.minimalSpecifications(point, possibleValues, specificBoundary));
            }
        }
        Expression.removeMoreSpecificExpressions(generalizedBoundary);
    }

    public static ArrayList<Expression> generateVersionSpace(ArrayList<Expression> specificBoundary,
                                                             ArrayList<Expression> generalBoundary,
                                                             ArrayList<ArrayList<AttributeValue>> possibleValues) {
        ArrayList<Expression> expressionQueue = new ArrayList<>(), versionSpace = new ArrayList<>();
        // working from the general side to the specific side
        versionSpace.addAll(specificBoundary);
        expressionQueue.addAll(generalBoundary);
        // work through each of the expressions, and examine all of the possible values for each wildcard

        // for each attribute that is currently a wildcard, create expressions for each of the possible values of that attribute
        while(!expressionQueue.isEmpty()) {
            Expression currentExpression = expressionQueue.get(0);
            expressionQueue.remove(0);
            if(currentExpression.values == null) {
                System.out.println("Encountered an error, found an expression with null for attributes: " + currentExpression.toString());
                break;
            }
            // if we've already seen this expression, there's no reason to go any further
            if(!versionSpace.contains(currentExpression)) {
                // add this expression to the version space, and put all of its children that are general than at
                // least one expression on the specific boundary on the queue
                versionSpace.add(currentExpression);
                for (int attIndex = 0; attIndex < possibleValues.size(); attIndex++) {
                    // for each attribute that has a wildcard, look at each of it's possible values
                    if(currentExpression.values[attIndex].isWildcard()) {
                        for(AttributeValue possibleValue: possibleValues.get(attIndex)) {
                            Expression childExpression = currentExpression.copyWithValueAtPosition(attIndex, possibleValue);
                            if(childExpression.isMoreGeneralThanAtLeastOne(specificBoundary)) {
                                expressionQueue.add(childExpression);
                            }
                        }
                    }
                }
            }
        }
        return versionSpace;
    }
}
