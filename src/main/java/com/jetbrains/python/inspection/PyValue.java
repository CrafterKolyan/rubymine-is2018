package com.jetbrains.python.inspection;

import com.jetbrains.python.psi.PyBinaryExpression;

import java.math.BigDecimal;
import java.math.BigInteger;

public class PyValue {
    private enum Type {
        UNDEFINED,
        BIG_INTEGER,
        BIG_DECIMAL,
        OBJECT;
    }

    public static final PyValue ZERO = new PyValue(BigInteger.ZERO);
    public static final PyValue ONE = new PyValue(BigInteger.ONE);

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

    public PyValue() { setThis(); }
    public PyValue(BigInteger bigInt) { setThis(bigInt); }
    public PyValue(BigDecimal bigDec) { setThis(bigDec); }
    /*public PyValue(Object obj) {
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

    public boolean isDetermined() { return type != Type.UNDEFINED; }
    public boolean isNumber() { return type == Type.BIG_INTEGER || type == Type.BIG_DECIMAL; }
    public boolean isInteger() { return type == Type.BIG_INTEGER; }
    public boolean isDouble() { return type == Type.BIG_DECIMAL; }
    public boolean isObject() { return type == Type.OBJECT; }

    public Object getValue() { return value; }

    public String getTypeString() {
        switch (type) {
            case BIG_INTEGER: return "integer";
            case BIG_DECIMAL: return "float";
            case UNDEFINED: return "undefined";
            case OBJECT: return "object";
        }
        return type.toString();
    }

    public BigDecimal getBigDecimal() {
        if (!isNumber()) {
            return BigDecimal.ZERO;
        } else if (type == Type.BIG_INTEGER) {
            return new BigDecimal((BigInteger) value);
        }
        return (BigDecimal) value;
    }

    @Override
    public boolean equals(Object another) {
        if (!(another instanceof PyValue))
            return false;
        PyValue other = (PyValue)another;
        if (!isNumber() || !other.isNumber()) {
            return value.equals(other.value);
        } else if (type == Type.BIG_INTEGER && other.type == Type.BIG_INTEGER) {
            return ((BigInteger)value).equals((BigInteger)other.value);
        }
        return getBigDecimal().equals(other.getBigDecimal());
    }

    public int compareTo(PyValue other) {
        if (type == Type.UNDEFINED && other.type == Type.UNDEFINED) {
            return 0;
        } else if (type == Type.UNDEFINED || other.type == Type.UNDEFINED) {
            return (type == Type.UNDEFINED) ? -1 : 1;
        } else if (!isNumber() || !other.isNumber()) {
            return value.equals(other.value) ? 0 : -1;
        }
        if (type == Type.BIG_INTEGER && other.type == Type.BIG_INTEGER) {
            return ((BigInteger) value).compareTo((BigInteger) other.value);
        }
        return getBigDecimal().compareTo(other.getBigDecimal());
    }

    public PyValue negate() {
        switch (type) {
            case BIG_INTEGER:
                return new PyValue(((BigInteger) value).negate());
            case BIG_DECIMAL:
                return new PyValue(((BigDecimal) value).negate());
            default:
                return new PyValue();
        }
    }

    public PyValue add(PyValue other) {
        if (type == Type.BIG_INTEGER && other.type == Type.BIG_INTEGER) {
            return new PyValue(((BigInteger) value).add((BigInteger) other.value));
        } else if (isNumber() && other.isNumber()) {
            return new PyValue(getBigDecimal().add(other.getBigDecimal()));
        }
        return new PyValue();
    }

    public PyValue subtract(PyValue other) {
        if (type == Type.BIG_INTEGER && other.type == Type.BIG_INTEGER) {
            return new PyValue(((BigInteger) value).subtract((BigInteger) other.value));
        } else if (isNumber() && other.isNumber()) {
            return new PyValue(getBigDecimal().subtract(other.getBigDecimal()));
        }
        return new PyValue();
    }

    public PyValue multiply(PyValue other) {
        if (type == Type.BIG_INTEGER && other.type == Type.BIG_INTEGER) {
            return new PyValue(((BigInteger) value).multiply((BigInteger) other.value));
        } else if (isNumber() && other.isNumber()) {
            return new PyValue(getBigDecimal().multiply(other.getBigDecimal()));
        }
        return new PyValue();
    }

    public PyValue divide(PyValue other) {
        if (isNumber() && other.isNumber()) {
            return new PyValue(getBigDecimal().divide(other.getBigDecimal(), precision, BigDecimal.ROUND_HALF_UP));
        }
        return new PyValue();
    }

    public PyValue floordivide(PyValue other) {
        if (type == Type.BIG_INTEGER && other.type == Type.BIG_INTEGER) {
            return new PyValue(((BigInteger) value).divide((BigInteger) other.value));
        } else if (isNumber() && other.isNumber()) {
            return new PyValue(getBigDecimal().divideAndRemainder(other.getBigDecimal())[0]);
        }
        return new PyValue();
    }

    public PyValue mod(PyValue other) {
        if (type == Type.BIG_INTEGER && other.type == Type.BIG_INTEGER) {
            return new PyValue(((BigInteger) value).mod((BigInteger) other.value));
        } else if (isNumber() && other.isNumber()) {
            return new PyValue(getBigDecimal().divideAndRemainder(other.getBigDecimal())[1]);
        }
        return new PyValue();
    }

    public PyValue shiftLeft(PyValue other) {
        if (type == Type.BIG_INTEGER && other.type == Type.BIG_INTEGER) {
            return new PyValue(((BigInteger) value).shiftLeft(((BigInteger) other.value).intValueExact()));
        }
        return new PyValue();
    }

    public PyValue shiftRight(PyValue other) {
        if (type == Type.BIG_INTEGER && other.type == Type.BIG_INTEGER) {
            return new PyValue(((BigInteger) value).shiftRight(((BigInteger) other.value).intValueExact()));
        }
        return new PyValue();
    }

    public PyValue xor(PyValue other) {
        if (type == Type.BIG_INTEGER && other.type == Type.BIG_INTEGER) {
            return new PyValue(((BigInteger) value).xor((BigInteger) other.value));
        }
        return new PyValue();
    }

    public PyValue and(PyValue other) {
        if (type == Type.BIG_INTEGER && other.type == Type.BIG_INTEGER) {
            return new PyValue(((BigInteger) value).and((BigInteger) other.value));
        }
        return new PyValue();
    }

    public PyValue or(PyValue other) {
        if (type == Type.BIG_INTEGER && other.type == Type.BIG_INTEGER) {
            return new PyValue(((BigInteger) value).or((BigInteger) other.value));
        }
        return new PyValue();
    }
}
