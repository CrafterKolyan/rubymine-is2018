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
    };

    Type type;
    Object value;

    public PyConditionValue(PyExpression pyExpr) {
        if (pyExpr instanceof PyBoolLiteralExpression) {
            value = ((PyBoolLiteralExpression) pyExpr).getValue();
            type = Type.BOOLEAN;
        } else if (pyExpr instanceof PyNumericLiteralExpression) {
            value = ((PyNumericLiteralExpression) pyExpr).getBigIntegerValue();
            type = Type.BIG_INTEGER;
        } else if (pyExpr instanceof PyPrefixExpression) {
            PyConditionValue operand = new PyConditionValue(((PyPrefixExpression) pyExpr).getOperand());
            PyElementType operator = ((PyPrefixExpression) pyExpr).getOperator();

            type = Type.BIG_INTEGER;
            if (operator.equals(PyTokenTypes.PLUS)) {
                value = operand.toBigInteger();
            } else if (operator.equals(PyTokenTypes.MINUS)) {
                value = operand.toBigInteger().negate();
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
            } else {
                type = Type.UNDEFINED;
                value = null;
            }
        } else {
            value = null;
            type = Type.UNDEFINED;
        }
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
