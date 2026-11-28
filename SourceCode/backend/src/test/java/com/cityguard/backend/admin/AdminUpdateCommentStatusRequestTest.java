package com.cityguard.backend.admin;

import com.cityguard.backend.dto.admin.comment.AdminUpdateCommentStatusRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AdminUpdateCommentStatusRequestTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void distinguishesRestoreFromMissingStatusAndValidatesReason() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var restore = read("""
                    {"isHidden":false,"reason":"Reviewed"}
                    """);
            assertThat(restore.getIsHidden()).isFalse();
            assertThat(validator.validate(restore)).isEmpty();

            assertThat(invalidFields(validator, read("""
                    {"reason":"Reviewed"}
                    """))).containsExactly("isHidden");
            assertThat(invalidFields(validator, read("""
                    {"isHidden":true,"reason":"  "}
                    """))).containsExactly("reason");

            restore.setReason("a".repeat(1000));
            assertThat(validator.validate(restore)).isEmpty();
            restore.setReason("a".repeat(1001));
            assertThat(invalidFields(validator, restore)).containsExactly("reason");
        }
    }

    private AdminUpdateCommentStatusRequest read(String json) {
        return mapper.readValue(json, AdminUpdateCommentStatusRequest.class);
    }

    private static java.util.List<String> invalidFields(Validator validator, AdminUpdateCommentStatusRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList();
    }
}
