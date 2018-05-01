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

import java.math.BigDecimal;
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
            enum Type {
                UNDEFINED,
                BOOLEAN,
                VALUE,
                BOOLEAN_AND_VALUE
            }

            private Type type;
            private boolean result;
            private PyValue value;

            private PyConditionValue(Type t, boolean res, PyValue val) {
                type = t;
                result = res;
                value = val;
            }

            private PyConditionValue() {
                type = Type.UNDEFINED;
                result = false;
                value = new PyValue();
            }

            private PyConditionValue(boolean res) {
                type = Type.BOOLEAN;
                result = res;
                value = result ? PyValue.ONE : PyValue.ZERO;
            }

            private PyConditionValue(BigInteger res) {
                type = Type.VALUE;
                value = new PyValue(res);
                result = value.compareTo(PyValue.ZERO) != 0;
            }

            private PyConditionValue(BigDecimal res) {
                type = Type.VALUE;
                value = new PyValue(res);
                result = value.compareTo(PyValue.ZERO) != 0;
            }

            private PyConditionValue(PyValue res) {
                if (!res.isDetermined()) {
                    type = Type.UNDEFINED;
                    result = false;
                    value = res;
                } else if (res.isNumber()) {
                    type = Type.VALUE;
                    value = res;
                    result = !value.equals(PyValue.ZERO);
                } else {
                    type = Type.VALUE;
                    value = res;
                    result = !value.equals(null);
                }
            }

            private boolean isDetermined() { return type != Type.UNDEFINED; }

            /**
             * Undefined behaviour when type == Type.UNDEFINED
             * @return boolean result of condition expression
             */
            private boolean getBoolean() { return result; }

            /**
             * Undefined behaviour when type == Type.UNDEFINED
             * @return BigInteger result of condition expression
             */
            private PyValue getValue() { return value; }
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
            if (pyExpr.isIntegerLiteral()) {
               return new PyConditionValue(pyExpr.getBigIntegerValue());
            } else {
                return new PyConditionValue(pyExpr.getBigDecimalValue());
            }
        }

        private PyConditionValue processPrefExpr(PyPrefixExpression pyExpr) {
            PyConditionValue operand = process(pyExpr.getOperand());
            PyElementType operator = pyExpr.getOperator();

            if (!operand.isDetermined()) {
                return new PyConditionValue();
            }

            if (operator.equals(PyTokenTypes.PLUS)) {
                return new PyConditionValue(operand.getValue());
            } else if (operator.equals(PyTokenTypes.MINUS)) {
                return new PyConditionValue(operand.getValue().negate());
            } else if (operator.equals(PyTokenTypes.NOT_KEYWORD)) {
                return new PyConditionValue(!operand.getBoolean());
            } else if (operator.equals(PyTokenTypes.TILDE)) {
                return new PyConditionValue(operand.getValue().negate().subtract(PyValue.ONE));
            } else {
                return new PyConditionValue();
            }
        }

        private PyConditionValue processBinExpr(PyBinaryExpression pyExpr) {
            PyConditionValue left = process(pyExpr.getLeftExpression());
            PyConditionValue right = process(pyExpr.getRightExpression());
            PyElementType op = pyExpr.getOperator();

            if (left.isDetermined()) {
                if (op.equals(PyTokenTypes.AND_KEYWORD)) {
                    return left.getBoolean() ? right : left;
                } else if (op.equals(PyTokenTypes.OR_KEYWORD)) {
                    return left.getBoolean() ? left : right;
                } else if (left.type == PyConditionValue.Type.BOOLEAN_AND_VALUE && !left.getBoolean()
                        && (op.equals(PyTokenTypes.LT) || op.equals(PyTokenTypes.LE)
                        || op.equals(PyTokenTypes.GT) || op.equals(PyTokenTypes.GE)
                        || op.equals(PyTokenTypes.EQEQ) || op.equals(PyTokenTypes.NE) || op.equals(PyTokenTypes.NE_OLD))) {
                    return new PyConditionValue(false);
                }
            }

            if (!left.isDetermined() || !right.isDetermined()) {
                return new PyConditionValue();
            }

            if (op.equals(PyTokenTypes.LT)) {
                return new PyConditionValue(PyConditionValue.Type.BOOLEAN_AND_VALUE,
                        left.getValue().compareTo(right.getValue()) < 0,
                        right.getValue());
            } else if (op.equals(PyTokenTypes.LE)) {
                return new PyConditionValue(PyConditionValue.Type.BOOLEAN_AND_VALUE,
                        left.getValue().compareTo(right.getValue()) <= 0,
                        right.getValue());
            } else if (op.equals(PyTokenTypes.GT)) {
                return new PyConditionValue(PyConditionValue.Type.BOOLEAN_AND_VALUE,
                        left.getValue().compareTo(right.getValue()) > 0,
                        right.getValue());
            } else if (op.equals(PyTokenTypes.GE)) {
                return new PyConditionValue(PyConditionValue.Type.BOOLEAN_AND_VALUE,
                        left.getValue().compareTo(right.getValue()) >= 0,
                        right.getValue());
            } else if (op.equals(PyTokenTypes.EQEQ)) {
                return new PyConditionValue(PyConditionValue.Type.BOOLEAN_AND_VALUE,
                        left.getValue().compareTo(right.getValue()) == 0,
                        right.getValue());
            } else if (op.equals(PyTokenTypes.NE) || op.equals(PyTokenTypes.NE_OLD)) {
                return new PyConditionValue(PyConditionValue.Type.BOOLEAN_AND_VALUE,
                        left.getValue().compareTo(right.getValue()) != 0,
                        right.getValue());
            } else if (op.equals(PyTokenTypes.PLUS)) {
                return new PyConditionValue(left.getValue().add(right.getValue()));
            } else if (op.equals(PyTokenTypes.MINUS)) {
                return new PyConditionValue(left.getValue().subtract(right.getValue()));
            }/* else if (op.equals(PyTokenTypes.MULT)) {
                return new PyConditionValue(left.getValue().multiply(right.getValue()));
            } else if (op.equals(PyTokenTypes.DIV)) {
                // TODO: FIX. This should be float division
                return new PyConditionValue();
            } else if (op.equals(PyTokenTypes.EXP)) {
                BigInteger l = left.getValue();
                int r = right.getValue().intValueExact();
                if (l.equals(BigInteger.ZERO)) {
                    if (r == 0) {
                        return new PyConditionValue(BigInteger.ONE);
                    } else if (r < 0) {
                        registerProblem(pyExpr, "0 cannot be raised to a negative power (" + r + ")");
                        return new PyConditionValue();
                    }
                }
                return new PyConditionValue(left.getValue().pow(right.getValue().intValueExact()));
            } else if (op.equals(PyTokenTypes.FLOORDIV)) {
                BigInteger divider = right.getValue();
                if (divider.equals(BigInteger.ZERO)) {
                    registerProblem(pyExpr, "Division by 0");
                    return new PyConditionValue();
                }
                return new PyConditionValue(left.getValue().divide(divider));
            } else if (op.equals(PyTokenTypes.PERC)) {
                BigInteger divisor = left.getValue();
                BigInteger divider = right.getValue();
                if (divider.equals(BigInteger.ZERO)) {
                    registerProblem(pyExpr, "Taking modulo by 0");
                    return new PyConditionValue();
                } else if (divider.compareTo(BigInteger.ZERO) < 0) {
                    return new PyConditionValue(divisor.negate().mod(divider.negate()).negate());
                }
                return new PyConditionValue(divisor.mod(divider));
            } else if (op.equals(PyTokenTypes.LTLT)) {
                int shift = right.getValue().intValueExact();
                if (shift < 0) {
                    registerProblem(pyExpr, "Shifting by negative number (" + shift + ")");
                    return new PyConditionValue();
                }
                return new PyConditionValue(left.getValue().shiftLeft(shift));
            } else if (op.equals(PyTokenTypes.GTGT)) {
                int shift = right.getValue().intValueExact();
                if (shift < 0) {
                    registerProblem(pyExpr, "Shifting by negative number (" + shift + ")");
                    return new PyConditionValue();
                }
                return new PyConditionValue(left.getValue().shiftRight(shift));
            } else if (op.equals(PyTokenTypes.XOR)) {
                return new PyConditionValue(left.getValue().xor(right.getValue()));
            } else if (op.equals(PyTokenTypes.AND)) {
                return new PyConditionValue(left.getValue().and(right.getValue()));
            } else if (op.equals(PyTokenTypes.OR)) {
                return new PyConditionValue(left.getValue().or(right.getValue()));
            }
*/
            return new PyConditionValue();
        }

        private PyConditionValue processParExpr(PyParenthesizedExpression pyExpr) {
            return process(pyExpr.getContainedExpression());
        }
    }
}
