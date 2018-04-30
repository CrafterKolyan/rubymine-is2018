package com.jetbrains.python.inspection;

import com.jetbrains.python.PyTokenTypes;
import com.jetbrains.python.psi.*;

import java.math.BigInteger;

public class PyConditionValue {
    enum Type
    {
        UNDEFINED,
        BOOLEAN,
        BIG_INTEGER
    }

    private Type type;
    private Object value;

    private void setThis(PyConditionValue pyCondValue) {
        type = pyCondValue.type;
        value = pyCondValue.value;
    }

    private void process(PyExpression pyExpr) {
        if (pyExpr instanceof PyBoolLiteralExpression) {
            value = ((PyBoolLiteralExpression) pyExpr).getValue();
            type = Type.BOOLEAN;
        } else if (pyExpr instanceof PyNumericLiteralExpression) {
            value = ((PyNumericLiteralExpression) pyExpr).getBigIntegerValue();
            type = Type.BIG_INTEGER;
        } else if (pyExpr instanceof PyPrefixExpression) {
            PyConditionValue operand = new PyConditionValue(((PyPrefixExpression) pyExpr).getOperand());
            PyElementType operator = ((PyPrefixExpression) pyExpr).getOperator();

            if (operator.equals(PyTokenTypes.PLUS)) {
                type = Type.BIG_INTEGER;
                value = operand.toBigInteger();
            } else if (operator.equals(PyTokenTypes.MINUS)) {
                type = Type.BIG_INTEGER;
                value = operand.toBigInteger().negate();
            } else if (operator.equals(PyTokenTypes.NOT_KEYWORD)) {
                type = Type.BOOLEAN;
                value = !operand.toBoolean();
            } else if (operator.equals(PyTokenTypes.TILDE)) {
                type = Type.BIG_INTEGER;
                value = operand.toBigInteger().negate().subtract(BigInteger.ONE);
            } else {
                type = Type.UNDEFINED;
                value = null;
            }
        } else if (pyExpr instanceof PyBinaryExpression) {
            PyConditionValue left = new PyConditionValue(((PyBinaryExpression) pyExpr).getLeftExpression());
            PyConditionValue right = new PyConditionValue(((PyBinaryExpression) pyExpr).getRightExpression());
            PyElementType op = ((PyBinaryExpression) pyExpr).getOperator();

            if (!left.isDetermined() || !right.isDetermined()) {
                type = Type.UNDEFINED;
                value = null;
                return;
            }

            if (op.equals(PyTokenTypes.LT)) {
                type = Type.BOOLEAN;
                value = left.toBigInteger().compareTo(right.toBigInteger()) < 0;
            } else if (op.equals(PyTokenTypes.LE)) {
                type = Type.BOOLEAN;
                value = left.toBigInteger().compareTo(right.toBigInteger()) <= 0;
            } else if (op.equals(PyTokenTypes.GT)) {
                type = Type.BOOLEAN;
                value = left.toBigInteger().compareTo(right.toBigInteger()) > 0;
            } else if (op.equals(PyTokenTypes.GE)) {
                type = Type.BOOLEAN;
                value = left.toBigInteger().compareTo(right.toBigInteger()) >= 0;
            } else if (op.equals(PyTokenTypes.EQEQ)) {
                type = Type.BOOLEAN;
                value = left.toBigInteger().compareTo(right.toBigInteger()) == 0;
            } else if (op.equals(PyTokenTypes.NE) || op.equals(PyTokenTypes.NE_OLD)) {
                type = Type.BOOLEAN;
                value = left.toBigInteger().compareTo(right.toBigInteger()) != 0;
            } else if (op.equals(PyTokenTypes.PLUS)) {
                type = Type.BIG_INTEGER;
                value = left.toBigInteger().add(right.toBigInteger());
            } else if (op.equals(PyTokenTypes.MINUS)) {
                type = Type.BIG_INTEGER;
                value = left.toBigInteger().subtract(right.toBigInteger());
            } else if (op.equals(PyTokenTypes.MULT)) {
                type = Type.BIG_INTEGER;
                value = left.toBigInteger().multiply(right.toBigInteger());
            } else if (op.equals(PyTokenTypes.DIV)) {
                // TODO: FIX. This should be float division
                type = Type.UNDEFINED;
                value = null;
            } else if (op.equals(PyTokenTypes.EXP)) {
                type = Type.BIG_INTEGER;
                value = left.toBigInteger().pow(right.toBigInteger().intValueExact());
            } else if (op.equals(PyTokenTypes.FLOORDIV)) {
                type = Type.BIG_INTEGER;
                value = left.toBigInteger().divide(right.toBigInteger());
            } else if (op.equals(PyTokenTypes.PERC)) {
                type = Type.BIG_INTEGER;
                value = left.toBigInteger().mod(right.toBigInteger());
            } else if (op.equals(PyTokenTypes.LTLT)) {
                type = Type.BIG_INTEGER;
                value = left.toBigInteger().shiftLeft(right.toBigInteger().intValueExact());
            } else if (op.equals(PyTokenTypes.GTGT)) {
                type = Type.BIG_INTEGER;
                value = left.toBigInteger().shiftRight(right.toBigInteger().intValueExact());
            } else if (op.equals(PyTokenTypes.AND_KEYWORD)) {
                setThis(left.toBoolean() ? right : left);
            } else if (op.equals(PyTokenTypes.OR_KEYWORD)) {
                setThis(left.toBoolean() ? left : right);
            } else if (op.equals(PyTokenTypes.XOR)) {
                type = Type.BIG_INTEGER;
                value = left.toBigInteger().xor(right.toBigInteger());
            } else if (op.equals(PyTokenTypes.AND)) {
                type = Type.BIG_INTEGER;
                value = left.toBigInteger().and(right.toBigInteger());
            } else if (op.equals(PyTokenTypes.OR)) {
                type = Type.BIG_INTEGER;
                value = left.toBigInteger().or(right.toBigInteger());
            } else {
                type = Type.UNDEFINED;
                value = null;
            }
        } else if (pyExpr instanceof PyParenthesizedExpression) {
            PyParenthesizedExpression pyParExpr = (PyParenthesizedExpression) pyExpr;
            process(pyParExpr.getContainedExpression());
        } else {
            value = null;
            type = Type.UNDEFINED;
        }
    }

    public PyConditionValue(PyExpression pyExpr) {
        process(pyExpr);
    }

    public boolean isDetermined() {
        return type != Type.UNDEFINED;
    }

    public Boolean toBoolean() {
        switch (type) {
            case BOOLEAN:
                return (boolean)value;
            case BIG_INTEGER:
                return value != BigInteger.ZERO;
        }
        return null;
    }

    public BigInteger toBigInteger() {
        switch (type) {
            case BOOLEAN:
                return ((boolean) value) ? BigInteger.ONE : BigInteger.ZERO;
            case BIG_INTEGER:
                return (BigInteger) value;
        }
        return null;
    }
}
