package com.safire.models;

import com.safire.services.RandomDataGenerator;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;

@Builder
@Getter  @Setter @ToString
public class Journal {

    private Long id;

    private String companyId;

    @Builder.Default
    private String accountId = RandomDataGenerator.getRandomAccount();

    @Builder.Default
    private String centerId = RandomDataGenerator.getRandomCostCenter();

    @Builder.Default
    private String mxCurrencyId = RandomDataGenerator.getRandomCurrency();

    @Builder.Default
    private String clientJobId = RandomDataGenerator.getRandomClientJob();

    @Builder.Default
    private Timestamp effectiveDate = Timestamp.from(Instant.now(Clock.systemUTC()));

    @Builder.Default
    private String debitCreditCode = RandomDataGenerator.getRandomDRCRCode();

    @Builder.Default
    private double mxAmount = RandomDataGenerator.getRandomAmount();

    @Builder.Default
    private double baseAmount = RandomDataGenerator.getRandomAmount();

    @Builder.Default
    private double cvAmount = RandomDataGenerator.getRandomAmount();

    @Builder.Default
    private String description = RandomDataGenerator.getRandomLengthString(35, 50);

    @Builder.Default
    private String sourceCode = RandomDataGenerator.getRandomFixedLengthString(10);

    @Builder.Default
    private String employeeId = RandomDataGenerator.getRandomFixedLengthString(5);

    @Builder.Default
    private Timestamp entryDate = Timestamp.from(Instant.now(Clock.systemUTC()));

    @Builder.Default
    private Timestamp glEntryDate = Timestamp.from(Instant.now(Clock.systemUTC()));

    @Builder.Default
    private String userId = RandomDataGenerator.getRandomFixedLengthString(8);

    @Builder.Default
    private String flag = "0";

    @Builder.Default
    private String referenceNumber = RandomDataGenerator.getRandomFixedLengthString(9);

}
