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
    DataPoint firstArrayPositivePoint = new DataPoint(firstArray, 0);
    DataPoint firstArrayNegativePoint = new DataPoint(firstArray, 1);

    Double[] secondArray = [1d, 2d, 3d, 3d];
    DataPoint secondArrayPositivePoint = new DataPoint(secondArray, 0);
    DataPoint secondArrayNegativePoint = new DataPoint(secondArray, 1);

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

        assertTrue(firstExpression.isSatisfiedBy(firstArrayPositivePoint));
        assertFalse(firstExpression.isSatisfiedBy(firstArrayNegativePoint));

        assertTrue(secondExpression.isSatisfiedBy(firstArrayPositivePoint));
        assertFalse(secondExpression.isSatisfiedBy(firstArrayNegativePoint));

        assertTrue(thirdExpression.isSatisfiedBy(firstArrayPositivePoint));
        assertFalse(thirdExpression.isSatisfiedBy(firstArrayNegativePoint));

        assertFalse(fourthExpression.isSatisfiedBy(firstArrayPositivePoint));
        assertTrue(fourthExpression.isSatisfiedBy(firstArrayNegativePoint));

        assertFalse(nullExpression.isSatisfiedBy(firstArrayPositivePoint));
        assertTrue(nullExpression.isSatisfiedBy(firstArrayNegativePoint));

        assertTrue(thirdExpression.isSatisfiedBy(thirdArrayPositivePoint));
        assertFalse(thirdExpression.isSatisfiedBy(thirdArrayNegativePoint));

        assertTrue(mixedExpression.isSatisfiedBy(thirdArrayPositivePoint));
        assertFalse(mixedExpression.isSatisfiedBy(thirdArrayNegativePoint));

        assertTrue(createExpression(null, 1, null, null).isSatisfiedBy(firstArrayNegativePoint));

    }

    void testRemoveInconsistentExpressions() {
        ArrayList<Expression> testBoundary = createBoundary();
        Expression.removeInconsistentExpressions(testBoundary, firstArrayPositivePoint);
        assertEquals(3, testBoundary.size());
        assertTrue(testBoundary.get(0).equals(firstExpression));
        assertTrue(testBoundary.get(1).equals(secondExpression));
        assertTrue(testBoundary.get(2).equals(thirdExpression));

        testBoundary = createBoundary();
        Expression.removeInconsistentExpressions(testBoundary, firstArrayNegativePoint);
        assertEquals(3, testBoundary.size());
        assertTrue(testBoundary.get(0).equals(fourthExpression));
        assertTrue(testBoundary.get(1).equals(nullExpression));
        assertTrue(testBoundary.get(2).equals(mixedExpression));

        testBoundary = createBoundary();
        Expression.removeInconsistentExpressions(testBoundary, thirdArrayPositivePoint);
        assertEquals(2, testBoundary.size());
        assertTrue(testBoundary.get(0).equals(thirdExpression));
        assertTrue(testBoundary.get(1).equals(mixedExpression));

        testBoundary = createBoundary();
        Expression.removeInconsistentExpressions(testBoundary, thirdArrayNegativePoint);
        assertEquals(4, testBoundary.size());
        assertTrue(testBoundary.get(0).equals(firstExpression));
        assertTrue(testBoundary.get(1).equals(secondExpression));
        assertTrue(testBoundary.get(2).equals(fourthExpression));
        assertTrue(testBoundary.get(3).equals(nullExpression));

    }
    void testCopyWithWildcard() {
        assertTrue(firstExpression.copyWithWildcardAtPosition(3).equals(secondExpression));
        // repeatedly calling this...
        Expression test = secondExpression.copyWithWildcardAtPosition(0).copyWithWildcardAtPosition(1).copyWithWildcardAtPosition(2);
        assertTrue(test.equals(thirdExpression));
        test = mixedExpression.copyWithWildcardAtPosition(0);
        Expression expectedExpression = createExpression(null, 2, "test2", null);
        assertTrue(test.equals(expectedExpression));
    }
    void testCopyWithValue() {
        Expression copyOfSecond = secondExpression.copyWithValueAtPosition(3, new AttributeValue(4d));
        assertTrue(firstExpression.equals(copyOfSecond));

        Expression copyOfFourth = fourthExpression.copyWithValueAtPosition(0, new AttributeValue(1d));
        assertTrue(firstExpression.equals(copyOfFourth));

        //second expression -> mixed expression
        copyOfSecond = secondExpression.copyWithValueAtPosition(0, new AttributeValue("test"));
        copyOfSecond = copyOfSecond.copyWithValueAtPosition(2, new AttributeValue("test2"));
        assertTrue(copyOfSecond.equals(mixedExpression));

        // mixed expression -> second expression
        Expression copyOfMixed = mixedExpression.copyWithValueAtPosition(0, new AttributeValue(1));
        copyOfMixed = copyOfMixed.copyWithValueAtPosition(2, new AttributeValue(3));
        assertTrue(copyOfMixed.equals(secondExpression));
    }

    void testMinimalGeneralizations() {
        ArrayList<Expression> generalizations = firstExpression.minimalGeneralizations(secondArrayPositivePoint);
        // ensure all resulting expressions have a wildcard in the last position
        for(Expression expression: generalizations) {
            assertTrue(expression.values[3].isWildcard());
        }
        Expression.removeMoreGeneralExpressions(generalizations);
        assertEquals(1, generalizations.size());
        assertTrue(generalizations.get(0).equals(secondExpression));

        generalizations = firstExpression.minimalGeneralizations(thirdArrayPositivePoint);
        for(Expression expression: generalizations) {
            assertTrue(expression.values[0].isWildcard());
            assertTrue(expression.values[2].isWildcard());
        }
        Expression.removeMoreGeneralExpressions(generalizations);
        assertEquals(1, generalizations.size());
        assertEquals(2d, generalizations.get(0).values[1].getDouble());


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
        possibleValues.get(0).add(new AttributeValue("test"));

        possibleValues.add(new ArrayList<AttributeValue>());
        possibleValues.get(1).add(new AttributeValue(1d));
        possibleValues.get(1).add(new AttributeValue(2d));

        possibleValues.add(new ArrayList<AttributeValue>());
        possibleValues.get(2).add(new AttributeValue(3d));
        possibleValues.get(2).add(new AttributeValue(4d));
        possibleValues.get(2).add(new AttributeValue("test2"));

        possibleValues.add(new ArrayList<AttributeValue>());
        possibleValues.get(3).add(new AttributeValue(3d));
        possibleValues.get(3).add(new AttributeValue(4d));

        ArrayList<Expression> specifications = secondExpression.minimalSpecifications(secondArrayNegativePoint, possibleValues);
        // starts out as (1,2,3,*). After the minimal specification, it should just be (1,2,3,4)
        assertEquals(1, specifications.size());
        assertTrue(specifications.get(0).equals(firstExpression));


        specifications = thirdExpression.minimalSpecifications(firstArrayNegativePoint, possibleValues);
//        System.out.println(specifications);
        assertEquals(6, specifications.size());
        assertTrue(specifications.get(0).equals(createExpression(2, null, null, null)));
        assertTrue(specifications.get(1).equals(createExpression("test", null, null, null)));
        assertTrue(specifications.get(2).equals(createExpression(null, 1, null, null)));
        assertTrue(specifications.get(3).equals(createExpression(null, null, 4, null)));
        assertTrue(specifications.get(4).equals(createExpression(null, null, "test2", null)));
        assertTrue(specifications.get(5).equals(createExpression(null, null, null, 3)));

        specifications = Expression.initialGeneralBoundary(4);
        Expression.minimallySpecify(specifications, firstArrayNegativePoint, possibleValues);
        assertEquals(6, specifications.size());


    }
    public void testInitialGeneralBoundary() {
        ArrayList<Expression> generalBoundary = Expression.initialGeneralBoundary(4);
        assertEquals(1, generalBoundary.size());
        Expression initialExpression = generalBoundary.get(0);
//        System.out.println("Initial expression from boundary: " + initialExpression);
        for (int i = 0; i < 4; i++) {
            assertTrue(initialExpression.values[i].isWildcard());
        }
    }

    public void testAccuracy() {
        ArrayList<Expression> boundary = new ArrayList<>();
        boundary.add(createExpression(1, null, 3, null));
        boundary.add(createExpression(1, 2, null, null));

        ArrayList<DataPoint> dataPoints = new ArrayList<>();
        dataPoints.add(createDataPoint(1,2,3,4,0))
        dataPoints.add(createDataPoint(1,2,4,4,0))
        dataPoints.add(createDataPoint(1,3,3,3,0))
        assertEquals(1d, CandidateElimination.determineAccuracy(dataPoints, boundary, false));

        dataPoints.add(createDataPoint(2, 2, 3, 4, 0)); // will be incorrectly classified
        assertEquals(0.75d, CandidateElimination.determineAccuracy(dataPoints, boundary, false));

        dataPoints.add(createDataPoint(2, 2, 3, 4, 1)); // will be accurately classified
        assertEquals(0.8d, CandidateElimination.determineAccuracy(dataPoints, boundary, false));

        dataPoints.add(createDataPoint(1, 4, 3, 0, 1)); // will be incorrectly classified
        assertEquals(2d/3, CandidateElimination.determineAccuracy(dataPoints, boundary, false));

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

    public static DataPoint createDataPoint(Object a, Object b, Object c, Object d, int classIndex) {
        Object[] objects = new Object[4];
        objects[0] = a;
        objects[1] = b;
        objects[2] = c;
        objects[3] = d;
        return new DataPoint(objects, classIndex);
    }

    //////////////////////////////////////   AttributeValue tests /////////////////////////////////////
    void testAttributeEquals() {
        AttributeValue first = new AttributeValue(0);
        AttributeValue second = new AttributeValue(1);
        AttributeValue third = new AttributeValue(0d);
        AttributeValue fourth = new AttributeValue("test");

        assertTrue(first.equals(first));
        assertFalse(first.equals(second));
        assertTrue(first.equals(third));
        assertFalse(first.equals(fourth));

        assertFalse(second.equals(first));
        assertTrue(second.equals(second));
        assertFalse(second.equals(third));
        assertFalse(second.equals(fourth));

        assertTrue(third.equals(first));
        assertFalse(third.equals(second));
        assertTrue(third.equals(third));
        assertFalse(third.equals(fourth));

        assertFalse(fourth.equals(first));
        assertFalse(fourth.equals(second));
        assertFalse(fourth.equals(third));
        assertTrue(fourth.equals(fourth));
    }

    ///////////////////////////////// DataPoint Tests ///////////////////////////////////////////////
    public void testCopyOf() {
        Object[] objects = new Object[3];
        objects[0] = 1d;
        objects[1] = "hi";
        objects[2] = null;
        DataPoint point = new DataPoint(objects, 0);

        DataPoint point2 = point.copyOf();
//        System.out.println("Point: " + point);
//        System.out.println("Point2: " + point2);
        assertTrue(point.equals(point2));
    }
}
