package com.jetbrains.python.inspection;

import com.jetbrains.python.PyTokenTypes;
import com.jetbrains.python.psi.*;

import java.math.BigInteger;

public class PyConditionValue {
    enum Type
    {
        UNDEFINED,
        BOOLEAN,
        BIG_INTEGER,
        BOOLEAN_AND_BIG_INTEGER
    }

    private Type type;
    private boolean result;
    private BigInteger value;

    private void setThis(PyConditionValue pyCondValue) {
        type = pyCondValue.type;
        result = pyCondValue.result;
        value = pyCondValue.value;
    }

    private void setThis(boolean res) {
        type = Type.BOOLEAN;
        result = res;
        value = result ? BigInteger.ONE : BigInteger.ZERO;
    }

    private void setThis(BigInteger res) {
        type = Type.BIG_INTEGER;
        value = res;
        result = value != BigInteger.ZERO;
    }

    private void setThisUndefined() {
        type = Type.UNDEFINED;
        result = false;
        value = BigInteger.ZERO;
    }

    private void process(PyBoolLiteralExpression pyExpr) {
        setThis(pyExpr.getValue());
    }

    private void process(PyNumericLiteralExpression pyExpr) {
        setThis(pyExpr.getBigIntegerValue());
    }

    private void process(PyPrefixExpression pyExpr) {
        PyConditionValue operand = new PyConditionValue(pyExpr.getOperand());
        PyElementType operator = pyExpr.getOperator();

        if (operator.equals(PyTokenTypes.PLUS)) {
            setThis(operand.getBigInteger());
        } else if (operator.equals(PyTokenTypes.MINUS)) {
            setThis(operand.getBigInteger().negate());
        } else if (operator.equals(PyTokenTypes.NOT_KEYWORD)) {
            setThis(!operand.getBoolean());
        } else if (operator.equals(PyTokenTypes.TILDE)) {
            setThis(operand.getBigInteger().negate().subtract(BigInteger.ONE));
        } else {
            setThisUndefined();
        }
    }

    private void process(PyBinaryExpression pyExpr) {
        PyConditionValue left = new PyConditionValue(pyExpr.getLeftExpression());
        PyConditionValue right = new PyConditionValue(pyExpr.getRightExpression());
        PyElementType op = pyExpr.getOperator();

        if (!left.isDetermined() || !right.isDetermined()) {
            setThisUndefined();
            return;
        }

        if (op.equals(PyTokenTypes.LT)) {
            setThis(left.getBigInteger().compareTo(right.getBigInteger()) < 0);
        } else if (op.equals(PyTokenTypes.LE)) {
            setThis(left.getBigInteger().compareTo(right.getBigInteger()) <= 0);
        } else if (op.equals(PyTokenTypes.GT)) {
            setThis(left.getBigInteger().compareTo(right.getBigInteger()) > 0);
        } else if (op.equals(PyTokenTypes.GE)) {
            setThis(left.getBigInteger().compareTo(right.getBigInteger()) >= 0);
        } else if (op.equals(PyTokenTypes.EQEQ)) {
            setThis(left.getBigInteger().compareTo(right.getBigInteger()) == 0);
        } else if (op.equals(PyTokenTypes.NE) || op.equals(PyTokenTypes.NE_OLD)) {
            setThis(left.getBigInteger().compareTo(right.getBigInteger()) != 0);
        } else if (op.equals(PyTokenTypes.PLUS)) {
            setThis(left.getBigInteger().add(right.getBigInteger()));
        } else if (op.equals(PyTokenTypes.MINUS)) {
            setThis(left.getBigInteger().subtract(right.getBigInteger()));
        } else if (op.equals(PyTokenTypes.MULT)) {
            setThis(left.getBigInteger().multiply(right.getBigInteger()));
        } else if (op.equals(PyTokenTypes.DIV)) {
            // TODO: FIX. This should be float division
            setThisUndefined();
            return;
        } else if (op.equals(PyTokenTypes.EXP)) {
            setThis(left.getBigInteger().pow(right.getBigInteger().intValueExact()));
        } else if (op.equals(PyTokenTypes.FLOORDIV)) {
            setThis(left.getBigInteger().divide(right.getBigInteger()));
        } else if (op.equals(PyTokenTypes.PERC)) {
            setThis(left.getBigInteger().mod(right.getBigInteger()));
        } else if (op.equals(PyTokenTypes.LTLT)) {
            setThis(left.getBigInteger().shiftLeft(right.getBigInteger().intValueExact()));
        } else if (op.equals(PyTokenTypes.GTGT)) {
            setThis(left.getBigInteger().shiftRight(right.getBigInteger().intValueExact()));
        } else if (op.equals(PyTokenTypes.XOR)) {
            setThis(left.getBigInteger().xor(right.getBigInteger()));
        } else if (op.equals(PyTokenTypes.AND)) {
            setThis(left.getBigInteger().and(right.getBigInteger()));
        } else if (op.equals(PyTokenTypes.OR)) {
            setThis(left.getBigInteger().or(right.getBigInteger()));
        } else if (op.equals(PyTokenTypes.AND_KEYWORD)) {
            setThis(left.getBoolean() ? right : left);
        } else if (op.equals(PyTokenTypes.OR_KEYWORD)) {
            setThis(left.getBoolean() ? left : right);
        } else {
            setThisUndefined();
        }

        if (op.equals(PyTokenTypes.LT) || op.equals(PyTokenTypes.LE)
                || op.equals(PyTokenTypes.GT) || op.equals(PyTokenTypes.GE)
                || op.equals(PyTokenTypes.EQEQ) || op.equals(PyTokenTypes.NE) || op.equals(PyTokenTypes.NE_OLD)) {
            type = Type.BOOLEAN_AND_BIG_INTEGER;
            value = right.getBigInteger();
        }
    }

    private void process(PyParenthesizedExpression pyExpr) {
        process(pyExpr.getContainedExpression());
    }

    private void process(PyExpression pyExpr) {
        if (pyExpr instanceof PyBoolLiteralExpression) {
            process((PyBoolLiteralExpression) pyExpr);
        } else if (pyExpr instanceof PyNumericLiteralExpression) {
            process((PyNumericLiteralExpression) pyExpr);
        } else if (pyExpr instanceof PyPrefixExpression) {
            process((PyPrefixExpression) pyExpr);
        } else if (pyExpr instanceof PyBinaryExpression) {
            process((PyBinaryExpression) pyExpr);
        } else if (pyExpr instanceof PyParenthesizedExpression) {
            process((PyParenthesizedExpression) pyExpr);
        } else {
            setThisUndefined();
        }
    }

    public PyConditionValue(PyExpression pyExpr) {
        process(pyExpr);
    }

    public boolean isDetermined() {
        return type != Type.UNDEFINED;
    }

    /**
     * Undefined behaviour in case condition is not determined
     * @return boolean result for condition
     */
    public boolean getBoolean() {
        return result;
    }

    /**
     * Undefined behaviour in case condition is not determined
     * @return BigInteger result for condition
     */
    public BigInteger getBigInteger() {
        return value;
    }
}
