
package com.quanlynhakhoa.configuration.web;

import com.quanlynhakhoa.shared.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public final class SystemController {

    @GetMapping("/ping")
    public ApiResponse<Void> ping() {
        return ApiResponse.<Void>builder()
                .message("pong")
                .build();
    }

    @GetMapping("/test-error")
    public void testError() {
        throw new IllegalStateException(
                "Simulated unexpected exception"
        );
    }
}
