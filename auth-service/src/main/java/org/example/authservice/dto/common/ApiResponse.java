package org.example.authservice.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    int code;
    String message;
    T result;
}
