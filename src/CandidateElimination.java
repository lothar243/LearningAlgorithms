import java.util.ArrayList;
import java.util.Arrays;

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

    public Expression(AttributeValue[] values) {
        this.values = values;
        nullExpression = false;
    }
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
        if(nullExpression || this.values == null) {
            System.out.println("Error, can't give a wildcard in positions to a null expression");
            System.exit(2);
        }
        AttributeValue[] attributeValues = new AttributeValue[this.values.length];
        for (int i = 0; i < attributeValues.length; i++) {
            // performing a deep copy
            attributeValues[i] = new AttributeValue(this.values[i].getDouble());
        }
        attributeValues[position].setToWildcard();
        return new Expression(attributeValues);
    }

    public static ArrayList<Expression> initialSpecificBoundary() {
        ArrayList<Expression> output = new ArrayList<>();
        output.add(new Expression());
        return output;
    }

    public ArrayList<Expression> minimalGeneralizations(DataPoint point) {
        ArrayList<Expression> output = new ArrayList<>();
        if(this.nullExpression || this.values == null) {
            output.add(new Expression(point.attributes));
            return output;
        }
        // now look through each of the values that is not a wildcard, and add a new expression that makes it a wildcard
        // if the point still doesn't satisfy the more generalized expression, recurse
        for (int i = 0; i < point.attributes.length; i++) {
            if(this.values[i].isNotWildcard()) {
                //todo finish
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

    /**
     * Find expressions for which the point is inconsistent, remove them from the boundary and add in all minimal
     * generalizations such that the new expressions are satisfied by the point
     * @param boundary The S boundary
     * @param point A positive example (It has a class of 1)
     */
    public static void minimallyGeneralize(ArrayList<Expression> boundary, DataPoint point,
                                           ArrayList<ArrayList<AttributeValue>> possibleValues) {
        for(int i = boundary.size(); i >= 0; i--) {
            if(!boundary.get(i).isSatisfiedBy(point)) {
                Expression removedExpression = boundary.get(i);
                boundary.remove(i);
                boundary.addAll(removedExpression.minimalGeneralizations(point));
            }
        }
    }


}
