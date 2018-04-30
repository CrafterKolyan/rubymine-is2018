package com.jetbrains.python.inspection;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.python.inspections.PyInspection;
import com.jetbrains.python.inspections.PyInspectionVisitor;
import com.jetbrains.python.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PyConstantExpression extends PyInspection {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly,
                                          @NotNull LocalInspectionToolSession session) {
        return new Visitor(holder, session);
    }

    private static class Visitor extends PyInspectionVisitor {

        enum Result { UNDEFINED, FALSE, TRUE }

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
            Result conditionValue = process(condition);
            if (conditionValue == Result.TRUE) {
                registerProblem(condition, "The condition is always " + "true");
            } else if (conditionValue == Result.FALSE) {
                registerProblem(condition, "The condition is always " + "false");
            }
        }

        private Result process(@NotNull PyExpression pyExpr){
            if (pyExpr instanceof PyBoolLiteralExpression) {
                final boolean retValue = ((PyBoolLiteralExpression) pyExpr).getValue();
                if (retValue) {
                    return Result.TRUE;
                } else {
                    return Result.FALSE;
                }
            }
            return Result.UNDEFINED;
        }
    }
}
