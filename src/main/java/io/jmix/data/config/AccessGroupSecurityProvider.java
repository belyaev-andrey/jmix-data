package io.jmix.data.config;

import javax.persistence.criteria.Predicate;
import java.util.Map;

public interface AccessGroupSecurityProvider {
    //Will take username from the current security context

    Predicate getCreatePredicate(Class<?> entity);

    Predicate getReadPredicate(Class<?> entity);

    Predicate getUpdatePredicate(Class<?> entity);

    Predicate getDeletePredicate(Class<?> entity);

    Map<String, Object> getSessionAttributes(); //

}
