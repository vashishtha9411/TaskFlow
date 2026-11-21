package com.taskflow.dto.common;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationErrorDetails {
    private String field;
    private String message;
    private Object rejectedValue;
}
