package com.cogniance.simpledb.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;

import org.apache.log4j.Logger;

import com.cogniance.simpledb.model.SimpleDBEntity;
import com.cogniance.simpledb.util.SimpleDBObjectBuilder;
import com.cogniance.simpledb.util.SimpleDBQueryBuilder;
import com.xerox.amazonws.sdb.Domain;
import com.xerox.amazonws.sdb.Item;
import com.xerox.amazonws.sdb.ItemAttribute;
import com.xerox.amazonws.sdb.QueryWithAttributesResult;
import com.xerox.amazonws.sdb.SDBException;
import com.xerox.amazonws.sdb.SimpleDB;

/**
 * Extend this class to have basic support for SimpleDB DAO. Child classes should implement getters
 * for access and secret keys, and also for model class.
 * 
 * @author Andriy Gusyev
 */
public abstract class SimpleDBDAOSupport<T extends SimpleDBEntity<ID>, ID extends Serializable> implements SimpleDBDAO<T, ID> {
    
    Logger logger = Logger.getLogger(SimpleDBDAOSupport.class);
    
    private static final String SELECT = "select * from %s where %s limit %s";
    
    private static final String SELECT_NO_CONDITION = "select * from %s limit %s";

    private static final String SELECT_COUNT_ALL = "select count(*) from %s";

    private static final String SELECT_COUNT_WHERE = "select count(*) from %s where %s";

    private static final String EMPTY_TOKEN = "";

    private static final Integer BATCH_SIZE = 250;

    private static SimpleDB sdb;

    protected abstract Class<T> getEntityClass();

    protected abstract String getAccessKey();

    protected abstract String getSecretKey();

    @SuppressWarnings("unchecked")
    protected Domain getDomain() throws SDBException {
        if (sdb == null) {
            sdb = new SimpleDB(getAccessKey(), getSecretKey());
        }
        Class clz = getEntityClass();
        String domainName;
        Entity annotation = (Entity) clz.getAnnotation(Entity.class);
        if (annotation != null) {
            domainName = annotation.name();
        } else {
            domainName = clz.getSimpleName();
        }
        return sdb.getDomain(domainName);
    }

    public List<T> getAll() {
        return getAll(EMPTY_TOKEN);
    }
    
    public List<T> getAll(String conditionQuery) {
        List<T> list = new ArrayList<T>();
        String token = EMPTY_TOKEN;
        while (token != null) {
            Result<T> result = getPortion(conditionQuery, BATCH_SIZE, token.equals(EMPTY_TOKEN) ? null : token);
            token = result.getNextToken();
            list.addAll(result.getItems());
        }
        return list;
    }

    public Result<T> getPortion(Integer count, String nextToken) {
        return getPortion(EMPTY_TOKEN, count, nextToken);
    }
    
    @SuppressWarnings("unchecked")
    public Result<T> getPortion(String conditionQuery, Integer count, String nextToken) {
        if (count == null || count.equals(0) || count > BATCH_SIZE) {
            count = BATCH_SIZE;
        }
        try {
            List<T> list = new ArrayList<T>();
            Domain domain = getDomain();
            QueryWithAttributesResult result = null;
            if (EMPTY_TOKEN.equals(conditionQuery)) {
                result = domain.selectItems(String.format(SELECT_NO_CONDITION, domain.getName(), count.toString()),
                        nextToken);
            } else {
                conditionQuery = SimpleDBQueryBuilder.transformQuery(getEntityClass(), conditionQuery);
                result = domain.selectItems(String.format(SELECT, domain.getName(), conditionQuery, count.toString()),
                        nextToken);
            }
            nextToken = result.getNextToken();
            for (List<ItemAttribute> attrs : result.getItems().values()) {
                T obj = (T) SimpleDBObjectBuilder.buildObject(getEntityClass(), attrs);
                list.add(obj);
            }
            return new Result<T>(list, nextToken);
        } catch (SDBException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public T getById(ID id) {
        try {
            Domain domain = getDomain();
            Item item = domain.getItem(id.toString());
            T obj = (T) SimpleDBObjectBuilder.buildObject(getEntityClass(), item.getAttributes());
            if (obj.getId() == null) {
                return null;
            } else {
                return obj;
            }    
        } catch (SDBException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public void saveOrUpdate(T entity) {
        try {
            Domain domain = getDomain();
            Item item = domain.getItem(entity.getId().toString());
            List<ItemAttribute> attrs = SimpleDBObjectBuilder.getItemAttributes(entity);
            item.putAttributes(attrs);
        } catch (SDBException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public void delete(T entity) {
        try {
            Domain domain = getDomain();
            ID id = entity.getId();
            domain.deleteItem(id.toString());
        } catch (SDBException e) {
            throw new IllegalStateException(e);
        }
    }

    public Integer countRows() {
        try {
            Domain domain = getDomain();
            String select = String.format(SELECT_COUNT_ALL, domain.getName());
            QueryWithAttributesResult result = domain.selectItems(select, null);
            for (List<ItemAttribute> list : result.getItems().values()) {
                if (list.size() > 0) {
                    ItemAttribute attr = list.get(0);
                    return Integer.parseInt(attr.getValue());
                }
            }
            return 0;
        } catch (SDBException e) {
            throw new IllegalStateException(e);
        }
    }

    public Integer countRows(String conditionQuery) {
        try {
            Domain domain = getDomain();
            conditionQuery = SimpleDBQueryBuilder.transformQuery(getEntityClass(), conditionQuery);
            String select = String.format(SELECT_COUNT_WHERE, domain.getName(), conditionQuery);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Select query: %s", select));
            }
            QueryWithAttributesResult result = domain.selectItems(select, null);
            for (List<ItemAttribute> list : result.getItems().values()) {
                if (list.size() > 0) {
                    ItemAttribute attr = list.get(0);
                    return Integer.parseInt(attr.getValue());
                }
            }
            return 0;
        } catch (SDBException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class Result<T> implements Serializable {

        private List<T> items;

        private String nextToken;

        public Result() {
        }

        public Result(List<T> items, String nextToken) {
            this.items = items;
            this.nextToken = nextToken;
        }

        public List<T> getItems() {
            return items;
        }

        public void setItems(List<T> items) {
            this.items = items;
        }

        public String getNextToken() {
            return nextToken;
        }

        public void setNextToken(String nextToken) {
            this.nextToken = nextToken;
        }

    }
}
