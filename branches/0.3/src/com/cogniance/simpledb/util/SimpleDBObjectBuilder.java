package com.cogniance.simpledb.util;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;

import org.apache.commons.beanutils.PropertyUtils;

import com.xerox.amazonws.sdb.ItemAttribute;

/**
 * @author Andriy Gusyev
 */
public class SimpleDBObjectBuilder {

    @SuppressWarnings("unchecked")
    private static final Map<Class, Map<String, PropertyDescriptor>> cache = new HashMap<Class, Map<String, PropertyDescriptor>>();

    public static <T> Object buildObject(Class<T> clazz, List<ItemAttribute> attrs) {
        try {
            Object newObj = clazz.newInstance();
            Map<String, PropertyDescriptor> map = cache.get(clazz);
            if (map != null) {
                for (ItemAttribute attribute : attrs) {
                    PropertyDescriptor pd = map.get(attribute.getName());
                    if (pd != null) {
                        Method getter = clazz.getMethod(pd.getReadMethod().getName());
                        Object value = convertValue(getter.getReturnType(), attribute.getValue());
                        PropertyUtils.setProperty(newObj, pd.getName(), value);
                    }
                }
            } else {
                Map<String, PropertyDescriptor> newMap = new HashMap<String, PropertyDescriptor>();
                PropertyDescriptor[] properties = PropertyUtils.getPropertyDescriptors(clazz);
                for (PropertyDescriptor pd : properties) {
                    Method getter = clazz.getMethod(pd.getReadMethod().getName());
                    String propertyName = pd.getName();
                    String itemName = propertyName;
                    Annotation a = getter.getAnnotation(Column.class);
                    if (a != null) {
                        itemName = ((Column) a).name();
                    }
                    for (ItemAttribute attribute : attrs) {
                        if (attribute.getName().equals(itemName)) {
                            Object value = convertValue(getter.getReturnType(), attribute.getValue());
                            newMap.put(itemName, pd);
                            PropertyUtils.setProperty(newObj, propertyName, value);
                            break;
                        }
                    }
                }
                cache.put(clazz, newMap);
            }
            return newObj;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<ItemAttribute> getItemAttributes(Object entity) {
        Class clazz = entity.getClass();
        List<ItemAttribute> attrs = new ArrayList<ItemAttribute>();
        try {
            PropertyDescriptor[] properties = PropertyUtils.getPropertyDescriptors(entity);
            for (PropertyDescriptor pd : properties) {
                Method getter = clazz.getMethod(pd.getReadMethod().getName());
                String propertyName = pd.getName();
                Annotation a = getter.getAnnotation(Column.class);
                if (a != null) {
                    String attrName = ((Column) a).name();
                    attrs.add(new ItemAttribute(attrName, PropertyUtils.getProperty(entity, propertyName).toString(),
                            true));
                }

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
