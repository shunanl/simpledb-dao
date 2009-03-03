package com.cogniance.simpledb.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;

import com.xerox.amazonws.sdb.ItemAttribute;

/**
 * @author Andriy Gusyev
 */
public class SimpleDBObjectBuilder {

    @SuppressWarnings("unchecked")
    private static final Map<Class, Map<String, PropertyDescription>> cache = new HashMap<Class, Map<String, PropertyDescription>>();

    @SuppressWarnings("unchecked")
    public static <T> Object buildObject(Class<T> clazz, List<ItemAttribute> attrs) {
        try {
            Object newObj = clazz.newInstance();
            Map<String, PropertyDescription> map = cache.get(clazz);
            if (map != null) {
                for (ItemAttribute attribute : attrs) {
                    PropertyDescription pd = map.get(attribute.getName());
                    if (pd != null) {
                        Object value = convertValue(pd.getType(), attribute.getValue());
                        PropertyUtils.setProperty(newObj, pd.getName(), value);
                    }
                }
            } else {
                Map<String, PropertyDescription> newMap = new HashMap<String, PropertyDescription>();
                for (Method method : clazz.getDeclaredMethods()) {
                    String methodName = method.getName();
                    Class methodType = method.getReturnType();
                    if (methodName.startsWith("get")) {
                        String propertyName = StringUtils.uncapitalize(methodName.substring(methodName.indexOf("get")
                                + "get".length()));
                        String itemName = propertyName;
                        Annotation a = method.getAnnotation(Column.class);
                        if (a != null) {
                            itemName = ((Column) a).name();
                        }
                        for (ItemAttribute attribute : attrs) {
                            if (attribute.getName().equals(itemName)) {
                                Object value = convertValue(methodType, attribute.getValue());
                                newMap.put(attribute.getName(), new PropertyDescription(propertyName, methodType));
                                PropertyUtils.setProperty(newObj, propertyName, value);
                                break;
                            }
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
    private static Object convertValue(Class requiredType, String originalValue) {
        if (requiredType.isAssignableFrom(Integer.class)) {
            return Integer.parseInt(originalValue);
        } else if (requiredType.isAssignableFrom(Boolean.class)) {
            return Boolean.parseBoolean(originalValue);
        } else {
            return originalValue;
        }
    }

    @SuppressWarnings("unchecked")
    private static class PropertyDescription {

        private String name;

        private Class type;

        public String getName() {
            return name;
        }

        public Class getType() {
            return type;
        }

        public PropertyDescription(String name, Class type) {
            this.name = name;
            this.type = type;
        }
    }
}
