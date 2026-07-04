package com.tenpo.challenge.config;

import java.time.Duration;

public record RetryProperties(
    String name, int maxAttempts, Duration initialBackoff, double backoffMultiplier) {}
