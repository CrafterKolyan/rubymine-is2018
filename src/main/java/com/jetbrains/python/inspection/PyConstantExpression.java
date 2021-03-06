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

            private static final PyConditionValue UNDEFINED = new PyConditionValue();

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
                    result = res.getValue() != null;
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

            private PyConditionValue normalize() {
                if (type == Type.BOOLEAN_AND_VALUE) {
                    type = Type.BOOLEAN;
                    value = result ? PyValue.ONE : PyValue.ZERO;
                }
                return this;
            }
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
            return PyConditionValue.UNDEFINED;
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
                return PyConditionValue.UNDEFINED;
            }

            if (operator.equals(PyTokenTypes.PLUS)) {
                return new PyConditionValue(operand.getValue());
            } else if (operator.equals(PyTokenTypes.MINUS)) {
                return new PyConditionValue(operand.getValue().negate());
            } else if (operator.equals(PyTokenTypes.NOT_KEYWORD)) {
                return new PyConditionValue(!operand.getBoolean());
            } else if (operator.equals(PyTokenTypes.TILDE)) {
                if (!operand.getValue().isInteger()) {
                    registerProblem(pyExpr, "Unsupported operand type (" + operand.getValue().getTypeString() + ")");
                    return PyConditionValue.UNDEFINED;
                }
                return new PyConditionValue(operand.getValue().negate().subtract(PyValue.ONE));
            } else {
                return PyConditionValue.UNDEFINED;
            }
        }

        private PyConditionValue processBinExpr(PyBinaryExpression pyExpr) {
            PyConditionValue left = process(pyExpr.getLeftExpression());
            PyConditionValue right = process(pyExpr.getRightExpression());
            PyElementType op = pyExpr.getOperator();

            if (left.isDetermined()) {
                if (op.equals(PyTokenTypes.AND_KEYWORD)) {
                    return left.getBoolean() ? right.normalize() : left.normalize();
                } else if (op.equals(PyTokenTypes.OR_KEYWORD)) {
                    return left.getBoolean() ? left.normalize() : right.normalize();
                } else if (left.type == PyConditionValue.Type.BOOLEAN_AND_VALUE && !left.getBoolean()
                        && (op.equals(PyTokenTypes.LT) || op.equals(PyTokenTypes.LE)
                        || op.equals(PyTokenTypes.GT) || op.equals(PyTokenTypes.GE)
                        || op.equals(PyTokenTypes.EQEQ) || op.equals(PyTokenTypes.NE) || op.equals(PyTokenTypes.NE_OLD))) {
                    return new PyConditionValue(false);
                }
            }

            if (!left.isDetermined() || !right.isDetermined() || !left.getValue().isNumber() || !right.getValue().isNumber()) {
                return PyConditionValue.UNDEFINED;
            }

            if (op.equals(PyTokenTypes.LTLT) || op.equals(PyTokenTypes.GTGT)
                    || op.equals(PyTokenTypes.XOR) || op.equals(PyTokenTypes.AND) || op.equals(PyTokenTypes.OR)) {
                if (!left.getValue().isInteger() || !right.getValue().isInteger()) {
                    registerProblem(pyExpr,
                            "Unsupported operand types (" + left.getValue().getTypeString() + " and " +
                                    right.getValue().getTypeString() + ")");
                    return PyConditionValue.UNDEFINED;
                }
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
            }
            left.normalize();
            right.normalize();
            if (op.equals(PyTokenTypes.PLUS)) {
                return new PyConditionValue(left.getValue().add(right.getValue()));
            } else if (op.equals(PyTokenTypes.MINUS)) {
                return new PyConditionValue(left.getValue().subtract(right.getValue()));
            } else if (op.equals(PyTokenTypes.MULT)) {
                return new PyConditionValue(left.getValue().multiply(right.getValue()));
            } else if (op.equals(PyTokenTypes.DIV)) {
                PyValue divider = right.getValue();
                if (divider.equals(PyValue.ZERO)) {
                    registerProblem(pyExpr, "Division by 0");
                    return PyConditionValue.UNDEFINED;
                }
                return new PyConditionValue(left.getValue().divide(divider));
            } else if (op.equals(PyTokenTypes.EXP)) {
                PyValue l = left.getValue();
                PyValue r = right.getValue();
                if (l.equals(PyValue.ZERO)) {
                    if (r.equals(PyValue.ZERO)) {
                        return new PyConditionValue(PyValue.ONE);
                    } else if (r.compareTo(PyValue.ZERO) < 0) {
                        registerProblem(pyExpr, "0 cannot be raised to a negative power (" + r + ")");
                        return PyConditionValue.UNDEFINED;
                    }
                }
                if (!r.isInteger()) {
                    return PyConditionValue.UNDEFINED;
                }
                return new PyConditionValue(left.getValue().pow(right.getValue()));
            } else if (op.equals(PyTokenTypes.FLOORDIV)) {
                PyValue divider = right.getValue();
                if (divider.equals(PyValue.ZERO)) {
                    registerProblem(pyExpr, "Division by 0");
                    return PyConditionValue.UNDEFINED;
                }
                return new PyConditionValue(left.getValue().floordivide(divider));
            } else if (op.equals(PyTokenTypes.PERC)) {
                PyValue divisor = left.getValue();
                PyValue divider = right.getValue();
                if (divider.equals(PyValue.ZERO)) {
                    registerProblem(pyExpr, "Taking modulo by 0");
                    return PyConditionValue.UNDEFINED;
                }
                return new PyConditionValue(divisor.mod(divider));
            } else if (op.equals(PyTokenTypes.LTLT)) {
                if (right.getValue().compareTo(PyValue.ZERO) < 0) {
                    registerProblem(pyExpr, "Shifting by negative number (" + right.getValue() + ")");
                    return PyConditionValue.UNDEFINED;
                }
                return new PyConditionValue(left.getValue().shiftLeft(right.getValue()));
            } else if (op.equals(PyTokenTypes.GTGT)) {
                 if (right.getValue().compareTo(PyValue.ZERO) < 0) {
                    registerProblem(pyExpr, "Shifting by negative number (" + right.getValue() + ")");
                    return PyConditionValue.UNDEFINED;
                }
                return new PyConditionValue(left.getValue().shiftRight(right.getValue()));
            } else if (op.equals(PyTokenTypes.XOR)) {
                return new PyConditionValue(left.getValue().xor(right.getValue()));
            } else if (op.equals(PyTokenTypes.AND)) {
                return new PyConditionValue(left.getValue().and(right.getValue()));
            } else if (op.equals(PyTokenTypes.OR)) {
                return new PyConditionValue(left.getValue().or(right.getValue()));
            }
            return PyConditionValue.UNDEFINED;
        }

        private PyConditionValue processParExpr(PyParenthesizedExpression pyExpr) {
            return process(pyExpr.getContainedExpression());
        }
    }
}
