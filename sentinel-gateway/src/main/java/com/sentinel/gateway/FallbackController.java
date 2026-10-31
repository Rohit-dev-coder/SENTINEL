package com.sentinel.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Target of the circuit breaker's fallbackUri: returns a clean 503 instead of a gateway error page
 * when a downstream service is failing or the breaker is open.
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/service-unavailable")
    public ResponseEntity<ProblemDetail> serviceUnavailable() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "The upstream service is temporarily unavailable. Please retry shortly.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
}
