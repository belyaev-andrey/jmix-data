package org.springframework.data.jpa.repository.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import static org.springframework.data.jpa.repository.query.QueryParameterSetter.ErrorHandling.LENIENT;

public class JmixAbstractStringBasedJpaQuery extends AbstractJpaQuery {

    private static final Logger log = LoggerFactory.getLogger(JmixAbstractStringBasedJpaQuery.class);


    private final DeclaredQuery query;
    private final DeclaredQuery countQuery;
    private final QueryMethodEvaluationContextProvider evaluationContextProvider;
    private final SpelExpressionParser parser;


    public JmixAbstractStringBasedJpaQuery(JpaQueryMethod method, EntityManager em, String queryString,
                                           QueryMethodEvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser) {

        super(method, em);

        Assert.hasText(queryString, "Query string must not be null or empty!");
        Assert.notNull(evaluationContextProvider, "ExpressionEvaluationContextProvider must not be null!");
        Assert.notNull(parser, "Parser must not be null!");

        log.info(method.getName()+" : "+queryString);

        //TODO here we should modify query string and add additional conditions for soft delete and row-based security

        this.evaluationContextProvider = evaluationContextProvider;
        this.query = new ExpressionBasedStringQuery(queryString, method.getEntityInformation(), parser);

        DeclaredQuery countQuery = query.deriveCountQuery(method.getCountQuery(), method.getCountQueryProjection());
        this.countQuery = ExpressionBasedStringQuery.from(countQuery, method.getEntityInformation(), parser);

        this.parser = parser;

        Assert.isTrue(method.isNativeQuery() || !query.usesJdbcStyleParameters(),
                "JDBC style parameters (?) are not supported for JPA queries.");
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateQuery(java.lang.Object[])
     */
    @Override
    public Query doCreateQuery(Object[] values) {

        ParameterAccessor accessor = new ParametersParameterAccessor(getQueryMethod().getParameters(), values);
        String sortedQueryString = QueryUtils.applySorting(query.getQueryString(), accessor.getSort(), query.getAlias());
        ResultProcessor processor = getQueryMethod().getResultProcessor().withDynamicProjection(accessor);

        Query query = createJpaQuery(sortedQueryString, processor.getReturnedType());

        // it is ok to reuse the binding contained in the ParameterBinder although we create a new query String because the
        // parameters in the query do not change.
        return parameterBinder.get().bindAndPrepare(query, values);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#createBinder(java.lang.Object[])
     */
    @Override
    protected ParameterBinder createBinder() {

        return ParameterBinderFactory.createQueryAwareBinder(getQueryMethod().getParameters(), query, parser,
                evaluationContextProvider);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateCountQuery(java.lang.Object[])
     */
    @Override
    protected Query doCreateCountQuery(Object[] values) {

        String queryString = countQuery.getQueryString();
        EntityManager em = getEntityManager();

        Query query = getQueryMethod().isNativeQuery() //
                ? em.createNativeQuery(queryString) //
                : em.createQuery(queryString, Long.class);

        return parameterBinder.get().bind(query, values, LENIENT);
    }

    /**
     * @return the query
     */
    public DeclaredQuery getQuery() {
        return query;
    }

    /**
     * @return the countQuery
     */
    public DeclaredQuery getCountQuery() {
        return countQuery;
    }

    /**
     * Creates an appropriate JPA query from an {@link EntityManager} according to the current {@link AbstractJpaQuery}
     * type.
     */
    protected Query createJpaQuery(String queryString, ReturnedType returnedType) {

        EntityManager em = getEntityManager();

        if (this.query.hasConstructorExpression() || this.query.isDefaultProjection()) {
            return em.createQuery(queryString);
        }

        return getTypeToRead(returnedType) //
                .<Query> map(it -> em.createQuery(queryString, it)) //
                .orElseGet(() -> em.createQuery(queryString));
    }


}
