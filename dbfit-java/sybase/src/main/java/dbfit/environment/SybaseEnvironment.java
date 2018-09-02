package dbfit.environment;

import dbfit.annotations.DatabaseEnvironment;
import dbfit.api.AbstractDbEnvironment;
import dbfit.util.DbParameterAccessor;
import dbfit.util.DbParameterAccessorsMapBuilder;
import dbfit.util.Direction;
import static dbfit.environment.SybaseTypeNameNormaliser.normaliseTypeName;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import dbfit.util.Options;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@DatabaseEnvironment(name="Sybase", driver="com.sybase.jdbc4.jdbc.SybDriver")
public class SybaseEnvironment extends AbstractDbEnvironment {

    public SybaseEnvironment(String driverClassName) {
        super(driverClassName);
        defaultParamPatternString = "@([A-Za-z0-9_]+)";
        Options.setOption(Options.OPTION_PARAMETER_PATTERN, paramNamePattern);
    }

    public boolean supportsOuputOnInsert() {
        return false;
    }

    @Override
    protected String getConnectionString(String dataSource) {
        return "jdbc:sybase:Tds:" + dataSource;
    }

    @Override
    protected String getConnectionString(String dataSource, String database) {
        return getConnectionString(dataSource) + "?ServiceName=" + database;
    }

    @Override
    public void connect(String connectionString, Properties info) throws SQLException {
        // Add sendTimeAsDatetime=false option to enforce sending Time as
        // java.sql.Time (otherwise some precision is lost in conversions)
        super.connect(connectionString, info);
    }

    private static String paramNamePattern = "@([A-Za-z0-9_]+)";

    public Map<String, DbParameterAccessor> getAllColumns(String tableOrViewName)
            throws SQLException {
        String check = " and u.name || '.' || o.name = ? ";
        // Adding current user_name if it's not provided
        if (tableOrViewName.indexOf('.') == -1)
            check = " and u.name = user_name() and o.name = ? ";
        String query = " select c.name, t.name as type "
                + " from dbo.sysobjects o "
                + " join dbo.sysusers u on u.uid = o.uid "
                + " join dbo.syscolumns c on c.id = o.id "
                + " join dbo.systypes t on t.type = c.type and t.usertype = c.usertype "
                + " where o.type in ('U', 'V') "
                + check
                + " order by colid";

        DbParameterAccessorsMapBuilder params = new DbParameterAccessorsMapBuilder(dbfitToJdbcTransformerFactory);

        try (PreparedStatement dc = currentConnection.prepareStatement(query)) {
            dc.setString(1, tableOrViewName);
            ResultSet rs = dc.executeQuery();

            while (rs.next()) {
                String paramName = defaultIfNull(rs.getString("name"), "");
                params.add(paramName,
                           Direction.INPUT,
                           getSqlType(rs.getString("type")),
                           getJavaClass(rs.getString("type")));
            }
        }

        return params.toMap();
    }
/*
    private static List<DataType> sybaseTypes2 = Arrays.asList(new DataType[] {
        new DataType("BIT", Types.BIT, Boolean.class),
        new DataType("TINYINT", Types.TINYINT, Integer.class)
    }
    );
*/
    private static List<String> bitBooleanTypes = Arrays.asList(new String[] {
        "BIT" });               // BIT, java.lang.Boolean

    private static List<String> tinyintIntegerTypes = Arrays.asList(new String[] {
        "TINYINT" });           // TINYINT, java.lang.Integer

    private static List<String> bigintLongTypes = Arrays.asList(new String[] {
        "BIGINT" } );           // BIGINT, java.lang.Long;

    private static List<String> bigintBigDecimalTypes = Arrays.asList(new String[] {
        "UNSIGNED BIGINT" } );  // BIGINT, java.math.BigDecimal

    private static List<String> longvarcharStringTypes = Arrays.asList(new String[] {
        "TEXT" } );             // LONGVARCHAR, java.lang.String

    private static List<String> charStringTypes = Arrays.asList(new String[] {
        "CHAR" } );             // CHAR, java.lang.String

    private static List<String> numericBigDecimalTypes = Arrays.asList(new String[] {
        "MONEY", "SMALLMONEY", "NUMERIC" } );    // MONEY/SMALLMONEY/NUMERIC, java.math.BigDecimal

    private static List<String> decimalBigDecimalTypes = Arrays.asList(new String[] {
        "DECIMAL" } );          // DECIMAL, java.math.BigDecimal

    // For queries returning an UNSIGNED INT the Sybase JDBC driver says that ResultSetMetadata will
    // create a Long but in fact it creates an Integer;
    private static List<String> integerIntegerTypes = Arrays.asList(new String[] {
        "INT", "INTEGER", "UNSIGNED INT" } );  // INTEGER, java.lang.Integer

    private static List<String> smallintIntegerTypes = Arrays.asList(new String[] {
        "SMALLINT" } );         // SMALLINT, java.lang.Integer

    private static List<String> realFloatTypes = Arrays.asList(new String[] {
        "REAL" } );             // REAL, java.lang.Float

    private static List<String> doubleDoubleTypes = Arrays.asList(new String[] {
        "FLOAT" } );            // DOUBLE, java.lang.Double

    private static List<String> varcharStringTypes = Arrays.asList(new String[] {
        "VARCHAR" } );          // VARCHAR, java.lang.String

    private static List<String> dateDateTypes = Arrays.asList(new String[] {
        "DATE" } );             // DATE, java.sql.Data

    private static List<String> timeTimestampTypes = Arrays.asList(new String[] { // Sybase create a timestamp subtype of java.sql.Time;
        "TIME" } );             // TIME, java.sql.Timestamp

