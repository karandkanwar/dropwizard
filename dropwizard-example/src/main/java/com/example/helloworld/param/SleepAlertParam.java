package com.example.helloworld.param;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Optional;

public class SleepAlertParam {

    final long customerID;
    final LocalDate alertDate;

    @NotNull
    final ConcernLevel concernLevel;

    @JsonCreator
    public SleepAlertParam(
        @JsonProperty("customer") final long customerID,
        @JsonProperty("date") final Optional<LocalDate> alertDate,
        @JsonProperty("concern") final ConcernLevel concernLevel
    ) {
        this.customerID = customerID;
        // If not provided, assume the alert is for today
        this.alertDate = alertDate.orElse(LocalDate.now());
        this.concernLevel = concernLevel;
    }

    public long getCustomerID() {
        return customerID;
    }

    public LocalDate getAlertDate() {
        return alertDate;
    }

    public ConcernLevel getConcernLevel() {
        return concernLevel;
    }
}
