import java.util.ArrayList;

/**
 * A generic type with some necessary methods
 */
class AttributeValue {

    Object value;

    public static AttributeValue[] createArray(Double[] values) {
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
        if (this.isDouble() && other.isDouble()) {
            return (Double) value + (Double) other.getValue();
        }
        return null;
    }

    public boolean isLessThan(Double other) {
        return this.isDouble() && (Double) value < other;
    }

    public boolean isGreaterThan(Double other) {
        return this.isDouble() && (Double) value > other;
    }

    public boolean isDouble() {
        if(value == null) return false;
        return value.getClass().equals(Double.class);
    }

    public String toString() {
        return "" + value;
    }

    @Override
    public boolean equals(Object other) {
        if (!other.getClass().equals(AttributeValue.class)) {
            return false;
        }
        // now it's safe to cast
        AttributeValue otherValue = (AttributeValue)other;
        if(this.isWildcard()) {
            return otherValue.isWildcard();
        }
        return value.equals(otherValue.getValue());
    }

    public Double minus(AttributeValue other) {
        if (this.isDouble() && other.isDouble()) {
            return (Double) value - (Double) other.getValue();
        }
        return null;
    }

    public Double minus(Double other) {
        if (this.isDouble()) {
            return (Double) value - other;
        }
        return null
                ;
    }

    public Double getDouble() {
        if (this.isDouble()) return (Double) value;
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
        return new AttributeValue(value);
    }
}
