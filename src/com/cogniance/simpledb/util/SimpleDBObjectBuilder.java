package com.cogniance.simpledb.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;

import com.xerox.amazonws.sdb.ItemAttribute;

/**
 * @author Andriy Gusyev
 */
public class SimpleDBObjectBuilder {

    public static <T> Object buildObject(Class<T> clazz, List<ItemAttribute> attrs) {
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

    @SuppressWarnings("unchecked")
    public static List<ItemAttribute> getItemAttributes(Object entity) {
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

    @SuppressWarnings("unchecked")
    private static Object convertValue(Class requiredType, String originalValue) {
        if (requiredType.isAssignableFrom(Integer.class)) {
            return Integer.parseInt(originalValue);
        } else if (requiredType.isAssignableFrom(Boolean.class)) {
            return Boolean.parseBoolean(originalValue);
        } else {
            return originalValue;
        }
    }
}
