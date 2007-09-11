package liquibase.database;

import liquibase.exception.JDBCException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DB2Database extends AbstractDatabase {
    public boolean isCorrectDatabaseImplementation(Connection conn) throws JDBCException {
        return getDatabaseProductName(conn).startsWith("DB2");
    }

    public String getDefaultDriver(String url) {
        if (url.startsWith("jdbc:db2")) {
            return "com.ibm.db2.jcc.DB2Driver";
        }
        return null;
    }

    public String getProductName() {
        return "DB2";
    }

    public String getTypeName() {
        return "db2";
    }

    public String getSchemaName() throws JDBCException {//NOPMD
        return super.getSchemaName().toUpperCase();
    }

    public boolean supportsInitiallyDeferrableColumns() {
        return false;
    }

    public String getAutoIncrementClause() {
        return "GENERATED BY DEFAULT AS IDENTITY";
    }

    public String getCurrentDateTimeFunction() {
        return "CURRENT TIMESTAMP";
    }

    protected String getBooleanType() {
        return "SMALLINT";
    }

    public String getTrueBooleanValue() {
        return "1";
    }

    public String getFalseBooleanValue() {
        return "0";
    }

    /**
     * Return an DB2 date literal with the same value as a string formatted using ISO 8601.
     * <p/>
     * Convert an ISO8601 date string to one of the following results:
     * to_date('1995-05-23', 'YYYY-MM-DD')
     * to_date('1995-05-23 09:23:59', 'YYYY-MM-DD HH24:MI:SS')
     * <p/>
     * Implementation restriction:
     * Currently, only the following subsets of ISO8601 are supported:
     * YYYY-MM-DD
     * hh:mm:ss
     * YYYY-MM-DDThh:mm:ss
     */
    public String getDateLiteral(String isoDate) {
        String normalLiteral = super.getDateLiteral(isoDate);

        if (isDateOnly(isoDate)) {
            StringBuffer val = new StringBuffer();
            val.append("DATE(");
            val.append(normalLiteral);
            val.append(')');
            return val.toString();
        } else if (isTimeOnly(isoDate)) {
            StringBuffer val = new StringBuffer();
            val.append("TIME(");
            val.append(normalLiteral);
            val.append(')');
            return val.toString();
        } else if (isDateTime(isoDate)) {
            StringBuffer val = new StringBuffer();
            val.append("TIMESTAMP(");
            val.append(normalLiteral);
            val.append(')');
            return val.toString();
        } else {
            return "UNSUPPORTED:" + isoDate;
        }
    }

    protected String getCurrencyType() {
        return "DECIMAL";
    }

    protected String getUUIDType() {
        return "VARCHAR(36)";
    }

    protected String getClobType() {
        return "CLOB";
    }

    protected String getBlobType() {
        return "BLOB";
    }

    protected String getDateTimeType() {
        return "TIMESTAMP";
    }

    protected void dropSequences(DatabaseConnection conn) throws JDBCException {
        ResultSet rs = null;
        Statement selectStatement = null;
        Statement dropStatement = null;
        try {
            selectStatement = conn.createStatement();
            dropStatement = conn.createStatement();
            rs = selectStatement.executeQuery("SELECT SEQNAME FROM SYSIBM.SYSSEQUENCES WHERE SEQSCHEMA='" + getSchemaName() + "'");
            while (rs.next()) {
                String sequenceName = rs.getString("SEQNAME");
                log.finest("Dropping sequence " + sequenceName);
                String sql = "DROP SEQUENCE " + sequenceName;
                try {
                    dropStatement.executeUpdate(sql);
                } catch (SQLException e) {
                    throw new JDBCException("Error dropping sequence '" + sequenceName + "': " + e.getMessage(), e);
                }
            }
        } catch (SQLException e) {
            throw new JDBCException(e);
        } finally {
            try {
                if (selectStatement != null) {
                    selectStatement.close();
                }
                if (dropStatement != null) {
                    dropStatement.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                ;
            }
        }
    }


    public String createFindSequencesSQL() throws JDBCException {
        return "SELECT SEQNAME AS SEQUENCE_NAME FROM SYSCAT.SEQUENCES WHERE SEQSCHEMA = '" + getSchemaName() + "'";
    }


    public boolean shouldQuoteValue(String value) {
        return super.shouldQuoteValue(value)
                && !value.startsWith("\"SYSIBM\"");
    }


    public boolean supportsTablespaces() {
        return true;
    }

    protected String getViewDefinitionSql(String name) throws JDBCException {
        return "select view_definition from SYSIBM.VIEWS where TABLE_NAME='" + name + "' and TABLE_SCHEMA='" + getSchemaName() + "'";
    }

    public String getViewDefinition(String name) throws JDBCException {
        return super.getViewDefinition(name).replaceFirst("CREATE VIEW \\w+ AS ", ""); //db2 returns "create view....as select
    }

}
