package com.safire.services;

import com.github.javafaker.Faker;
import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public class RandomDataGenerator {
    private static final FakeValuesService fakeValueService;
    private static final Faker faker;

    static {
        RandomService rs = new RandomService();
        fakeValueService = new FakeValuesService(Locale.ENGLISH, rs);
        faker = new Faker(Locale.ENGLISH, rs);
    }

    public static String getRandomCompany() { return fakeValueService.numerify("####"); }

    public static String getRandomCurrency() { return faker.currency().code(); }

    public static String getRandomAccount() { return fakeValueService.numerify("0######"); }

    public static String getRandomCostCenter() { return fakeValueService.bothify("000?####", true); }

    public static String getRandomClientJob() { return fakeValueService.regexify("[A-Z0-9]{4}[0-9]{4}"); }

    public static String getRandomDRCRCode() { return fakeValueService.regexify("[DC]R"); }

    public static double getRandomAmount() {
        return BigDecimal.valueOf(faker.random().nextDouble() * 10000000).setScale(3, RoundingMode.HALF_EVEN).doubleValue();
    }

    public static String getRandomFixedLengthString(int l) {
        return fakeValueService.regexify(String.format("[A-Z0-9a-z]{%s}", l));
    }

    public static String getRandomLengthString(int min, int max) {
        return getRandomFixedLengthString(faker.random().nextInt(max - min + 1) + min);
    }
}
