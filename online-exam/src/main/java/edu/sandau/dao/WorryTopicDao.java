package edu.sandau.dao;


import edu.sandau.entity.WorryTopic;
import edu.sandau.rest.model.Page;
import edu.sandau.rest.model.WorryTopicData;
import edu.sandau.security.SessionWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class WorryTopicDao {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private SessionWrapper sessionWrapper;

    public void saveWorryTopic(List<WorryTopic> worryTopicList){
        String sql = "insert into worry_topic" +
                "(user_id,record_id,exam_id,topic_id,worryanswer,correctanswer)" +
                "VALUES(?,?,?,?,?,?)";
        List<Object[]> params = new ArrayList<Object[]>();
        worryTopicList.stream().forEach((worryTopic)->{
            Object[] obj = new Object[]{worryTopic.getUser_id(),worryTopic.getRecord_id()
                    ,worryTopic.getExam_id(),worryTopic.getTopic_id(),worryTopic.getWorryanswer(),worryTopic.getCorrectanswer()};
            params.add(obj);
        });
        jdbcTemplate.batchUpdate(sql,params);
    }


    public void deleteById(Integer recordId, Integer userId) {
        String sql = "delete from worry_topic where record_id = ? and user_id = ?";
        jdbcTemplate.update(sql,recordId,userId);
    }

    public List<WorryTopic> findById(Integer recordId, Integer userId) {
        String sql = "select * from worry_topic where record_id = ? and user_id = ?";
        List<WorryTopic> worryTopicList = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(WorryTopic.class), recordId, userId);
        return worryTopicList;
    }

    public List<WorryTopicData> selectAll(Page page) {
        int start = (page.getPageNo() - 1) * page.getPageSize();
        Map<String, Object> params = page.getOption();
        if(params == null || params.size() == 0){
            params = new HashMap<>();
        }
        List<Object> obj = new ArrayList<Object>();

        StringBuffer sql = new StringBuffer("SELECT t.id,t.`description`,t.`correctkey`,wt.`worryanswer`,t.`difficult`,t.`analysis`,t.`subject_id`,wt.`createtime` " +
                     "FROM worry_topic AS wt INNER JOIN topic t ON wt.topic_id = t.`id` where  1 = 1 ");

        String sqlAppend = getSqlAndParams(params, obj);
        sql.append(sqlAppend);
        int count = getCount(sql.toString(), obj);
        page.setTotal(count);
//        sql.append(" limit ?,?");
//        obj.add(start);
//        obj.add(page.getPageSize());
        return jdbcTemplate.query(sql.toString(), new BeanPropertyRowMapper<>(WorryTopicData.class), obj.toArray());
    }

    public int getCount(String sql, List<Object> obj) {
        StringBuilder sqlCount = new StringBuilder("select count(1) from  ( ");
        sqlCount.append(sql);
        sqlCount.append(" ) a");
        return jdbcTemplate.queryForObject(sqlCount.toString(),Integer.class,obj.toArray());
    }


    //拼接动态Sql
    private String getSqlAndParams( Map<String, Object> params,List<Object> obj){
        StringBuffer sql = new StringBuffer();
        Set<String> keySet = params.keySet();
        for (String key : keySet) {
            Object value = params.get(key);
            if(key.equalsIgnoreCase("user_id"))
            {
                sql.append(" AND wt." + key + " = ?");
            }else{
                sql.append(" AND  " + key + " = ?");
            }
            obj.add(value);
        }
        //如果前端没有传user_id，则将查询当前登录用户的错题记录
        if(!params.containsKey("user_id")){
            sql.append(" And wt.user_id = ?");
            obj.add(sessionWrapper.getUserId());
        }
        return sql.toString();
    }
}
