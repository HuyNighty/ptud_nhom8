package com.quanlynhakhoa.configuration.web;

import com.quanlynhakhoa.shared.response.PingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minimal transport smoke check; this endpoint does not indicate database readiness. */
@RestController
@RequestMapping("/api/v1/system")
public final class SystemController {
    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("pong");
    }
}
