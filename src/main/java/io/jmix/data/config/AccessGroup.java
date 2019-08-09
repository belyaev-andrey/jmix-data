package io.jmix.data.config;

import javax.persistence.criteria.Predicate;
import java.security.Principal;
import java.util.List;
import java.util.Map;

public interface AccessGroup {

    String getName();

    List<Principal> getUsers();

    Map<Class, List<Predicate>> getCreatePredicates(Class entity);

    Map<Class, List<Predicate>> getReadPredicates(Class entity);

    Map<Class, List<Predicate>> getUpdatePredicates(Class entity);

    Map<Class, List<Predicate>> getDeletePredicates(Class entity);

    Map<String, Object> getSessionAttributes();

    AccessGroup getParent();
}
