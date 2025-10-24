package com.modernbank.transaction_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Table(name = "error_codes")
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorCodes {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "error")
    private String error;

    @Column(name = "description")
    private String description;
}