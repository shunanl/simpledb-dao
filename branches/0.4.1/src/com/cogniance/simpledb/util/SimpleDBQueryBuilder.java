package com.cogniance.simpledb.util;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;


/**
 * @author Andriy Gusyev
 */
public class SimpleDBQueryBuilder implements Serializable {

    public static <T> String transformQuery(Class<T> clazz, String query) {
        String[] tokens = StringUtils.splitPreserveAllTokens(query);
        for (String token : tokens) {
            String newValue = SimpleDBPropertyResolver.resolveProperty(clazz, token);
            if (newValue != null) {
                query = query.replaceFirst(token, newValue);
            }
        }
        return query;
    }
}
