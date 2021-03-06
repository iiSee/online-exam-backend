package edu.sandau.utils;

import edu.sandau.datasource.DruidManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JDBCUtil {
    private static JdbcTemplate jdbcTemplate;

    public JDBCUtil(JdbcTemplate jdbcTemplate) {
        JDBCUtil.jdbcTemplate = jdbcTemplate;
    }

    /***
     * 查询返回 ResultSet
     * @param sql
     * @return
     * @throws SQLException
     */
    public static ResultSet queryForResultSet( String sql ) throws SQLException {
        if ( !StringUtils.isEmpty(sql) ) {
            try(
                    Connection connection = DruidManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)
            ) {
                return statement.executeQuery();
            } catch ( Exception e ) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /***
     * 查询返回 List<Map<String, Object>>
     * @param sql
     * @return
     * @throws SQLException
     */
    public static List<Map<String, Object>> queryForList( String sql ) throws SQLException {
        if ( !StringUtils.isEmpty(sql) ) {
            List<Map<String, Object>> resultList = new ArrayList<>();
            try(
                    Connection connection = DruidManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet resultSet = statement.executeQuery()
            ) {
                ResultSetMetaData metaData = resultSet.getMetaData();   // 取得数据库的列名
                int numberOfColumns = metaData.getColumnCount();

                while ( resultSet.next() ) {
                    Map<String, Object> resultMap = new HashMap<>(numberOfColumns);
                    for ( int i = 1; i <= numberOfColumns; i++ ) {
                        resultMap.put(metaData.getColumnName(i), resultSet.getObject(i));
                    }
                    resultList.add(resultMap);
                }
                return resultList;
            } catch ( Exception e ) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /***
     * 分页查询实体集合
     * 当 start 和 end 为 null 或不合规时会查询全部内容
     * @param clazz 实体类.class
     * @param start 分页 开始序号
     * @param end   分页 结束序号
     * @return
     */
    public static List get( Class clazz, Integer start, Integer end ) {
        String clazzName = clazz.getSimpleName().toLowerCase();
        String tableName = getTableName(clazz);
        String SQL = " SELECT * FROM " + tableName + " ORDER BY ? ASC " ;
        boolean flag = false;
        if ( start != null && end != null && start >= 0 && end >= start ) {
            SQL = SQL + " LIMIT ?, ? ";
            flag = true;
        }
        List<Object> objects = new ArrayList<Object>();
        try(
                Connection connection = DruidManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(SQL)
        ) {
            statement.setString(1, clazzName+"_id");    //order by key
            if ( flag ) {
                statement.setInt(2, start);
                statement.setInt(3, end);
            }
            ResultSet resultSet = statement.executeQuery();
            String[] colNames = getColNames(resultSet);
            Method[] methods = clazz.getMethods();

            while ( resultSet.next() ) {
                Object object = clazz.newInstance();
                for (String colName : colNames) {
                    //获取set方法的方法名
                    String methodName = colName.substring(0,1).toUpperCase() + colName.substring(1);
                    methodName = "set" + methodName;
                    for (Method method : methods) {
                        if (methodName.endsWith(method.getName())) {
                            method.invoke(object, resultSet.getObject(colName));
                            break;
                        }
                    }
                }
                objects.add(object);
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            return null;
        }
        return objects;
    }

    /***
     * 通过实体类查询，返回List<Object>
     * @param clazz 类名.class
     * @return
     */
    public static List get( Class clazz ) {
        return get(clazz, null, null);
    }

    /***
     * 获取ResultSet查询结果的列名
     * @param rs
     * @return
     * @throws SQLException
     */
    private static String[] getColNames(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        //获取查询的列数
        int count = metaData.getColumnCount();
        String[] colNames = new String[count];
        for(int i = 1; i <= count; i ++) {
            //获取查询类的别名
            colNames[i - 1] = metaData.getColumnLabel(i);
        }
        return colNames;
    }

    /***
     *批量启动禁用功能
     * @param tableName,flag,idList
     * @return count.length
     */
    public static void deleteForRecord(String tableName, String flag,String idName,List<Integer> idArrays){
            String sql = " update " + tableName + " set " + flag + " = 0 where " + idName+ " = ?";
            List<Object[]> params = new ArrayList<>();
            for (Integer id : idArrays) {
                params.add(new Object[]{id});
            }
            jdbcTemplate.batchUpdate(sql,params);
    }

    /***
     * 通过变量 TABLE_NAME 获得数据库表名
     * @param clazz
     * @return
     */
    private static String getTableName(Class clazz) {
        String clazzName = null;
        try {
            Object obj = clazz.newInstance();
            Field field = clazz.getDeclaredField("TABLE_NAME");
            field.setAccessible(true);
            clazzName = field.get(obj).toString();
        } catch (NoSuchFieldException e) {
            return null;
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return clazzName;
    }

}
