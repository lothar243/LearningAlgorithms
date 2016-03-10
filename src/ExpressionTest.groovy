/**
 *
 */
class ExpressionTest extends GroovyTestCase {
    Expression firstExpression = createExpression(1,2,3,4);
    Expression secondExpression = createExpression(1,2,3,null);
    Expression thirdExpression = createExpression(null, null, null, null); // reminder: null is used for wildcard
    Expression fourthExpression = createExpression(2, 2, 3, 4);
    Expression nullExpression = new Expression();
    Expression mixedExpression = createExpression("test", 2, "test2", null);

    Double[] firstArray = [1d, 2d, 3d, 4d];
    DataPoint firstTestPoint = new DataPoint(firstArray, 0);
    DataPoint secondTestPoint = new DataPoint(firstArray, 1);

    Double[] secondArray = [1d, 2d, 3d, 3d];
    DataPoint thirdTestPoint = new DataPoint(secondArray, 0);
    DataPoint fourthTestPoint = new DataPoint(secondArray, 1);

    Object[] thirdArray = ["test", 2d, "test2", 4];
    DataPoint thirdArrayPositivePoint = new DataPoint(thirdArray, 0);
    DataPoint thirdArrayNegativePoint = new DataPoint(thirdArray, 1);

    void testIsMoreGeneralThan() {
        assertTrue(createExpression(1,2,3,null).isMoreGeneralThan(createExpression(1,2,3,4)));
        assertFalse(createExpression(1,2,3,4).isMoreGeneralThan(createExpression(1,2,3,4)));
        assertFalse(createExpression(1,2,3,4).isMoreGeneralThan(createExpression(1,2,3,null)));
        assertFalse(createExpression(1,2,3,null).isMoreGeneralThan(createExpression(1,2,3,null)))
        assertTrue(thirdExpression.isMoreGeneralThan(mixedExpression));
    }
    void testIsMoreSpecificThan() {
        assertFalse(createExpression(1,2,3,null).isMoreSpecificThan(createExpression(1,2,3,4)));
        assertFalse(createExpression(1,2,3,4).isMoreSpecificThan(createExpression(1,2,3,4)));
        assertTrue(createExpression(1,2,3,4).isMoreSpecificThan(createExpression(1,2,3,null)));
        assertFalse(createExpression(1,2,3,null).isMoreSpecificThan(createExpression(1,2,3,null)));
        assertFalse(thirdExpression.isMoreSpecificThan(mixedExpression));
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

        assertTrue(thirdExpression.isSatisfiedBy(thirdArrayPositivePoint));
        assertFalse(thirdExpression.isSatisfiedBy(thirdArrayNegativePoint));

        assertTrue(mixedExpression.isSatisfiedBy(thirdArrayPositivePoint));
        assertFalse(mixedExpression.isSatisfiedBy(thirdArrayNegativePoint));
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
    void testCopyWithValue() {
        Expression copyOfSecond = secondExpression.copyWithValueAtPosition(3, new AttributeValue(4d));
        assertTrue(firstExpression.equals(copyOfSecond));

        Expression copyOfFourth = fourthExpression.copyWithValueAtPosition(0, new AttributeValue(1d));
        assertTrue(firstExpression.equals(copyOfFourth));
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
    void testRemoveMoreGeneralExpressions() {
        ArrayList<Expression> testBoundary = new ArrayList<>();
        testBoundary.add(firstExpression);
        testBoundary.add(firstExpression);
        testBoundary.add(secondExpression);
        testBoundary.add(secondExpression);
        Expression.removeMoreGeneralExpressions(testBoundary);
        assertEquals(1, testBoundary.size());
        assertTrue(testBoundary.get(0).equals(firstExpression));
    }
    void testRemoveMoreSpecificExpressions() {
        ArrayList<Expression> testBoundary = new ArrayList<>();
        testBoundary.add(firstExpression);
        testBoundary.add(firstExpression);
        testBoundary.add(secondExpression);
        testBoundary.add(secondExpression);
        Expression.removeMoreSpecificExpressions(testBoundary);
        assertEquals(1, testBoundary.size());
        assertTrue(testBoundary.get(0).equals(secondExpression));
    }
    void testMinimalSpecifications() {
        ArrayList<ArrayList<AttributeValue>> possibleValues = new ArrayList<>();
        possibleValues.add(new ArrayList<AttributeValue>());
        possibleValues.get(0).add(new AttributeValue(1d));
        possibleValues.get(0).add(new AttributeValue(2d));

        possibleValues.add(new ArrayList<AttributeValue>());
        possibleValues.get(1).add(new AttributeValue(1d));
        possibleValues.get(1).add(new AttributeValue(2d));

        possibleValues.add(new ArrayList<AttributeValue>());
        possibleValues.get(2).add(new AttributeValue(3d));
        possibleValues.get(2).add(new AttributeValue(4d));

        possibleValues.add(new ArrayList<AttributeValue>());
        possibleValues.get(3).add(new AttributeValue(3d));
        possibleValues.get(3).add(new AttributeValue(4d));

        ArrayList<Expression> specifications = secondExpression.minimalSpecifications(fourthTestPoint, possibleValues);
        // starts out as (1,2,3,*). After the minimal specification, it should just be (1,2,3,4)
        assertEquals(1, specifications.size());
        assertTrue(specifications.get(0).equals(firstExpression));

        specifications = thirdExpression.minimalSpecifications(secondTestPoint, possibleValues);
        Expression[] expectedExpressions = new Expression[4];
        expectedExpressions[0] = createExpression(2, null, null, null);
        expectedExpressions[1] = createExpression(null, 1, null, null);
        expectedExpressions[2] = createExpression(null, null, 4, null);
        expectedExpressions[3] = createExpression(null, null, null, 3);
        assertEquals(4, specifications.size());
        for (int i = 0; i < 4; i++) {
            assertTrue(specifications.get(i).equals(expectedExpressions[i]));
        }

        specifications = Expression.initialGeneralBoundary(4);
        Expression.minimallySpecify(specifications, secondTestPoint, possibleValues);
        assertEquals(4, specifications.size());


    }
    public void testInitialGeneralBoundary() {
        ArrayList<Expression> generalBoundary = Expression.initialGeneralBoundary(4);
        assertEquals(1, generalBoundary.size());
        Expression initialExpression = generalBoundary.get(0);
        System.out.println("Initial expression from boundary: " + initialExpression);
        for (int i = 0; i < 4; i++) {
            assertTrue(initialExpression.values[i].isWildcard());
        }
    }

    public static Expression createExpression(Object a, Object b, Object c, Object d) {
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
        testBoundary.add(mixedExpression);
        return testBoundary;
    }

}
