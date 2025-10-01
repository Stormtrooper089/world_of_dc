package org.dcoffice.cachar.service;

import org.dcoffice.cachar.entity.Counter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class CounterService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public Long getNextSequence(String key) {
        Query query = new Query(Criteria.where("_id").is(key));
        Update update = new Update().inc("seq", 1);
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true).upsert(true);
        Counter counter = mongoTemplate.findAndModify(query, update, options, Counter.class);
        return counter.getSeq();
    }
}