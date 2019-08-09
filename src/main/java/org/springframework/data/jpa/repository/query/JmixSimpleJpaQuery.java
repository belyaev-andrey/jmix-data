package org.springframework.data.jpa.repository.query;

import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import javax.persistence.EntityManager;

public class JmixSimpleJpaQuery extends JmixAbstractStringBasedJpaQuery {


    public JmixSimpleJpaQuery(JpaQueryMethod method, EntityManager em, String queryString,
                              QueryMethodEvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser) {

        super(method, em, queryString, evaluationContextProvider, parser);

        validateQuery(getQuery().getQueryString(), "Validation failed for query for method %s!", method);

        if (method.isPageQuery()) {
            validateQuery(getCountQuery().getQueryString(),
                    String.format("Count query validation failed for method %s!", method));
        }
    }

    private void validateQuery(String query, String errorMessage, Object... arguments) {

        if (getQueryMethod().isProcedureQuery()) {
            return;
        }

        EntityManager validatingEm = null;

        try {
            validatingEm = getEntityManager().getEntityManagerFactory().createEntityManager();
            validatingEm.createQuery(query);

        } catch (RuntimeException e) {

            // Needed as there's ambiguities in how an invalid query string shall be expressed by the persistence provider
            // http://java.net/projects/jpa-spec/lists/jsr338-experts/archive/2012-07/message/17
            throw new IllegalArgumentException(String.format(errorMessage, arguments), e);

        } finally {

            if (validatingEm != null) {
                validatingEm.close();
            }
        }
    }


}
