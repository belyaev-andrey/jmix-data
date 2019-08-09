package io.jmix.data.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JmixQueryLookupStrategy;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import java.util.Optional;

public class JmixRepositoryFactory extends JpaRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(JmixRepositoryFactory.class);

    private final EntityManager entityManager;
    private final QueryExtractor extractor;
    private EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;
    private ApplicationContext applicationContext;


    public JmixRepositoryFactory(EntityManager entityManager, ApplicationContext applicationContext) {
        super(entityManager);
        this.applicationContext = applicationContext;
        Assert.notNull(entityManager, "EntityManager must not be null!");

        this.entityManager = entityManager;
        this.extractor = PersistenceProvider.fromEntityManager(entityManager);

    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(QueryLookupStrategy.Key key, QueryMethodEvaluationContextProvider evaluationContextProvider) {
        Optional<QueryLookupStrategy> queryLookupStrategy =
                Optional.of(JmixQueryLookupStrategy.create(entityManager, key, extractor, evaluationContextProvider, escapeCharacter, applicationContext));
        return queryLookupStrategy;
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return JmixCrudRepositoryImpl.class;
    }


    @Override
    public <T> T getRepository(Class<T> repositoryInterface, RepositoryComposition.RepositoryFragments fragments) {
        T repository = super.getRepository(repositoryInterface, fragments);
        if (repository instanceof SoftDeleteRepository) {
            ((SoftDeleteRepository)repository).setSoftDeleteEnabled(true);
        }
        return repository;
    }

    @Override
    public void setEscapeCharacter(EscapeCharacter escapeCharacter) {
        super.setEscapeCharacter(escapeCharacter);
        this.escapeCharacter = escapeCharacter;
    }

}
