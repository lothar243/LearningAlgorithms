/**
 *
 */
class ExpressionTest extends GroovyTestCase {
    Expression firstExpression = createExpression(1,2,3,4);
    Expression secondExpression = createExpression(1,2,3,null);
    Expression thirdExpression = createExpression(null, null, null, null); // reminder: null is used for wildcard
    Expression fourthExpression = createExpression(2, 2, 3, 4);
    Expression nullExpression = new Expression();

    Double[] firstArray = [1d, 2d, 3d, 4d];
    DataPoint firstTestPoint = new DataPoint(firstArray, 1);
    DataPoint secondTestPoint = new DataPoint(firstArray, 0);

    Double[] secondArray = [1d, 2d, 3d, 3d];
    DataPoint thirdTestPoint = new DataPoint(secondArray, 1);
    DataPoint fourthTestPoint = new DataPoint(secondArray, 0);

    void testIsMoreGeneralThan() {
        assertTrue(createExpression(1,2,3,null).isMoreGeneralThan(createExpression(1,2,3,4)));
        assertFalse(createExpression(1,2,3,4).isMoreGeneralThan(createExpression(1,2,3,4)));
        assertFalse(createExpression(1,2,3,4).isMoreGeneralThan(createExpression(1,2,3,null)));
        assertFalse(createExpression(1,2,3,null).isMoreGeneralThan(createExpression(1,2,3,null)))
    }

    void testIsSatisfiedBy() {

        assertTrue(firstExpression.isSatisfiedBy(firstTestPoint));
        assertFalse(firstExpression.isSatisfiedBy(secondTestPoint));

        assertTrue(secondExpression.isSatisfiedBy(firstTestPoint));
        assertFalse(secondExpression.isSatisfiedBy(secondTestPoint));

        assertTrue(thirdExpression.isSatisfiedBy(firstTestPoint));
        assertFalse(thirdExpression.isSatisfiedBy(secondTestPoint));

        assertFalse(fourthExpression.isSatisfiedBy(firstTestPoint));
        assertTrue(fourthExpression.isSatisfiedBy(secondTestPoint));

        assertFalse(nullExpression.isSatisfiedBy(firstTestPoint));
        assertTrue(nullExpression.isSatisfiedBy(secondTestPoint));


    }

    void testRemoveInconsistentExpressions() {
        ArrayList<Expression> testBoundary = createBoundary();
        Expression.removeInconsistentExpressions(testBoundary, firstTestPoint);
        assertEquals(3, testBoundary.size());
        assertTrue(testBoundary.get(0).equals(firstExpression));
        assertTrue(testBoundary.get(1).equals(secondExpression));
        assertTrue(testBoundary.get(2).equals(thirdExpression));

        testBoundary = createBoundary();
        Expression.removeInconsistentExpressions(testBoundary, secondTestPoint);
        assertEquals(testBoundary.size(), 2);
        assertTrue(testBoundary.get(0).equals(fourthExpression));
        assertTrue(testBoundary.get(1).equals(nullExpression));

    }
    void testCopyWithWildcard() {
        assertTrue(firstExpression.copyWithWildcardAtPosition(3).equals(secondExpression));
        // repeatedly calling this...
        Expression test = secondExpression.copyWithWildcardAtPosition(0).copyWithWildcardAtPosition(1).copyWithWildcardAtPosition(2);
        assertTrue(test.equals(thirdExpression));
    }

    void testMinimalGeneralizations() {
        ArrayList<Expression> generalizations = firstExpression.minimalGeneralizations(thirdTestPoint);
        // ensure all resulting expressions have a wildcard in the last position
        for(Expression expression: generalizations) {
            assertTrue(expression.values[3].isWildcard());
        }
        Expression.removeMoreGeneralExpressions(generalizations);
        assertEquals(1, generalizations.size());
        assertTrue(generalizations.get(0).equals(secondExpression));

    }

    public static Expression createExpression(Double a, Double b, Double c, Double d) {
        AttributeValue[] attributeValues = new AttributeValue[4];
        attributeValues[0] = new AttributeValue(a);
        attributeValues[1] = new AttributeValue(b);
        attributeValues[2] = new AttributeValue(c);
        attributeValues[3] = new AttributeValue(d);
        return new Expression(attributeValues);
    }

    public ArrayList<Expression> createBoundary() {
        ArrayList<Expression> testBoundary = new ArrayList<>();
        testBoundary.add(firstExpression);
        testBoundary.add(secondExpression);
        testBoundary.add(thirdExpression);
        testBoundary.add(fourthExpression);
        testBoundary.add(nullExpression);
        return testBoundary;
    }

}
