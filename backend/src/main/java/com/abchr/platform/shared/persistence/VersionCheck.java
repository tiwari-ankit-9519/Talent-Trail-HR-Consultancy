package com.abchr.platform.shared.persistence;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

public final class VersionCheck {

    private VersionCheck() {
    }

    public static void verify(Long expected, Long actual, Class<?> type, Object id) {
        if (expected != null && !expected.equals(actual)) {
            throw new ObjectOptimisticLockingFailureException(type, id);
        }
    }
}
