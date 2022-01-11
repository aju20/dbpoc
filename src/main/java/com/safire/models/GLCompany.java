package com.safire.models;

import com.safire.services.RandomDataGenerator;
import lombok.*;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;

@Builder
@Getter @Setter @EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class GLCompany {
    @Builder.Default @EqualsAndHashCode.Include
    private String id = RandomDataGenerator.getRandomCompany();

    @Builder.Default
    private String baseCurrencyId = RandomDataGenerator.getRandomCurrency();

    @Builder.Default
    private String cvCurrencyId = RandomDataGenerator.getRandomCurrency();

    @Builder.Default
    private String revalAccount = RandomDataGenerator.getRandomAccount();

    @Builder.Default
    private String revalCostCenter = RandomDataGenerator.getRandomCostCenter();

    @Builder.Default
    private String washAccount = RandomDataGenerator.getRandomAccount();

    @Builder.Default
    private Timestamp postingDate = Timestamp.from(Instant.now(Clock.systemUTC()));
}
