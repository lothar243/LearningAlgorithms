/**
 * A generic type with some necessary methods
 */
class AttributeValue {

    Object value;

    public static AttributeValue[] createArray(Object[] values) {
        AttributeValue[] array = new AttributeValue[values.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = new AttributeValue(values[i]);
        }
        return array;
    }

    public AttributeValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Double plus(AttributeValue other) {
        if (this.isNumeric() && other.isNumeric()) {
            return (Double) value + (Double) other.getValue();
        }
        return null;
    }

    public boolean isLessThan(Double other) {
        return this.isNumeric() && (Double) value < other;
    }

    public boolean isGreaterThan(Double other) {
        return this.isNumeric() && (Double) value > other;
    }

    public boolean isNumeric() {
        if(value == null) return false;
        Class classType = value.getClass();
        return classType.equals(Double.class) || classType.equals(Integer.class) || classType.equals(Float.class);
    }
    public boolean isString() {
        if(value == null) return false;
        return value.getClass().equals(String.class);
    }

    public String toString() {
        return "" + value;
    }

    @Override
    public boolean equals(Object otherObject) {

        if (!otherObject.getClass().equals(AttributeValue.class)) {
            return false;
        }
        // now it's safe to cast
        AttributeValue other = (AttributeValue)otherObject;
        if(this.isWildcard()) {
            return other.isWildcard();
        }
        Object otherValue = other.getValue();
        System.out.println(value.getClass() + " and " + otherValue.getClass());
        if(this.isNumeric() && other.isNumeric()) {
            final double EPSILON = .000001d;
            System.out.println("both are numbers: " + value + " and " + otherValue);
            return Math.abs((0d + value) - ((Double)otherValue)) < EPSILON;
        }
        if(value.getClass().equals(String.class)) {
            return ((String)value).equals((String)otherValue);
        }
        return value.equals(other.getValue());
    }

    public Double minus(AttributeValue other) {
        if (this.isNumeric() && other.isNumeric()) {
            return (Double) value - (Double) other.getValue();
        }
        return null;
    }

    public Double minus(Double other) {
        if (this.isNumeric()) {
            return (Double) value - other;
        }
        return null
                ;
    }

    public Double getDouble() {
        if (this.isNumeric()) return (Double) value;
        return null;
    }

    public boolean isWildcard() {
        return (value == null);
    }
    public boolean isNotWildcard() {
        return value != null;
    }

    public void setToWildcard() {
        value = null;
    }


    public AttributeValue copyOf() {
        Object copiedValue = value;
        if(value.getClass().equals(String.class)) {
            copiedValue = "" + value;
        }

        return new AttributeValue(value);
    }
}
