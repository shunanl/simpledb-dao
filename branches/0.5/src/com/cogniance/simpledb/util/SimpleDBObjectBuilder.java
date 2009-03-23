package com.cogniance.simpledb.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.PropertyUtils;

import com.xerox.amazonws.sdb.ItemAttribute;

/**
 * @author Andriy Gusyev
 */
@SuppressWarnings("unchecked")
public class SimpleDBObjectBuilder {
    
    private Map<Class, Converter> converters = new HashMap<Class, Converter>();
    
    public SimpleDBObjectBuilder(Map<Class, Converter> converters) {
        this.converters = converters;
    }
    
    public void addConverter(Class clazz, Converter converter) {
        this.converters.put(clazz, converter);
    }
    
    public void removeConverter(Class clazz) {
        this.converters.remove(clazz);
    }

    public <T> Object buildObject(Class<T> clazz, List<ItemAttribute> attrs) {
        try {
            Object newObj = clazz.newInstance();
            for (ItemAttribute attribute : attrs) {
                PropertyDescriptor pd = SimpleDBPropertyResolver.resolveAttribute(clazz, attribute.getName());
                if (pd != null) {
                    Method getter = clazz.getMethod(pd.getReadMethod().getName());
                    Object value = convertValue(getter.getReturnType(), attribute.getValue());
                    PropertyUtils.setProperty(newObj, pd.getName(), value);
                }
            }
            return newObj;
        } catch (Exception e) {
            throw new IllegalStateException("Can't build object", e);
        }
    }

    public List<ItemAttribute> getItemAttributes(Object entity) {
        Class clazz = entity.getClass();
        List<ItemAttribute> attrs = new ArrayList<ItemAttribute>();
        try {
            Map<String, String> map = SimpleDBPropertyResolver.resolveProperties(clazz);
            for (String propertyName : map.keySet()) {
                String attrName = map.get(propertyName);
                attrs.add(new ItemAttribute(attrName, PropertyUtils.getProperty(entity, propertyName).toString(),
                        true));
            }
            return attrs;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Object convertValue(Class requiredType, String originalValue) {
        Converter converter = converters.get(requiredType);
        if (converter != null) {
            return converter.convert(requiredType, originalValue);
        } else {
            return originalValue;
        }
    }
}
