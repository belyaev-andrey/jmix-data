package io.jmix.data.repository;

import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface SoftDeleteRepository<T, ID> {

    void setSoftDeleteEnabled(boolean softDeleteEnabled);

    boolean isSoftDeleteEnabled();

    void purge(T entity);

    void purgeById(ID id);

    void purgeAll(Iterable<? extends T> entities);

    void purgeAll();
}
