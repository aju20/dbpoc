package com.safire.db;

import com.safire.models.GLCompany;
import com.safire.models.Journal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

@Repository
public class SafireRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final Logger LOG = LoggerFactory.getLogger(SafireRepository.class);
    public static final String INSERT_GL_COMP = "INSERT INTO GL_COMP_TEST (COMP, BASE_CCY, CV_CCY, REVAL_AC, REVAL_CC, WASH_AC, POSTING_DATE) VALUES (?, ?, ?, ? ,?, ?, ?)";
    public static final String INSERT_JOURNAL = "INSERT INTO JOURNAL_TEST (ID, COMP, AC, CC, CCY, CLIENT_JOB, EFF_DATE, ENTRY_DATE, SOURCE_CD, DR_CR_CODE, BASE_AMT, MX_AMT, CV_AMT, DESCR, EMP_ID, USERID, REF_NUMBER, GL_ENTRY_DATE, FLAG) VALUES (?, ?, ?, ? ,?, ?, ?, ?, ?, ?, ? ,?, ?, ?, ?, ?, ?, ? ,?)";

    public SafireRepository(JdbcTemplate jt) {
        jdbcTemplate = jt;
    }

    public void bulkCompanyInsert(Collection<GLCompany> companies) {
        Iterator<GLCompany> cIterator = companies.iterator();
        int[] results = jdbcTemplate.batchUpdate(INSERT_GL_COMP, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement stmt, int i) throws SQLException {
                GLCompany company = cIterator.next();
                stmt.setString(1, company.getId());
                stmt.setString(2, company.getBaseCurrencyId());
                stmt.setString(3, company.getCvCurrencyId());
                stmt.setString(4, company.getRevalAccount());
                stmt.setString(5, company.getRevalCostCenter());
                stmt.setString(6, company.getWashAccount());
                stmt.setTimestamp(7, company.getPostingDate());
            }

            @Override
            public int getBatchSize() {
                return companies.size();
            }
        });
        LOG.info("Bulk insert GL Company {} rows", results.length);
    }

    public void bulkJournalInsert(Collection<Journal> journals) {
        Iterator<Journal> jIterator = journals.iterator();
        int[] results = jdbcTemplate.batchUpdate(INSERT_JOURNAL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement stmt, int i) throws SQLException {
                Journal journal = jIterator.next();
                stmt.setLong(1, journal.getId());
                stmt.setString(2, journal.getCompanyId());
                stmt.setString(3, journal.getAccountId());
                stmt.setString(4, journal.getCenterId());
                stmt.setString(5, journal.getMxCurrencyId());
                stmt.setString(6, journal.getClientJobId());
                stmt.setTimestamp(7, journal.getEffectiveDate());
                stmt.setTimestamp(8, journal.getEntryDate());
                stmt.setString(9, journal.getSourceCode());
                stmt.setString(10, journal.getDebitCreditCode());
                stmt.setDouble(11, journal.getBaseAmount());
                stmt.setDouble(12, journal.getMxAmount());
                stmt.setDouble(13, journal.getCvAmount());
                stmt.setString(14, journal.getDescription());
                stmt.setString(15, journal.getEmployeeId());
                stmt.setString(16, journal.getUserId());
                stmt.setString(17, journal.getReferenceNumber());
                stmt.setTimestamp(18, journal.getGlEntryDate());
                stmt.setString(19, journal.getFlag());
            }

            @Override
            public int getBatchSize() {
                return journals.size();
            }
        });
        LOG.info("Bulk Insert Journal {} rows", results.length);
    }

    public void updateJournal(Collection<String> companyList) {
        String query = "UPDATE JOURNAL_TEST\n" +
                "SET MX_AMT = MX_AMT/2, BASE_AMT = BASE_AMT/2, CV_AMT = CV_AMT/2\n" +
                "WHERE COMP IN (" +  getPlaceholderString(companyList.size()) + ")";
        int result = jdbcTemplate.update(query, stmt -> {
            int i = 0;
            for(String companyId : companyList) {
                stmt.setString(++i, companyId);
            }
        });
        LOG.info("Update Journal {} rows",result);
    }

    public void deleteJournal(Collection<String> companyList) {
        String query = "DELETE FROM JOURNAL_TEST\n" +
                "WHERE COMP IN (" +  getPlaceholderString(companyList.size()) + ")";
        int result = jdbcTemplate.update(query, stmt -> {
            int i = 0;
            for(String companyId : companyList) {
                stmt.setString(++i, companyId);
            }
        });
        LOG.info("Delete Journal {} rows",result);
    }

    public void selectWithJoinAndAggregation(Collection<String> companyList) {
        String query = "SELECT \n" +
                "J.COMP, AC, CC, CCY, CLIENT_JOB, BASE_CCY, CV_CCY, \n" +
                "SUM(CASE WHEN DR_CR_CODE = 'DR' THEN MX_AMT ELSE -1 * MX_AMT END) AS \"TOTAL_MX\",\n" +
                "SUM(CASE WHEN DR_CR_CODE = 'DR' THEN BASE_AMT ELSE -1 * BASE_AMT END) AS \"TOTAL_BASE\",\n" +
                "SUM(CASE WHEN DR_CR_CODE = 'DR' THEN CV_AMT ELSE -1 * CV_AMT END) AS \"TOTAL_CV\"\n" +
                "FROM JOURNAL_TEST J\n" +
                "INNER JOIN GL_COMP_TEST G\n" +
                "ON J.COMP = G.COMP\n" +
                "WHERE J.COMP IN (" +  getPlaceholderString(companyList.size()) + ")\n" +
                "GROUP BY J.COMP, AC, CC, CCY, CLIENT_JOB, BASE_CCY, CV_CCY\n" +
                "ORDER BY J.COMP, AC, CC, CCY, CLIENT_JOB, BASE_CCY, CV_CCY";
        Integer result = jdbcTemplate.query(query, stmt -> {
            int i = 0;
            for (String companyId : companyList) {
                stmt.setString(++i, companyId);
            }
        }, rs -> {
            int count = 0;
            while(rs.next())
                count++;
            return count;
        });
        LOG.info("Select Journal {} rows",result);
    }

    @Transactional
    public void updateTransactional(Collection<Journal> journals, Collection<GLCompany> companies) {
        bulkCompanyInsert(companies);
        bulkJournalInsert(journals);
        Collection<String> companyIds = companies.stream().map(GLCompany::getId).collect(Collectors.toList());
        updateJournal(companyIds);
        deleteJournal(companyIds);
    }

    public void cleanTables() {
        jdbcTemplate.execute("DELETE FROM JOURNAL_TEST");
        jdbcTemplate.execute("DELETE FROM GL_COMP_tEST");
    }

    private String getPlaceholderString(int len) {
        String[] placeholder = new String[len];
        Arrays.fill(placeholder, "?");
        return String.join(",", placeholder);
    }
}
