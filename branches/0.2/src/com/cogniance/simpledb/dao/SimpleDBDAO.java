package com.cogniance.simpledb.dao;

import java.io.Serializable;
import java.util.List;

import com.cogniance.simpledb.dao.SimpleDBDAOSupport.Result;
import com.cogniance.simpledb.model.SimpleDBEntity;


/**
 * @author Andriy Gusyev
 */
public interface SimpleDBDAO<T extends SimpleDBEntity<ID>, ID extends Serializable> {
    
    /**
     * Returns all entities
     * 
     * @return list of entities
     */
    List<T> getAll();
    
    /**
     * Returns portion of entities from SimpleDB, if count isn't set, returns max 250 entities. If
     * count greater then 250, returns max 250 entitties.
     * 
     * @param count - sets how much entities should be returned as maximim
     * @param nextToken - token for retrieveing next portion, nextToken could be taken from
     *        {@link Result} of previous portion, should be null for first portion.
     * @return {@link Result} which contains list of items and token for next portion.
     */
    Result<T> getPortion(Integer count, String nextToken);
    
    T getById(ID id);
    
    /**
     * Returns number of items in domain
     */
    Integer countRows();
    
    /**
     * Returns number of items with condition
     * 
     * @param conditionQuery - part of the query after WHERE
     */
    Integer countRows(String conditionQuery);

}
