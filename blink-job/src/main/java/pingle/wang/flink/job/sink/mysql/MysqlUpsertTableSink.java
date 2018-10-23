package pingle.wang.flink.job.sink.mysql;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.types.Row;
import pingle.wang.flink.job.JobConstant;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * @Author: wpl
 */
public class MysqlUpsertTableSink extends RichSinkFunction<Tuple2<Boolean, Row>>{
    private Properties props;

    private Connection connection;

    private String drivername = "com.mysql.jdbc.Driver";
    private String dburl;
    private String username;
    private String password;
    private String tableName;

    private List<String> primaryKeys;

    private String[] fieldNames;
    private TypeInformation<?>[] fieldTypes;

    private String updateSQL;


    public MysqlUpsertTableSink(Properties props, String[] fieldNames, TypeInformation<?>[] fieldTypes) {

        this.props = props;

        this.fieldNames = fieldNames;
        this.fieldTypes = fieldTypes;

    }

    private void initSql() {

        StringBuffer keyStr = new StringBuffer();
        StringBuffer valStr = new StringBuffer();
        StringBuffer updStr = new StringBuffer();

        for (String fieldName : fieldNames) {
            keyStr.append(fieldName + ",");
            valStr.append("?,");

            if (!primaryKeys.contains(fieldName)){
                updStr.append(fieldName + " = ?,");
            }
        }

        String key = keyStr.substring(0, keyStr.length() - 1);
        String val = valStr.substring(0, valStr.length() - 1);
        String upd = updStr.substring(0, updStr.length() - 1);

        updateSQL = String.format(" insert into " + tableName + "(%s) values (%s) ON DUPLICATE KEY UPDATE %s ", key, val, upd);

    }

    private void updateRowSingle(Row row) throws SQLException {

        PreparedStatement preparedStatement = connection.prepareStatement(updateSQL);

        if (makePreparedStatement(row, preparedStatement)){
            preparedStatement.executeUpdate();
        }

        if (preparedStatement != null) {
            preparedStatement.close();
        }
    }

    private boolean makePreparedStatement(Row row, PreparedStatement preparedStatement) throws SQLException {

        List<HashMap<String, Object>> dupList = new ArrayList<>();

        int count = 0;

        boolean result = true;

        for (Object type : fieldTypes) {

            if (row.getField(count) == null){
                result = false;
                break;
            }

            HashMap<String, Object> dupMap = new HashMap<>();
            switch (type.toString().toLowerCase()) {
                case "string":
                    preparedStatement.setString(count + 1, String.valueOf(row.getField(count)));
                    dupMap.put("string", row.getField(count));
                    break;
                case "boolean":
                    preparedStatement.setBoolean(count + 1, (Boolean) row.getField(count));
                    dupMap.put("boolean", row.getField(count));
                    break;
                case "byte":
                    preparedStatement.setByte(count + 1, (Byte) row.getField(count));
                    dupMap.put("byte", row.getField(count));
                    break;
                case "short":
                    preparedStatement.setShort(count + 1, (Short) row.getField(count));
                    dupMap.put("short", row.getField(count));
                    break;
                case "integer":
                    preparedStatement.setInt(count + 1, (Integer) row.getField(count));
                    dupMap.put("integer", row.getField(count));
                    break;
                case "long":
                    preparedStatement.setLong(count + 1, (Long) row.getField(count));
                    dupMap.put("long", row.getField(count));
                    break;
                case "float":
                    preparedStatement.setFloat(count + 1, (Float) row.getField(count));
                    dupMap.put("float", row.getField(count));
                    break;
                case "double":
                    preparedStatement.setDouble(count + 1, (Double) row.getField(count));
                    dupMap.put("double", row.getField(count));
                    break;
                case "bigdecimal":
                    preparedStatement.setBigDecimal(count + 1, (BigDecimal) row.getField(count));
                    dupMap.put("bigdecimal", row.getField(count));
                    break;
                case "date":
                    preparedStatement.setDate(count + 1, (Date) row.getField(count));
                    dupMap.put("date", row.getField(count));
                    break;
                case "time":
                    preparedStatement.setTime(count + 1, (Time) row.getField(count));
                    dupMap.put("time", row.getField(count));
                    break;
                case "timestamp":
                    preparedStatement.setTimestamp(count + 1, (Timestamp) row.getField(count));
                    dupMap.put("timestamp", row.getField(count));
                    break;
                default:
                    break;
            }

            if (!primaryKeys.contains(fieldNames[count])){
                dupList.add(dupMap);
            }

            count++;
        }

        if (!result){
            return result;
        }

        for (HashMap<String, Object> map : dupList){

            if (map.containsKey("string")){
                preparedStatement.setString(count + 1, String.valueOf(map.get("string")));
            } else if (map.containsKey("boolean")) {
                preparedStatement.setBoolean(count + 1, (Boolean)map.get("boolean"));
            } else if (map.containsKey("byte")) {
                preparedStatement.setByte(count + 1, (Byte)map.get("byte"));
            } else if (map.containsKey("short")) {
                preparedStatement.setShort(count + 1, (Short)map.get("short"));
            } else if (map.containsKey("integer")) {
                preparedStatement.setInt(count + 1, (Integer)map.get("integer"));
            } else if (map.containsKey("long")) {
                preparedStatement.setLong(count + 1, (Long)map.get("long"));
            } else if (map.containsKey("float")) {
                preparedStatement.setFloat(count + 1, (Float)map.get("float"));
            } else if (map.containsKey("double")) {
                preparedStatement.setDouble(count + 1, (Double)map.get("double"));
            } else if (map.containsKey("bigdecimal")) {
                preparedStatement.setBigDecimal(count + 1, (BigDecimal)map.get("bigdecimal"));
            } else if (map.containsKey("date")) {
                preparedStatement.setDate(count + 1, (Date)map.get("date"));
            } else if (map.containsKey("time")) {
                preparedStatement.setTime(count + 1, (Time)map.get("time"));
            } else if (map.containsKey("timestamp")) {
                preparedStatement.setTimestamp(count + 1, (Timestamp)map.get("timestamp"));
            }

            count++;
        }
        return result;
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        String conn = props.getProperty(JobConstant.CONNECTION);
        String dbName = props.getProperty(JobConstant.DBNAME);

        dburl = "jdbc:mysql://" + conn + "/" + dbName + "?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&autoReconnect=true&autoReconnectForPools=true";
        username = props.getProperty(JobConstant.USER);
        password = props.getProperty(JobConstant.PASS);
        tableName = props.getProperty(JobConstant.TABLENAME);

        primaryKeys = Arrays.asList(props.getProperty(JobConstant.PRIMARYKEY).split(","));

        initSql();

        if (connection == null) {
            Class.forName(drivername);
            connection = DriverManager.getConnection(dburl, username, password);
        }

    }

    @Override
    public void invoke(Tuple2<Boolean, Row> tuple2) throws Exception {

        Boolean isUpdate = tuple2.f0;

        if (isUpdate){

            Row row = tuple2.f1;

            if (row != null && row.getArity() == fieldNames.length) {

                updateRowSingle(row);
            }
        }

    }

}
