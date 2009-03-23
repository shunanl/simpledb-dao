package com.cogniance.simpledb.model;

import java.io.Serializable;


/**
 * Interface for Simple DB entities
 * 
 * @author Andriy Gusyev
 */
public interface SimpleDBEntity<ID extends Serializable> extends Serializable {
        
        public ID getId();
        public void setId(ID id);
        
}
