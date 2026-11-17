package com.rentalhub.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveOutRequestDTO {

    @NotNull(message = "Move-out date is required")
    private LocalDate moveOutDate;

    private String notes;
}