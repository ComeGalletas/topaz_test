package com.example.supportorders.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class DefaultStatusTransitionPolicyTest {

    private final StatusTransitionPolicy policy = new DefaultStatusTransitionPolicy();

    @ParameterizedTest(name = "{0} -> {1} permitido")
    @CsvSource({
            "OPEN, IN_PROGRESS",
            "OPEN, CANCELLED",
            "IN_PROGRESS, RESOLVED",
            "IN_PROGRESS, CANCELLED"
    })
    void allowsTransitionsFromTheSpec(OrderStatus from, OrderStatus to) {
        assertThat(policy.canTransition(from, to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} rechazado")
    @CsvSource({
            "OPEN, OPEN",
            "OPEN, RESOLVED",
            "IN_PROGRESS, OPEN",
            "RESOLVED, OPEN",
            "CANCELLED, IN_PROGRESS"
    })
    void rejectsOtherTransitions(OrderStatus from, OrderStatus to) {
        assertThat(policy.canTransition(from, to)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"RESOLVED", "CANCELLED"})
    void finalStatesHaveNoTransitions(OrderStatus status) {
        assertThat(policy.allowedTransitions(status)).isEmpty();
    }
}
