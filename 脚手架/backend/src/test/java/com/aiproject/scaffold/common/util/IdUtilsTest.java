package com.aiproject.scaffold.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdUtilsTest {

    @Test
    void compactUuidContainsOnlyHexCharacters() {
        assertThat(IdUtils.compactUuid())
                .hasSize(32)
                .matches("[0-9a-f]{32}");
    }
}
