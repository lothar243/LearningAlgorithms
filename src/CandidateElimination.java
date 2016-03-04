import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class CandidateElimination {

    public static void main(String[] args) {
        boolean verbose = false;
        boolean showDecisionTree = false;
        float sufficientEntropy = 0;
        String trainingDataFile = null;
        String testDataFile = null;

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
                    case "-h":
                    case "-help":
                        printHelpString();
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
            System.out.println("You must specify a training file, use -h for help");
            System.exit(1);
        }

        // read in training data from file
        Data trainingData = new Data();
        FileIO.readFromFile(trainingDataFile, trainingData);
        final int numAttributes = trainingData.numAttributes;


        ArrayList<Expression> generalBoundary = Expression.initialGeneralBoundary(numAttributes);
        ArrayList<Expression> specificBoundary = Expression.initialSpecificBoundary();

    }

    public static void printHelpString() {
        final String helpString = "\nUsage: ./CandidateElimination.sh -t trainingData.csv -T testData.csv <optional arguments>\n\n" +
                "Decision Tree implementation: Uses ID3, a greedy algorithm that prefers questions that maximize" +
                "information gain.\n\n" +
                "Optional Arguments: \n" +
                "\t-v, --verbose\n" +
                "\t\tverbose - show more information\n" +
                "\t-tree\n" +
                "\t\tshow full decision tree" +
                "\t-e FLOAT" +
                "\t\tspecify a sufficient entropyOf, range 0 - 1 (Default 0: Completely homogeneous data)";
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
        if(nullExpression) return false;
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

    public boolean isSatisfiedBy(DataPoint point) {
        if(nullExpression || values == null) return point.classificationIndex == 0;
        for (int i = 0; i < point.attributes.length; i++) {
            if(!(values[i].isWildcard() || values[i].equals(point.attributes[i]))) {
                return point.classificationIndex == 0; // something didn't match
            }
        }
        return point.classificationIndex == 1; // we looked through each attribute, and they all matched or were wildcards
    }

    public static ArrayList<Expression> initialGeneralBoundary(int numAttributes) {
        AttributeValue[] attributeValues = new AttributeValue[numAttributes];
        for (int i = 0; i < numAttributes; i++) {
            attributeValues = null;
        }
        ArrayList<Expression> output = new ArrayList<>();
        output.add(new Expression(attributeValues));
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
            System.out.println("Error, can't specialize a null expression");
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
            attributeValues[i] = new AttributeValue(this.values[i].getDouble());
        }
        return new Expression(attributeValues);
    }

    public static ArrayList<Expression> initialSpecificBoundary() {
        ArrayList<Expression> output = new ArrayList<>();
        output.add(new Expression());
        return output;
    }

    public ArrayList<Expression> minimalGeneralizations(DataPoint point) {
        if(point.classificationIndex == 0) return null; // this should only be used with positive examples
        if(this.isSatisfiedBy(point)) {
            // no generalization is necessary, the point already satisfies the expression
            ArrayList<Expression> trivialList = new ArrayList<>();
            trivialList.add(this);
            return trivialList;
        }
        ArrayList<Expression> output = new ArrayList<>();
        // if the specialized boundary is currently the null expression, set it to accept only return true for the current point
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

    private ArrayList<Expression> minimalSpecializations(DataPoint point,
                                                          ArrayList<ArrayList<AttributeValue>> possibleValues) {
        // we begin by ensuring that our inputs are as expected... a negative example that does not satisfy the current
        // expression and is not the null expression
        if(point.classificationIndex == 1) return null;
        if(this.isSatisfiedBy(point)) {
            ArrayList<Expression> trivialList = new ArrayList<>();
            trivialList.add(this);
            return trivialList;
        }
        ArrayList<Expression> output = new ArrayList<>();
        if(this.nullExpression || this.values == null) {
            System.out.println("Error, a null expression didn't satisfy a negative example");
            System.exit(4);
        }

        // for each attribute that is currently a wildcard, create expressions for each of the possible values of that attribute
        // if a resulting expression is still not satisfied by the point, recurse
        for(int i = 0; i < point.attributes.length; i++) {
            if(this.values[i].isWildcard()) {

            }
        }
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
            for(Expression expression: specificBoundary) {
                if(specificBoundary.get(i).isMoreGeneralThan(expression)) {
                    specificBoundary.remove(i);
                    break;
                }
            }
        }
    }

    /**
     * Find expressions for which the point is inconsistent, remove them from the boundary and add in all minimal
     * generalizations such that the new expressions are satisfied by the point
     * @param specializedBoundary The S boundary
     * @param point A positive example (It has a class of 1)
     */
    public static void minimallyGeneralize(ArrayList<Expression> specializedBoundary, DataPoint point,
                                           ArrayList<ArrayList<AttributeValue>> possibleValues) {
        if(point.classificationIndex == 0) {
            System.out.println("Error, negative examples should not generalize the S boundary");
            return;
        }
        for(int i = specializedBoundary.size(); i >= 0; i--) {
            Expression currentExpression = specializedBoundary.get(i);
            if(!currentExpression.isSatisfiedBy(point)) {
                specializedBoundary.remove(i);
                specializedBoundary.addAll(currentExpression.minimalGeneralizations(point));
            }
        }
        Expression.removeMoreGeneralExpressions(specializedBoundary);
    }

    /**
     * Find expressions for which a negative example is inconsistent, the remove them and minimally specialize the
     * boundary so that the negative example is correctly classified
     * @param generalizedBoundary The G boundary
     * @param point A negative example (Class of 0)
     * @param possibleValues All possible values of each of the attributes - needed to generate specializations
     */
    public static void minimallySpecialize(ArrayList<Expression> generalizedBoundary, DataPoint point,
                                           ArrayList<ArrayList<AttributeValue>> possibleValues) {
        if(point.classificationIndex == 1) {
            System.out.println("Error, positive examples should not be used to specialize the G boundary");
            return;
        }
        for(int i = generalizedBoundary.size() - 1; i >= 0; i--) {
            Expression currentExpression = generalizedBoundary.get(i);
            if(!currentExpression.isSatisfiedBy(point)) {
                generalizedBoundary.remove(i);
                generalizedBoundary.addAll(currentExpression.minimalSpecializations(point, possibleValues));
            }
        }
        // todo Expression.removeMoreSpecializedExpressions(generalizedBoundary);
    }


}
