package com.aiproject.scaffold.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaskUtilsTest {

    @Test
    void masksMobileAndEmail() {
        assertThat(MaskUtils.mobile("13812345678")).isEqualTo("138****5678");
        assertThat(MaskUtils.email("developer@example.com")).isEqualTo("d***@example.com");
    }
}
