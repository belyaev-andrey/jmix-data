package org.springframework.data.jpa.repository.query;

import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;

public class JmixNativeJpaQuery extends JmixAbstractStringBasedJpaQuery {

    public JmixNativeJpaQuery(JpaQueryMethod method, EntityManager em, String queryString,
                              QueryMethodEvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser) {

        super(method, em, queryString, evaluationContextProvider, parser);

        Parameters<?, ?> parameters = method.getParameters();

        if (parameters.hasSortParameter() && !queryString.contains("#sort")) {
            throw new InvalidJpaQueryMethodException("Cannot use native queries with dynamic sorting in method " + method);
        }
    }

    @Override
    protected Query createJpaQuery(String queryString, ReturnedType returnedType) {

        EntityManager em = getEntityManager();
        Class<?> type = getTypeToQueryFor(returnedType);

        return type == null ? em.createNativeQuery(queryString) : em.createNativeQuery(queryString, type);
    }

    @Nullable
    private Class<?> getTypeToQueryFor(ReturnedType returnedType) {

        Class<?> result = getQueryMethod().isQueryForEntity() ? returnedType.getDomainType() : null;

        if (this.getQuery().hasConstructorExpression() || this.getQuery().isDefaultProjection()) {
            return result;
        }

        return returnedType.isProjecting() && !getMetamodel().isJpaManaged(returnedType.getReturnedType()) //
                ? Tuple.class
                : result;
    }
}
