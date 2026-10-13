package com.quanlynhakhoa.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorCodeDetail {

    private Integer code;

    private String field;

    private String message;

    private Object rejectedValue;
}
