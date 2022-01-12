package com.safire.services;

import com.safire.db.SafireRepository;
import com.safire.models.GLCompany;
import com.safire.models.Journal;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class PerformanceEvaluator {
    private SafireRepository safireRepository;
    private static final Logger LOG = LoggerFactory.getLogger(PerformanceEvaluator.class);

    public PerformanceEvaluator(SafireRepository sr) {
        safireRepository = sr;
    }

    public void evaluatePerformance() {
        safireRepository.cleanTables();

        int dataSize = 1000, maxAttempts = 5;
        HashSet<GLCompany> uniqueCompanies = new HashSet<>();
        for(int attempt = 0; attempt < maxAttempts; attempt++) {
            HashSet<GLCompany> companies = new HashSet<>();
            while(companies.size() < dataSize) {
                GLCompany company = GLCompany.builder().build();
                if(!uniqueCompanies.contains(company)) {
                    companies.add(company);
                }
            }
            Metrics.timer("company.insert").record(() -> safireRepository.bulkCompanyInsert(companies));
            uniqueCompanies.addAll(companies);
        }
        GLCompany[] comapanyList = uniqueCompanies.toArray(new GLCompany[uniqueCompanies.size()]);

        dataSize = 20000;
        for(int attempt = 0; attempt < maxAttempts; attempt++) {
            List<Journal> journals = new ArrayList<>();
            for(int i = 0; i < dataSize; i++) {
                Journal j = Journal.builder().build();
                int id = attempt * dataSize + 1;
                j.setId((long)id);
                j.setCompanyId(comapanyList[id % comapanyList.length].getId());
                journals.add(j);
            }
            Metrics.timer("journal.insert.bulk").record(() -> safireRepository.bulkJournalInsert(journals));
        }

        dataSize = 50;
        HashSet<String> excludeCompanies = new HashSet<>();
        for(int attempt = 0; attempt < maxAttempts; attempt++) {
            List<String> comapanyIds = uniqueCompanies.stream().filter( c -> !excludeCompanies.contains(c.getId())).limit(dataSize).map(GLCompany::getId).collect(Collectors.toList());
            excludeCompanies.addAll(comapanyIds);
            Metrics.timer("journal.update").record(() -> safireRepository.updateJournal(comapanyIds));
        }

        excludeCompanies.clear();
        for(int attempt = 0; attempt < maxAttempts; attempt++) {
            List<String> comapanyIds = uniqueCompanies.stream().filter( c -> !excludeCompanies.contains(c.getId())).limit(dataSize).map(GLCompany::getId).collect(Collectors.toList());
            excludeCompanies.addAll(comapanyIds);
            Metrics.timer("journal.select").record(() -> safireRepository.selectWithJoinAndAggregation(comapanyIds));
        }

        excludeCompanies.clear();
        for(int attempt = 0; attempt < maxAttempts; attempt++) {
            List<String> comapanyIds = uniqueCompanies.stream().filter( c -> !excludeCompanies.contains(c.getId())).limit(dataSize).map(GLCompany::getId).collect(Collectors.toList());
            excludeCompanies.addAll(comapanyIds);
            Metrics.timer("journal.delete").record(() -> safireRepository.deleteJournal(comapanyIds));
        }
        Metrics.printTimerMetrics();
    }

    private static class Metrics {
        private static final SimpleMeterRegistry registry;

        static {
            registry = new SimpleMeterRegistry();
            registry.config().meterFilter(new MeterFilter() {
                @Override
                public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                    return DistributionStatisticConfig.builder()
                            .expiry(Duration.ofDays(24)).percentilesHistogram(true)
                            .build().merge(config);
                }
            });
            io.micrometer.core.instrument.Metrics.addRegistry(registry);
        }

        public static Timer timer(String name) {
            return Timer.builder(name).publishPercentiles(0.5, 0.6, 0.7, 0.8, 0.9, 1.0).register(registry);
        }

        public static void printTimerMetrics() {
            registry.forEachMeter(m -> {
                if(m instanceof Timer) {
                    Timer t = (Timer)m;
                    LOG.info("Timer : {}\nMean : {} ms\nMax : {} mx\nAttempts : {}\n| Percentile | Value |\n{}", t.getId().getName(), t.mean(TimeUnit.MILLISECONDS), t.max(TimeUnit.MILLISECONDS), t.count(),
                            Arrays.stream(t.takeSnapshot().percentileValues()).map( p -> String.format("| %s | %s |\n", p.percentile(), p.value(TimeUnit.MILLISECONDS))).reduce(String::concat).get());
                }
            });
        }
    }
}
