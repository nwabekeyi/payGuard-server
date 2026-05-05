package com.payguard.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InitPaymentResponse {
    private String accessCode;
    private String reference;
    private String email;
    private String amount;
    private String currency;
    private String callbackUrl;
}
