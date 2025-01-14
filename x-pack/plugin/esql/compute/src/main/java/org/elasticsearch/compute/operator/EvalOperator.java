/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.compute.operator;

import org.elasticsearch.compute.data.Block;
import org.elasticsearch.compute.data.Page;
import org.elasticsearch.core.Releasable;
import org.elasticsearch.core.Releasables;

/**
 * Evaluates a tree of functions for every position in the block, resulting in a
 * new block which is appended to the page.
 */
public class EvalOperator extends AbstractPageMappingOperator {

    public record EvalOperatorFactory(ExpressionEvaluator.Factory evaluator) implements OperatorFactory {

        @Override
        public Operator get(DriverContext driverContext) {
            return new EvalOperator(evaluator.get(driverContext));
        }

        @Override
        public String describe() {
            // TODO ThrowingDriverContext blows up when combined with Concat
            return "EvalOperator[evaluator=" + evaluator.get(new ThrowingDriverContext()) + "]";
        }
    }

    private final ExpressionEvaluator evaluator;

    public EvalOperator(ExpressionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    protected Page process(Page page) {
        return page.appendBlock(evaluator.eval(page));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[evaluator=" + evaluator + "]";
    }

    @Override
    public void close() {
        Releasables.closeExpectNoException(evaluator);
    }

    /**
     * Evaluates an expression {@code a + b} or {@code log(c)} one {@link Page} at a time.
     */
    public interface ExpressionEvaluator extends Releasable {
        /** A Factory for creating ExpressionEvaluators. */
        interface Factory {
            ExpressionEvaluator get(DriverContext driverContext);
        }

        /**
         * Evaluate the expression.
         */
        Block eval(Page page);
    }

    public static final ExpressionEvaluator CONSTANT_NULL = new ExpressionEvaluator() {
        @Override
        public Block eval(Page page) {
            return Block.constantNullBlock(page.getPositionCount());
        }

        @Override
        public String toString() {
            return "ConstantNull";
        }

        @Override
        public void close() {}
    };
}