    private static List<String> timestampTimestampTypes = Arrays.asList(new String[] {
        "TIMESTAMP", "DATETIME", "SMALLDATETIME" } ); // TIME, java.sql.Time

    private static int getSqlType(String dataType) {
System.out.println("SybaseEnvironment: getSqlType: dataType: " + dataType);
        // todo:strip everything from first blank
        dataType = normaliseTypeName(dataType);

        if (bitBooleanTypes.contains(dataType))
            return Types.BIT;
        if (tinyintIntegerTypes.contains(dataType))
            return Types.INTEGER;
        if (bigintLongTypes.contains(dataType))
            return Types.BIGINT;
        if (bigintBigDecimalTypes.contains(dataType))
            return Types.BIGINT;
        if (longvarcharStringTypes.contains(dataType))
            return Types.LONGVARCHAR;
        if (charStringTypes.contains(dataType))
            return Types.CHAR;
        if (numericBigDecimalTypes.contains(dataType))
            return Types.NUMERIC;
        if (decimalBigDecimalTypes.contains(dataType))
            return Types.DECIMAL;
        if (integerIntegerTypes.contains(dataType))
            return Types.INTEGER;
        if (smallintIntegerTypes.contains(dataType))
            return Types.SMALLINT;
        if (realFloatTypes.contains(dataType))
            return Types.REAL;
        if (doubleDoubleTypes.contains(dataType))
            return Types.DOUBLE;
        if (varcharStringTypes.contains(dataType))
            return Types.VARCHAR;
        if (dateDateTypes.contains(dataType))
            return Types.DATE;
        if (timeTimestampTypes.contains(dataType)) {
System.out.println("Found a time timeTimeTypes");
            return Types.TIME;
        }
        if (timestampTimestampTypes.contains(dataType)) {
            return Types.TIMESTAMP;
        }
        throw new UnsupportedOperationException("Type " + dataType
                + " is not supported");
    }

    public Class<?> getJavaClass(String dataType) {
System.out.println("SybaseEnvironment: getJavaClass: " + dataType);
        dataType = normaliseTypeName(dataType);

        if (bitBooleanTypes.contains(dataType))
            return Boolean.class;
        if (tinyintIntegerTypes.contains(dataType))
            return Integer.class;
        if (bigintLongTypes.contains(dataType))
            return Long.class;
        if (bigintBigDecimalTypes.contains(dataType))
            return BigDecimal.class;
        if (longvarcharStringTypes.contains(dataType))
            return String.class;
        if (charStringTypes.contains(dataType))
            return String.class;
        if (numericBigDecimalTypes.contains(dataType))
            return BigDecimal.class;
        if (decimalBigDecimalTypes.contains(dataType))
            return BigDecimal.class;
        if (integerIntegerTypes.contains(dataType))
            return Integer.class;
        if (smallintIntegerTypes.contains(dataType))
            return Integer.class;
        if (realFloatTypes.contains(dataType))
            return Float.class;
        if (doubleDoubleTypes.contains(dataType))
            return Double.class;
        if (varcharStringTypes.contains(dataType))
            return String.class;
        if (dateDateTypes.contains(dataType))
            return java.sql.Date.class;
        if (timeTimestampTypes.contains(dataType)) {
System.out.println("Found a time timeTimeTypes");
            return java.sql.Timestamp.class;
        }
        if (timestampTimestampTypes.contains(dataType))
            return java.sql.Timestamp.class;
        throw new UnsupportedOperationException("Type " + dataType
                + " is not supported");
    }

    public Map<String, DbParameterAccessor> getAllProcedureParameters(
            String procName) throws SQLException {
        String query = "exec sp_sproc_columns ?";
        // Adding current user_name if it's not provided
        if (procName.indexOf('.') == -1)
            query = "exec sp_sproc_columns user_name() || '.' || ?";

        DbParameterAccessorsMapBuilder params = new DbParameterAccessorsMapBuilder(dbfitToJdbcTransformerFactory);
        try (PreparedStatement dc = currentConnection.prepareStatement(query)) {
            dc.setString(1, procName);
            ResultSet rs = dc.executeQuery();

            while (rs.next()) {
                String paramName = rs.getString("column_name").substring(1).trim();
                String paramType = rs.getString("column_type").trim();
                String paramDataType = rs.getString("type_name").trim();

                Direction direction;
                if ("IN".equals(paramType)) direction = Direction.INPUT;
                    else if ("INOUT".equals(paramType)) direction = Direction.INPUT_OUTPUT;
                    else direction = Direction.RETURN_VALUE;

                params.add(("RETURN_VALUE".equals(paramName))? "" : paramName,
                           direction,
                           getSqlType(paramDataType),
                           getJavaClass(paramDataType));
            }
        }

        return params.toMap();
    }

    @Override
    public void bindStatementParameter(PreparedStatement statement, int parameterIndex, Object value)
            throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.CHAR);
        } else {
            statement.setObject(parameterIndex, value);
        }
    }

    public String buildInsertCommand(String tableName,
            DbParameterAccessor[] accessors) {
System.out.println("SybaseEnvironment: buildInsertCommand");
        StringBuilder sb = new StringBuilder("insert into ");
        sb.append(tableName).append("(");
        String comma = "";

        StringBuilder values = new StringBuilder();

        for (DbParameterAccessor accessor : accessors) {
            if (accessor.hasDirection(Direction.INPUT)) {
                sb.append(comma);
                values.append(comma);
                //This will allow column names that have spaces or are keywords.
                sb.append("[" + accessor.getName() + "]");
                values.append("?");
                comma = ",";
            }
        }
        sb.append(") values (");
        sb.append(values);
        sb.append(")");
        return sb.toString();
    }
}
