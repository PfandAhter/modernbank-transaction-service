package com.modernbank.transaction_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class DateRangeModel {
    private LocalDateTime start;

    private LocalDateTime end;
}