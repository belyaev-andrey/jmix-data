package org.springframework.data.repository.query;

import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceUnitUtil;
import java.lang.reflect.Method;

public class JmixJpaQueryMethod extends JpaQueryMethod {

    private ResultProcessor processor;

    public JmixJpaQueryMethod(Method method, RepositoryMetadata metadata,
                              ProjectionFactory factory, QueryExtractor extractor,
                              ApplicationContext applicationContext, EntityManager em) {
        super(method, metadata, factory, extractor);
        PersistenceUnitUtil persistenceUnitUtil = em.getEntityManagerFactory().getPersistenceUnitUtil();
        processor = new JmixResultProcessor(this, factory, applicationContext, persistenceUnitUtil);
    }

    @Override
    public ResultProcessor getResultProcessor() {
        return processor;
    }


}
