package org.springframework.data.jpa.repository.query;

import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.JmixJpaQueryMethod;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import java.lang.reflect.Method;

public class JmixQueryLookupStrategy {

    /**
     * Base class for {@link QueryLookupStrategy} implementations that need access to an {@link EntityManager}.
     *
     * @author Oliver Gierke
     * @author Thomas Darimont
     */
    private abstract static class AbstractQueryLookupStrategy implements QueryLookupStrategy {

        private final EntityManager em;
        private final QueryExtractor provider;
        protected final ApplicationContext applicationContext;

        /**
         * Creates a new {@link AbstractQueryLookupStrategy}.
         *
         * @param em
         * @param extractor
         */
        public AbstractQueryLookupStrategy(EntityManager em, QueryExtractor extractor, ApplicationContext applicationContext) {

            this.em = em;
            this.provider = extractor;
            this.applicationContext = applicationContext;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.projection.ProjectionFactory, org.springframework.data.repository.core.NamedQueries)
         */
        @Override
        public final RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
                                                  NamedQueries namedQueries) {
            return resolveQuery(new JmixJpaQueryMethod(method, metadata, factory, provider, applicationContext, em), em, namedQueries);
        }

        protected abstract RepositoryQuery resolveQuery(JpaQueryMethod method, EntityManager em, NamedQueries namedQueries);
    }

    /**
     * {@link QueryLookupStrategy} to create a query from the method name.
     *
     * @author Oliver Gierke
     * @author Thomas Darimont
     */
    private static class CreateQueryLookupStrategy extends AbstractQueryLookupStrategy {

        private final PersistenceProvider persistenceProvider;
        private final EscapeCharacter escape;

        public CreateQueryLookupStrategy(EntityManager em, QueryExtractor extractor, EscapeCharacter escape, ApplicationContext applicationContext) {

            super(em, extractor, applicationContext);

            this.persistenceProvider = PersistenceProvider.fromEntityManager(em);
            this.escape = escape;
        }

        @Override
        protected RepositoryQuery resolveQuery(JpaQueryMethod method, EntityManager em, NamedQueries namedQueries) {
            return new JmixPartTreeQuery(method, em, persistenceProvider, escape);
        }
    }

    /**
     * {@link QueryLookupStrategy} that tries to detect a declared query declared via {@link Query} annotation followed by
     * a JPA named query lookup.
     *
     * @author Oliver Gierke
     * @author Thomas Darimont
     */
    private static class DeclaredQueryLookupStrategy extends AbstractQueryLookupStrategy {

        private final QueryMethodEvaluationContextProvider evaluationContextProvider;

        /**
         * Creates a new {@link DeclaredQueryLookupStrategy}.
         *
         * @param em
         * @param extractor
         * @param evaluationContextProvider
         * @param applicationContext
         */
        public DeclaredQueryLookupStrategy(EntityManager em, QueryExtractor extractor,
                                           QueryMethodEvaluationContextProvider evaluationContextProvider, ApplicationContext applicationContext) {

            super(em, extractor, applicationContext);
            this.evaluationContextProvider = evaluationContextProvider;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.jpa.repository.query.JpaQueryLookupStrategy.AbstractQueryLookupStrategy#resolveQuery(org.springframework.data.jpa.repository.query.JpaQueryMethod, javax.persistence.EntityManager, org.springframework.data.repository.core.NamedQueries)
         */
        @Override
        protected RepositoryQuery resolveQuery(JpaQueryMethod method, EntityManager em, NamedQueries namedQueries) {

            RepositoryQuery query = JmixJpaQueryFactory.INSTANCE.fromQueryAnnotation(method, em, evaluationContextProvider);

            if (null != query) {
                return query;
            }

            query = JmixJpaQueryFactory.INSTANCE.fromProcedureAnnotation(method, em);

            if (null != query) {
                return query;
            }

            String name = method.getNamedQueryName();
            if (namedQueries.hasQuery(name)) {
                return JmixJpaQueryFactory.INSTANCE.fromMethodWithQueryString(method, em, namedQueries.getQuery(name),
                        evaluationContextProvider);
            }

            query = NamedQuery.lookupFrom(method, em);

            if (null != query) {
                return query;
            }

            throw new IllegalStateException(
                    String.format("Did neither find a NamedQuery nor an annotated query for method %s!", method));
        }
    }

    /**
     * {@link QueryLookupStrategy} to try to detect a declared query first (
     * {@link org.springframework.data.jpa.repository.Query}, JPA named query). In case none is found we fall back on
     * query creation.
     *
     * @author Oliver Gierke
     * @author Thomas Darimont
     */
    private static class CreateIfNotFoundQueryLookupStrategy extends AbstractQueryLookupStrategy {

        private final DeclaredQueryLookupStrategy lookupStrategy;
        private final CreateQueryLookupStrategy createStrategy;

        /**
         * Creates a new {@link CreateIfNotFoundQueryLookupStrategy}.
         *
         * @param em
         * @param extractor
         * @param createStrategy
         * @param lookupStrategy
         */
        public CreateIfNotFoundQueryLookupStrategy(EntityManager em, QueryExtractor extractor,
                                                   CreateQueryLookupStrategy createStrategy, DeclaredQueryLookupStrategy lookupStrategy, ApplicationContext applicationContext) {

            super(em, extractor, applicationContext);

            this.createStrategy = createStrategy;
            this.lookupStrategy = lookupStrategy;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.jpa.repository.query.JpaQueryLookupStrategy.AbstractQueryLookupStrategy#resolveQuery(org.springframework.data.jpa.repository.query.JpaQueryMethod, javax.persistence.EntityManager, org.springframework.data.repository.core.NamedQueries)
         */
        @Override
        protected RepositoryQuery resolveQuery(JpaQueryMethod method, EntityManager em, NamedQueries namedQueries) {

            try {
                return lookupStrategy.resolveQuery(method, em, namedQueries);
            } catch (IllegalStateException e) {
                return createStrategy.resolveQuery(method, em, namedQueries);
            }
        }
    }

    /**
     * Creates a {@link QueryLookupStrategy} for the given {@link EntityManager} and {@link QueryLookupStrategy.Key}.
     *
     * @param em                        must not be {@literal null}.
     * @param key                       may be {@literal null}.
     * @param extractor                 must not be {@literal null}.
     * @param evaluationContextProvider must not be {@literal null}.
     * @param escape
     * @param applicationContext
     * @return
     */
    public static QueryLookupStrategy create(EntityManager em, @Nullable QueryLookupStrategy.Key key, QueryExtractor extractor,
                                             QueryMethodEvaluationContextProvider evaluationContextProvider, EscapeCharacter escape
            , ApplicationContext applicationContext) {

        Assert.notNull(em, "EntityManager must not be null!");
        Assert.notNull(extractor, "QueryExtractor must not be null!");
        Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");

        switch (key != null ? key : QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND) {
            case CREATE:
                return new CreateQueryLookupStrategy(em, extractor, escape, applicationContext);
            case USE_DECLARED_QUERY:
                return new DeclaredQueryLookupStrategy(em, extractor, evaluationContextProvider, applicationContext);
            case CREATE_IF_NOT_FOUND:
                return new CreateIfNotFoundQueryLookupStrategy(em, extractor,
                        new CreateQueryLookupStrategy(em, extractor, escape, applicationContext),
                        new DeclaredQueryLookupStrategy(em, extractor, evaluationContextProvider, applicationContext), applicationContext);
            default:
                throw new IllegalArgumentException(String.format("Unsupported query lookup strategy %s!", key));
        }
    }

}
