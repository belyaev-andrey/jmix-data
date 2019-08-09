package org.springframework.data.jpa.repository.query;

import io.jmix.data.config.DeletedDate;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JmixJpaQueryCreator extends JpaQueryCreator {

    private final ReturnedType type;


    public JmixJpaQueryCreator(PartTree tree, ReturnedType type, CriteriaBuilder builder, ParameterMetadataProvider provider) {
        super(tree, type, builder, provider);
        this.type = type;
    }

    @Override
    protected CriteriaQuery<? extends Object> complete(Predicate predicate, Sort sort, CriteriaQuery<?> query, CriteriaBuilder builder, Root<?> root) {
        predicate = addSoftDelete(predicate, query, builder, root);
        return super.complete(predicate, sort, query, builder, root);
    }

    protected Predicate addSoftDelete(@Nullable Predicate predicate, CriteriaQuery<?> query, CriteriaBuilder builder, Root<?> root) {
        //TODO Refining query by adding soft delete - we also need to add row-based security expressions here
        Predicate queryRestriction = query.getRestriction();
        Field deletedBy = findDeletedMarkerField(type.getDomainType());
        Predicate isNotDeleted = builder.isNull(root.get(deletedBy.getName()));
        if (queryRestriction != null) {
            predicate = builder.and(queryRestriction);
        }
        return builder.and(isNotDeleted, predicate);
    }

    private Field findDeletedMarkerField(Class<?> aClass) {
        List<Field> fields = Arrays.stream(FieldUtils.getAllFields(aClass))
                .filter(field -> field.isAnnotationPresent(DeletedDate.class))
                .collect(Collectors.toList());
        if (fields.size() != 1) {
            throw new IllegalStateException("Entity should have exactly one deletedBy field, now it is: "+fields.size());
        }
        return fields.get(0);
    }

}
