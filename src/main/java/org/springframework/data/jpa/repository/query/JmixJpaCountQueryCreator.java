package org.springframework.data.jpa.repository.query;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public class JmixJpaCountQueryCreator extends JmixJpaQueryCreator {

    public JmixJpaCountQueryCreator(PartTree tree, ReturnedType type, CriteriaBuilder builder, ParameterMetadataProvider provider) {
        super(tree, type, builder, provider);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.jpa.repository.query.JpaQueryCreator#createCriteriaQuery(javax.persistence.criteria.CriteriaBuilder, org.springframework.data.repository.query.ReturnedType)
     */
    @Override
    protected CriteriaQuery<? extends Object> createCriteriaQuery(CriteriaBuilder builder, ReturnedType type) {
        return builder.createQuery(type.getDomainType());
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.jpa.repository.query.JpaQueryCreator#complete(javax.persistence.criteria.Predicate, org.springframework.data.domain.Sort, javax.persistence.criteria.CriteriaQuery, javax.persistence.criteria.CriteriaBuilder, javax.persistence.criteria.Root)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected CriteriaQuery<? extends Object> complete(@Nullable Predicate predicate, Sort sort,
                                                       CriteriaQuery<? extends Object> query, CriteriaBuilder builder, Root<?> root) {

        CriteriaQuery<? extends Object> select = query.select(getCountQuery(query, builder, root));

        predicate = addSoftDelete(predicate, query, builder, root);

        return predicate == null ? select : select.where(predicate);
    }

    @SuppressWarnings("rawtypes")
    private static Expression getCountQuery(CriteriaQuery<?> query, CriteriaBuilder builder, Root<?> root) {
        return query.isDistinct() ? builder.countDistinct(root) : builder.count(root);
    }


}
