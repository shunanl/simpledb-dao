package com.cogniance.simpledb.util;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;

import org.apache.commons.beanutils.PropertyUtils;

/**
 * @author Andriy Gusyev
 */
public class SimpleDBPropertyResolver implements Serializable {

    @SuppressWarnings("unchecked")
    public static final Map<Class, Map<String, PropertyDescriptor>> attributeCache = new HashMap<Class, Map<String, PropertyDescriptor>>();

    @SuppressWarnings("unchecked")
    public static final Map<Class, Map<String, String>> propertyCache = new HashMap<Class, Map<String, String>>();

    public static <T> PropertyDescriptor resolveAttribute(Class<T> clazz, String sdbAttributeName) {
        try {
            Map<String, PropertyDescriptor> map = attributeCache.get(clazz);
            if (map == null) {
                map = fillAttributeCache(clazz);
            }
            return map.get(sdbAttributeName);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Can't resolve attribute: " + sdbAttributeName, e);
        }
    }

    public static <T> String resolveProperty(Class<T> clazz, String propertyName) {
        try {
            Map<String, String> map = propertyCache.get(clazz);
            if (map == null) {
                map = fillProopertyCache(clazz);
            }
            return map.get(propertyName);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Can't resolve property: " + propertyName, e);
        }
    }

    public static <T> Map<String, String> resolveProperties(Class<T> clazz) {
        try {
            Map<String, String> map = propertyCache.get(clazz);
            if (map == null) {
                map = fillProopertyCache(clazz);
            }
            return map;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Can't resolve properties", e);
        }
    }

    private static <T> Map<String, PropertyDescriptor> fillAttributeCache(Class<T> clazz) throws NoSuchMethodException {
        Map<String, PropertyDescriptor> map = new HashMap<String, PropertyDescriptor>();
        PropertyDescriptor[] properties = PropertyUtils.getPropertyDescriptors(clazz);
        for (PropertyDescriptor pd : properties) {
            Method getter = clazz.getMethod(pd.getReadMethod().getName());
            String propertyName = pd.getName();
            String itemName = propertyName;
            Annotation a = getter.getAnnotation(Column.class);
            if (a != null) {
                itemName = ((Column) a).name();
            }
            map.put(itemName, pd);
        }
        attributeCache.put(clazz, map);
        return map;
    }

    private static <T> Map<String, String> fillProopertyCache(Class<T> clazz) throws NoSuchMethodException {
        Map<String, String> map = new HashMap<String, String>();
        PropertyDescriptor[] properties = PropertyUtils.getPropertyDescriptors(clazz);
        for (PropertyDescriptor pd : properties) {
            Method getter = clazz.getMethod(pd.getReadMethod().getName());
            String propertyName = pd.getName();
            Annotation a = getter.getAnnotation(Column.class);
            if (a != null) {
                String attrName = ((Column) a).name();
                map.put(propertyName, attrName);
            }
        }
        propertyCache.put(clazz, map);
        return map;
    }
}
