package org.springframework.data.repository.query;

import io.jmix.data.config.ExternalReference;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.support.Repositories;
import org.springframework.util.StringUtils;

import javax.persistence.PersistenceUnitUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JmixResultProcessor extends ResultProcessor {

    private final Repositories repositories;
    private final PersistenceUnitUtil persistenceUnitUtil;


    JmixResultProcessor(QueryMethod method, ProjectionFactory factory, ApplicationContext applicationContext, PersistenceUnitUtil persistenceUnitUtil) {
        super(method, factory);
        this.repositories = new Repositories(applicationContext);
        this.persistenceUnitUtil = persistenceUnitUtil;
    }

    @Override
    public <T> T processResult(Object source) {
        Object processed = processQueryResult(source);
        return super.processResult(processed);
    }

    @Override
    public <T> T processResult(Object source, Converter<Object, Object> preparingConverter) {
        Object processed = processQueryResult(source);
        return super.processResult(processed, preparingConverter);
    }

    @Override
    public ResultProcessor withDynamicProjection(ParameterAccessor accessor) {
        return super.withDynamicProjection(accessor);//TODO Might fail in some cases, should be overriden properly
    }

    private Object processQueryResult(Object result) {
        if (result instanceof Collection) {
            return ((Collection)result).stream().map(this::processSingleEntity).collect(Collectors.toList());
        } else if (result.getClass().isArray()) {
            return Arrays.stream((Object[])result).map(this::processSingleEntity).collect(Collectors.toList()).toArray();
        } else {
            return processSingleEntity(result);
        }
    }

    //TODO Works only for the 1st level entities - need to go through all entities in hierarchy
    private Object processSingleEntity(Object entity) {
        List<Field> fields =
                Arrays.stream(entity.getClass().getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(ExternalReference.class)) //if the field is not transient, app startup will fail
                        .collect(Collectors.toList());
        if (fields.isEmpty()) {
            return entity;
        }
        fields.forEach(f -> setFieldValue(entity, f));
        return entity;
    }

    private void setFieldValue(Object entity, Field valueField) {
        ExternalReference annotation = valueField.getAnnotation(ExternalReference.class);
        String referencedFieldName = annotation.references();
        String idFieldName = annotation.keyAttribute();

        if (!persistenceUnitUtil.isLoaded(entity, idFieldName)) {
            return;
        }

        Class<?> valueFieldType = valueField.getType();
        Object idValue = getId(entity, idFieldName);

        Object repository = repositories.getRepositoryFor(valueFieldType).orElseThrow(RuntimeException::new);
        String methodName = "findBy" + StringUtils.capitalize(referencedFieldName);
        Class<?>[] array = AopProxyUtils.proxiedUserInterfaces(repository);
        Method findById = Arrays.stream(array)
                .flatMap(c -> Arrays.stream(c.getMethods()))
                .filter(m -> m.getName().equals(methodName) && m.getParameterCount() == 1 && m.getReturnType().isAssignableFrom(valueFieldType))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        try {
            Object values =  Proxy.getInvocationHandler(repository).invoke(repository, findById, new Object[]{idValue});
            Object realValue = (values instanceof Optional) ? ((Optional)values).orElse(null) : values;
            FieldUtils.writeField(valueField, entity, realValue, true);
        } catch (Throwable throwable) {
            throw new IllegalArgumentException(throwable);
        }
    }

    private Object getId(Object entity, String idFieldName) {
        try {
            Field declaredField = FieldUtils.getField(entity.getClass(), idFieldName, true);
            return FieldUtils.readField(declaredField, entity);
        } catch (IllegalAccessException | SecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
