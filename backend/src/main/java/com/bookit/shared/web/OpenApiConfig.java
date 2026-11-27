package com.bookit.shared.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.ArrayList;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenApiCustomizer responseFieldsRequired() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null || components.getSchemas() == null) {
                return;
            }
            components.getSchemas().forEach((name, schema) -> {
                if (name.endsWith("Response") && schema.getProperties() != null) {
                    schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
                }
            });
        };
    }

    @Bean
    OpenApiCustomizer flattenedProblemDetail() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null || components.getSchemas() == null) {
                return;
            }
            Schema<?> problemDetail = components.getSchemas().get("ProblemDetail");
            if (problemDetail == null || problemDetail.getProperties() == null) {
                return;
            }
            Schema<?> fieldError = new ObjectSchema()
                    .addProperty("field", new StringSchema())
                    .addProperty("message", new StringSchema())
                    .required(List.of("field", "message"));
            problemDetail.getProperties().remove("properties");
            problemDetail.addProperty("code", new StringSchema());
            problemDetail.addProperty("errors", new ArraySchema().items(fieldError));
        };
    }
}
