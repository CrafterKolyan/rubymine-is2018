package com.jetbrains.python.inspection;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.python.PyTokenTypes;
import com.jetbrains.python.inspections.PyInspection;
import com.jetbrains.python.inspections.PyInspectionVisitor;
import com.jetbrains.python.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public class PyConstantExpression extends PyInspection {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly,
                                          @NotNull LocalInspectionToolSession session) {
        return new Visitor(holder, session);
    }

    private static class Visitor extends PyInspectionVisitor {

        private Visitor(@Nullable ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
            super(holder, session);
        }

        @Override
        public void visitPyIfStatement(PyIfStatement node) {
            super.visitPyIfStatement(node);
            processIfPart(node.getIfPart());
            for (PyIfPart part : node.getElifParts()) {
                processIfPart(part);
            }
        }

        private void processIfPart(@NotNull PyIfPart pyIfPart) {
            final PyExpression condition = pyIfPart.getCondition();
            PyConditionValue conditionValue = process(condition);
            if (conditionValue.isDetermined()) {
                registerProblem(condition, "The condition is always " + conditionValue.getBoolean());
            }
        }

        private static class PyConditionValue {
            public enum Type {
                UNDEFINED,
                BOOLEAN,
                BIG_INTEGER,
                BOOLEAN_AND_BIG_INTEGER
            }

            private Type type;
            private boolean result;
            private BigInteger value;

            public PyConditionValue(Type t, boolean res, BigInteger val) {
                type = t;
                result = res;
                value = val;
            }

            public PyConditionValue() {
                type = Type.UNDEFINED;
                result = false;
                value = BigInteger.ZERO;
            }

            public PyConditionValue(PyConditionValue pyCondValue) {
                type = pyCondValue.type;
                result = pyCondValue.result;
                value = pyCondValue.value;
            }

            public PyConditionValue(boolean res) {
                type = Type.BOOLEAN;
                result = res;
                value = result ? BigInteger.ONE : BigInteger.ZERO;
            }

            public PyConditionValue(BigInteger res) {
                type = Type.BIG_INTEGER;
                value = res;
                result = !value.equals(BigInteger.ZERO);
            }

            public boolean isDetermined() { return type != Type.UNDEFINED; }

            public boolean getBoolean() { return result; }

            public BigInteger getBigInteger() { return value; }
        }

        private PyConditionValue process(PyExpression pyExpr) {
            if (pyExpr instanceof PyBoolLiteralExpression) {
                return processBoolLiteral((PyBoolLiteralExpression) pyExpr);
            } else if (pyExpr instanceof PyNumericLiteralExpression) {
                return processNumLiteral((PyNumericLiteralExpression) pyExpr);
            } else if (pyExpr instanceof PyPrefixExpression) {
                return processPrefExpr((PyPrefixExpression) pyExpr);
            } else if (pyExpr instanceof PyBinaryExpression) {
                return processBinExpr((PyBinaryExpression) pyExpr);
            } else if (pyExpr instanceof PyParenthesizedExpression) {
                return processParExpr((PyParenthesizedExpression) pyExpr);
            }
            return new PyConditionValue();
        }

        private PyConditionValue processBoolLiteral(PyBoolLiteralExpression pyExpr) {
            return new PyConditionValue(pyExpr.getValue());
        }

        private PyConditionValue processNumLiteral(PyNumericLiteralExpression pyExpr) {
            return new PyConditionValue(pyExpr.getBigIntegerValue());
        }

        private PyConditionValue processPrefExpr(PyPrefixExpression pyExpr) {
            PyConditionValue operand = process(pyExpr.getOperand());
            PyElementType operator = pyExpr.getOperator();

            if (operator.equals(PyTokenTypes.PLUS)) {
                return new PyConditionValue(operand.getBigInteger());
            } else if (operator.equals(PyTokenTypes.MINUS)) {
                return new PyConditionValue(operand.getBigInteger().negate());
            } else if (operator.equals(PyTokenTypes.NOT_KEYWORD)) {
                return new PyConditionValue(!operand.getBoolean());
            } else if (operator.equals(PyTokenTypes.TILDE)) {
                return new PyConditionValue(operand.getBigInteger().negate().subtract(BigInteger.ONE));
            } else {
                return new PyConditionValue();
            }
        }

        private PyConditionValue processBinExpr(PyBinaryExpression pyExpr) {
            PyConditionValue left = process(pyExpr.getLeftExpression());
            PyConditionValue right = process(pyExpr.getRightExpression());
            PyElementType op = pyExpr.getOperator();

            if (op.equals(PyTokenTypes.AND_KEYWORD)) {
                return new PyConditionValue(left.getBoolean() ? right : left);
            } else if (op.equals(PyTokenTypes.OR_KEYWORD)) {
                return new PyConditionValue(left.getBoolean() ? left : right);
            }

            if (!left.isDetermined() || !right.isDetermined()) {
                return new PyConditionValue();
            }

            if (op.equals(PyTokenTypes.LT)) {
                return new PyConditionValue(PyConditionValue.Type.BOOLEAN_AND_BIG_INTEGER,
                        left.getBigInteger().compareTo(right.getBigInteger()) < 0,
                        right.getBigInteger());
            } else if (op.equals(PyTokenTypes.LE)) {
                return new PyConditionValue(PyConditionValue.Type.BOOLEAN_AND_BIG_INTEGER,
                        left.getBigInteger().compareTo(right.getBigInteger()) <= 0,
                        right.getBigInteger());
            } else if (op.equals(PyTokenTypes.GT)) {
                return new PyConditionValue(PyConditionValue.Type.BOOLEAN_AND_BIG_INTEGER,
                        left.getBigInteger().compareTo(right.getBigInteger()) > 0,
                        right.getBigInteger());
            } else if (op.equals(PyTokenTypes.GE)) {
                return new PyConditionValue(PyConditionValue.Type.BOOLEAN_AND_BIG_INTEGER,
                        left.getBigInteger().compareTo(right.getBigInteger()) >= 0,
                        right.getBigInteger());
            } else if (op.equals(PyTokenTypes.EQEQ)) {
                return new PyConditionValue(PyConditionValue.Type.BOOLEAN_AND_BIG_INTEGER,
                        left.getBigInteger().compareTo(right.getBigInteger()) == 0,
                        right.getBigInteger());
            } else if (op.equals(PyTokenTypes.NE) || op.equals(PyTokenTypes.NE_OLD)) {
                return new PyConditionValue(PyConditionValue.Type.BOOLEAN_AND_BIG_INTEGER,
                        left.getBigInteger().compareTo(right.getBigInteger()) != 0,
                        right.getBigInteger());
            } else if (op.equals(PyTokenTypes.PLUS)) {
                return new PyConditionValue(left.getBigInteger().add(right.getBigInteger()));
            } else if (op.equals(PyTokenTypes.MINUS)) {
                return new PyConditionValue(left.getBigInteger().subtract(right.getBigInteger()));
            } else if (op.equals(PyTokenTypes.MULT)) {
                return new PyConditionValue(left.getBigInteger().multiply(right.getBigInteger()));
            } else if (op.equals(PyTokenTypes.DIV)) {
                // TODO: FIX. This should be float division
                return new PyConditionValue();
            } else if (op.equals(PyTokenTypes.EXP)) {
                BigInteger l = left.getBigInteger();
                int r = right.getBigInteger().intValueExact();
                if (l.equals(BigInteger.ZERO)) {
                    if (r == 0) {
                        return new PyConditionValue(BigInteger.ONE);
                    } else if (r < 0) {
                        registerProblem(pyExpr, "0 cannot be raised to a negative power (" + r + ")");
                        return new PyConditionValue();
                    }
                }
                return new PyConditionValue(left.getBigInteger().pow(right.getBigInteger().intValueExact()));
            } else if (op.equals(PyTokenTypes.FLOORDIV)) {
                BigInteger divider = right.getBigInteger();
                if (divider.equals(BigInteger.ZERO)) {
                    registerProblem(pyExpr, "Division by 0");
                    return new PyConditionValue();
                }
                return new PyConditionValue(left.getBigInteger().divide(divider));
            } else if (op.equals(PyTokenTypes.PERC)) {
                BigInteger divisor = left.getBigInteger();
                BigInteger divider = right.getBigInteger();
                if (divider.equals(BigInteger.ZERO)) {
                    registerProblem(pyExpr, "Taking modulo by 0");
                    return new PyConditionValue();
                } else if (divider.compareTo(BigInteger.ZERO) < 0) {
                    return new PyConditionValue(divisor.negate().mod(divider.negate()).negate());
                }
                return new PyConditionValue(divisor.mod(divider));
            } else if (op.equals(PyTokenTypes.LTLT)) {
                int shift = right.getBigInteger().intValueExact();
                if (shift < 0) {
                    registerProblem(pyExpr, "Shifting by negative number (" + shift + ")");
                    return new PyConditionValue();
                }
                return new PyConditionValue(left.getBigInteger().shiftLeft(shift));
            } else if (op.equals(PyTokenTypes.GTGT)) {
                int shift = right.getBigInteger().intValueExact();
                if (shift < 0) {
                    registerProblem(pyExpr, "Shifting by negative number (" + shift + ")");
                    return new PyConditionValue();
                }
                return new PyConditionValue(left.getBigInteger().shiftRight(shift));
            } else if (op.equals(PyTokenTypes.XOR)) {
                return new PyConditionValue(left.getBigInteger().xor(right.getBigInteger()));
            } else if (op.equals(PyTokenTypes.AND)) {
                return new PyConditionValue(left.getBigInteger().and(right.getBigInteger()));
            } else if (op.equals(PyTokenTypes.OR)) {
                return new PyConditionValue(left.getBigInteger().or(right.getBigInteger()));
            }

            return new PyConditionValue();
        }

        private PyConditionValue processParExpr(PyParenthesizedExpression pyExpr) {
            return process(pyExpr.getContainedExpression());
        }
    }
}
