package io.jmix.data.repository;


import io.jmix.data.config.DeletedDate;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JmixCrudRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> implements SoftDeleteRepository<T, ID> {

    private static final Logger log = LoggerFactory.getLogger(JmixCrudRepositoryImpl.class);

    private Specification<T> deletedFilter;

    private EntityManager em;

    private final Field deleteDateField;

    private boolean softDeleteEnabled = false;

    public JmixCrudRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.em = entityManager;
        deleteDateField = setupDeletedField(entityInformation.getJavaType());
        deletedFilter = createFilterSpecification(deleteDateField);
    }

    public JmixCrudRepositoryImpl(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
        this.em = entityManager;
        deleteDateField = setupDeletedField(domainClass);
        deletedFilter = createFilterSpecification(deleteDateField);
    }

    @Override
    public void setSoftDeleteEnabled(boolean softDeleteEnabled) {
        this.softDeleteEnabled = softDeleteEnabled;
        deletedFilter = createFilterSpecification(deleteDateField);
    }

    @Override
    public boolean isSoftDeleteEnabled() {
        return softDeleteEnabled;
    }

    @Override
    public void deleteById(ID id) {
        if (softDeleteEnabled) {
            T entity = em.find(getDomainClass(), id);
            markAsDeleted(entity);
            em.merge(entity);
        } else {
            super.deleteById(id);
        }
    }


    @Override
    public void delete(T entity) {
        if (softDeleteEnabled) {
            em.merge(entity);
            markAsDeleted(entity);
            em.merge(entity);
        } else {
            super.delete(entity);
        }
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        if (softDeleteEnabled) {
            entities.forEach(entity -> {
                em.merge(entity);
                markAsDeleted(entity);
                em.merge(entity);
            });
        } else {
            super.deleteAll(entities);
        }
    }

    @Override
    public void deleteInBatch(Iterable<T> entities) {
        if (softDeleteEnabled) {
            entities.forEach(entity -> {
                em.merge(entity);
                markAsDeleted(entity);
                em.merge(entity);
            });
        } else {
            super.deleteInBatch(entities);
        }
    }

    @Override
    public void deleteAll() {
        if (softDeleteEnabled) {
            super.findAll().forEach(entity -> {
                em.merge(entity);
                markAsDeleted(entity);
                em.merge(entity);
            });
        } else {
            super.deleteAll();
        }
    }

    @Override
    public void deleteAllInBatch() {
        if (softDeleteEnabled) {
            super.findAll().forEach(entity -> {
                em.merge(entity);
                markAsDeleted(entity);
                em.merge(entity);
            });
        } else {
            super.deleteAllInBatch();
        }
    }


    @Override
    public void purge(T entity) {
        super.delete(entity);
    }

    @Override
    public void purgeById(ID id) {
        super.deleteById(id);
    }

    @Override
    public void purgeAll(Iterable<? extends T> entities) {
        super.deleteAll(entities);
    }

    @Override
    public void purgeAll() {
        super.deleteAll();
    }

    @Override
    public List<T> findAll() {
        TypedQuery<T> typedQuery = getQuery(deletedFilter, Sort.unsorted());
        logQueryString(typedQuery);
        return typedQuery.getResultList();
    }

    @Override
    public List<T> findAll(Specification<T> spec, Sort sort) {
        TypedQuery<T> typedQuery = getQuery(spec != null ? spec.and(deletedFilter) : deletedFilter, sort);
        logQueryString(typedQuery);
        return typedQuery.getResultList();
    }

    @Override
    public List<T> findAll(Specification<T> spec) {
        TypedQuery<T> typedQuery = getQuery(spec != null ? spec.and(deletedFilter) : deletedFilter, Sort.unsorted());
        logQueryString(typedQuery);
        return typedQuery.getResultList();
    }

    private Field findDeletedMarkerField(Class<T> aClass) {
        List<Field> fields = Arrays.stream(FieldUtils.getAllFields(aClass))
                .filter(field -> field.isAnnotationPresent(DeletedDate.class))
                .collect(Collectors.toList());
        if (fields.size() != 1) {
            throw new IllegalStateException("Entity should have exactly one deletedBy field, current amount is: "+fields.size());
        }
        return fields.get(0);
    }

    private Specification<T> createFilterSpecification(Field deleteDateField) {
        if (softDeleteEnabled) {
            return (Specification<T>) (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get(deleteDateField.getName()));
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.and();
        }
    }


    private void markAsDeleted(T entity) {
        LocalDate now = LocalDate.now();
        try {
            deleteDateField.set(entity, now);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot set field "+deleteDateField.getName()+" value "+now);
        }
    }

    private void logQueryString (Query query) {
        PersistenceProvider persistenceProvider = PersistenceProvider.fromEntityManager(em);
        if (persistenceProvider != PersistenceProvider.GENERIC_JPA) {
            log.info("Query string:"+persistenceProvider.extractQueryString(query));
        }
    }

    private Field setupDeletedField(Class<T> javaType) {
        Field deleteDateField = findDeletedMarkerField(javaType);
        deleteDateField.setAccessible(true);
        return deleteDateField;
    }


}
