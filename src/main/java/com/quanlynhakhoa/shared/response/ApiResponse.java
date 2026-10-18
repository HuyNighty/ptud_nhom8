package com.quanlynhakhoa.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.quanlynhakhoa.shared.exception.ErrorCodeDetail;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApiResponse<T> {

    @Builder.Default
    int code = 200;

    String message;

    T result;

    List<ErrorCodeDetail> errors;
}
