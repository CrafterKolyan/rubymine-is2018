package com.jetbrains.python.inspection;

import java.math.BigDecimal;
import java.math.BigInteger;

public class PyValue {
    private enum Type {
        UNDEFINED,
        BIG_INTEGER,
        BIG_DECIMAL,
        OBJECT
    }

    static final PyValue ZERO = new PyValue(BigInteger.ZERO);
    static final PyValue ONE = new PyValue(BigInteger.ONE);

    private static final int precision = 19;

    private Type type;
    private Object value;

    private void setThis() {
        type = Type.UNDEFINED;
        value = null;
    }

    private void setThis(BigInteger bigInt) {
        type = Type.BIG_INTEGER;
        value = bigInt;
    }

    private void setThis(BigDecimal bigDec) {
        type = Type.BIG_DECIMAL;
        value = bigDec;
    }

    private void setThis(Object obj) {
        type = Type.OBJECT;
        value = obj;
    }

    PyValue() { setThis(); }
    PyValue(BigInteger bigInt) { setThis(bigInt); }
    PyValue(BigDecimal bigDec) { setThis(bigDec); }
    /*PyValue(Object obj) {
        if (obj == null) {
            setThis();
        } else if (obj instanceof BigInteger) {
            setThis((BigInteger) obj);
        } else if (obj instanceof BigDecimal) {
            setThis((BigDecimal) obj);
        } else {
            setThis(obj);
        }
    }*/

    boolean isDetermined() { return type != Type.UNDEFINED; }
    boolean isNumber() { return type == Type.BIG_INTEGER || type == Type.BIG_DECIMAL; }
    boolean isInteger() { return type == Type.BIG_INTEGER; }
    boolean isDouble() { return type == Type.BIG_DECIMAL; }
    boolean isObject() { return type == Type.OBJECT; }

    Object getValue() { return value; }

    BigInteger getBigInteger() {
        if (!isInteger()) {
            return BigInteger.ZERO;
        }
        return (BigInteger) value;
    }

    BigDecimal getBigDecimal() {
        if (!isNumber()) {
            return BigDecimal.ZERO;
        } else if (type == Type.BIG_INTEGER) {
            return new BigDecimal((BigInteger) value);
        }
        return (BigDecimal) value;
    }

    String getTypeString() {
        switch (type) {
            case BIG_INTEGER: return "integer";
            case BIG_DECIMAL: return "float";
            case UNDEFINED: return "undefined";
            case OBJECT: return "object";
        }
        return type.toString();
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object another) {
        if (!(another instanceof PyValue))
            return false;
        PyValue other = (PyValue)another;
        return value.equals(other.value);
    }

    int compareTo(PyValue other) {
        if (type == Type.UNDEFINED && other.type == Type.UNDEFINED) {
            return 0;
        } else if (type == Type.UNDEFINED || other.type == Type.UNDEFINED) {
            return (type == Type.UNDEFINED) ? -1 : 1;
        } else if (!isNumber() || !other.isNumber()) {
            return value.equals(other.value) ? 0 : -1;
        }
        if (isInteger() && other.isInteger()) {
            return ((BigInteger) value).compareTo((BigInteger) other.value);
        }
        return getBigDecimal().compareTo(other.getBigDecimal());
    }

    PyValue negate() {
        switch (type) {
            case BIG_INTEGER:
                return new PyValue(((BigInteger) value).negate());
            case BIG_DECIMAL:
                return new PyValue(((BigDecimal) value).negate());
            default:
                return new PyValue();
        }
    }

    PyValue add(PyValue other) {
        if (isInteger() && other.isInteger()) {
            return new PyValue(((BigInteger) value).add((BigInteger) other.value));
        } else if (isNumber() && other.isNumber()) {
            return new PyValue(getBigDecimal().add(other.getBigDecimal()));
        }
        return new PyValue();
    }

    PyValue subtract(PyValue other) {
        if (isInteger() && other.isInteger()) {
            return new PyValue(((BigInteger) value).subtract((BigInteger) other.value));
        } else if (isNumber() && other.isNumber()) {
            return new PyValue(getBigDecimal().subtract(other.getBigDecimal()));
        }
        return new PyValue();
    }

    PyValue multiply(PyValue other) {
        if (isInteger() && other.isInteger()) {
            return new PyValue(((BigInteger) value).multiply((BigInteger) other.value));
        } else if (isNumber() && other.isNumber()) {
            return new PyValue(getBigDecimal().multiply(other.getBigDecimal()));
        }
        return new PyValue();
    }

    PyValue divide(PyValue other) {
        if (isNumber() && other.isNumber()) {
            return new PyValue(getBigDecimal().divide(other.getBigDecimal(), precision, BigDecimal.ROUND_HALF_UP));
        }
        return new PyValue();
    }

    PyValue floordivide(PyValue other) {
        if (isInteger() && other.isInteger()) {
            return new PyValue(((BigInteger) value).divide((BigInteger) other.value));
        } else if (isNumber() && other.isNumber()) {
            return new PyValue(getBigDecimal().divideAndRemainder(other.getBigDecimal())[0]);
        }
        return new PyValue();
    }

    PyValue mod(PyValue other) {
        if (isInteger() && other.isInteger()) {
            if (other.compareTo(PyValue.ZERO) < 0) {
                return negate().mod(other.negate()).negate();
            }
            return new PyValue(((BigInteger) value).mod((BigInteger) other.value));
        } else if (isNumber() && other.isNumber()) {
            BigDecimal divisor = getBigDecimal();
            BigDecimal divider = other.getBigDecimal();
            if (divisor.compareTo(BigDecimal.ZERO) * divider.compareTo(BigDecimal.ZERO) < 0) {
                return new PyValue(divisor.divideAndRemainder(divider)[1].add(divider));
            }
            return new PyValue(divisor.divideAndRemainder(divider)[1]);
        }
        return new PyValue();
    }

    PyValue pow(PyValue other) {
        if (isInteger() && other.isInteger()) {
            return new PyValue(((BigInteger) value).pow(((BigInteger) other.value).intValueExact()));
        } else if (isDouble() && other.isInteger()) {
            return new PyValue(getBigDecimal().pow(((BigInteger) other.value).intValueExact()));
        }
        return new PyValue();
    }

    PyValue shiftLeft(PyValue other) {
        if (isInteger() && other.isInteger()) {
            return new PyValue(((BigInteger) value).shiftLeft(((BigInteger) other.value).intValueExact()));
        }
        return new PyValue();
    }

    PyValue shiftRight(PyValue other) {
        if (isInteger() && other.isInteger()) {
            return new PyValue(((BigInteger) value).shiftRight(((BigInteger) other.value).intValueExact()));
        }
        return new PyValue();
    }

    PyValue xor(PyValue other) {
        if (isInteger() && other.isInteger()) {
            return new PyValue(((BigInteger) value).xor((BigInteger) other.value));
        }
        return new PyValue();
    }

    PyValue and(PyValue other) {
        if (isInteger() && other.isInteger()) {
            return new PyValue(((BigInteger) value).and((BigInteger) other.value));
        }
        return new PyValue();
    }

    PyValue or(PyValue other) {
        if (isInteger() && other.isInteger()) {
            return new PyValue(((BigInteger) value).or((BigInteger) other.value));
        }
        return new PyValue();
    }
}
