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
import java.util.*;
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

        //test small insert set
        int dataSize = 1000, maxAttempts = 5, jId = 1;
        HashSet<GLCompany> uniqueCompanies = new HashSet<>();
        for(int attempt = 0; attempt < maxAttempts; attempt++) {
            HashSet<GLCompany> companies = getRandomCompanies(uniqueCompanies, dataSize);
            Metrics.timer("company.insert").record(() -> safireRepository.bulkCompanyInsert(companies));
            uniqueCompanies.addAll(companies);
        }
        GLCompany[] comapanyList = uniqueCompanies.toArray(new GLCompany[uniqueCompanies.size()]);

        //test bulk insert set
        dataSize = 20000;
        for(int attempt = 0; attempt < maxAttempts; attempt++) {
            List<Journal> journals = getRandomJournals(comapanyList, jId, dataSize);
            jId += journals.size();
            Metrics.timer("journal.insert.bulk").record(() -> safireRepository.bulkJournalInsert(journals));
        }

        //test update
        dataSize = 50;
        HashSet<String> excludeCompanies = new HashSet<>();
        for(int attempt = 0; attempt < maxAttempts; attempt++) {
            List<String> comapanyIds = uniqueCompanies.stream().filter( c -> !excludeCompanies.contains(c.getId())).limit(dataSize).map(GLCompany::getId).collect(Collectors.toList());
            excludeCompanies.addAll(comapanyIds);
            Metrics.timer("journal.update").record(() -> safireRepository.updateJournal(comapanyIds));
        }

        //test select with aggregation and join
        excludeCompanies.clear();
        for(int attempt = 0; attempt < maxAttempts; attempt++) {
            List<String> comapanyIds = uniqueCompanies.stream().filter( c -> !excludeCompanies.contains(c.getId())).limit(dataSize).map(GLCompany::getId).collect(Collectors.toList());
            excludeCompanies.addAll(comapanyIds);
            Metrics.timer("journal.select").record(() -> safireRepository.selectWithJoinAndAggregation(comapanyIds));
        }

        //test delete
        excludeCompanies.clear();
        for(int attempt = 0; attempt < maxAttempts; attempt++) {
            List<String> comapanyIds = uniqueCompanies.stream().filter( c -> !excludeCompanies.contains(c.getId())).limit(dataSize).map(GLCompany::getId).collect(Collectors.toList());
            excludeCompanies.addAll(comapanyIds);
            Metrics.timer("journal.delete").record(() -> safireRepository.deleteJournal(comapanyIds));
        }

        //test transactional behaviour
        for(int attempt = 0; attempt < maxAttempts; attempt++) {
            Set<GLCompany> glCompaniesSet = getRandomCompanies(uniqueCompanies, 10);
            uniqueCompanies.addAll(glCompaniesSet);
            GLCompany[] glCompaniesList = glCompaniesSet.toArray(new GLCompany[10]);
            List<Journal> journals = getRandomJournals(glCompaniesList, jId, 10);
            jId += journals.size();
            Metrics.timer("journal.transaction").record(() -> safireRepository.updateTransactional(journals, glCompaniesSet));
        }
        Metrics.printTimerMetrics();
    }

    private HashSet<GLCompany> getRandomCompanies(HashSet<GLCompany> notInCompanies, int companyCount) {
        HashSet<GLCompany> companies = new HashSet<>();
        while(companies.size() < companyCount) {
            GLCompany company = GLCompany.builder().build();
            if(!notInCompanies.contains(company)) {
                companies.add(company);
            }
        }
        return companies;
    }

    private List<Journal> getRandomJournals(GLCompany[] companies, int nextId, int journalCount) {
        List<Journal> journals = new ArrayList<>();
        for(int i = 0; i < journalCount; i++) {
            Journal j = Journal.builder().build();
            j.setCompanyId(companies[nextId % companies.length].getId());
            j.setId((long)nextId++);
            journals.add(j);
        }
        return journals;
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
                    LOG.info("Timer : {}\nMean : {} ms\nMax : {} ms\nAttempts : {}\n| Percentile | Value |\n{}", t.getId().getName(), t.mean(TimeUnit.MILLISECONDS), t.max(TimeUnit.MILLISECONDS), t.count(),
                            Arrays.stream(t.takeSnapshot().percentileValues()).map( p -> String.format("| %s | %s |\n", p.percentile(), p.value(TimeUnit.MILLISECONDS))).reduce(String::concat).get());
                }
            });
        }
    }
}
