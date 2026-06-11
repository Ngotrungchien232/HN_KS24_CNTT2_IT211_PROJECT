package com.project_cuoimon.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponseDto {
    private Long id;
    private String transactionCode;
    private BigDecimal amount;
    private String description;
    private String status;
    private String type; // "DEBIT" (Trừ tiền) hoặc "CREDIT" (Cộng tiền)
    private String fromAccountNumber;
    private String toAccountNumber;
    private LocalDateTime createdAt;
}
