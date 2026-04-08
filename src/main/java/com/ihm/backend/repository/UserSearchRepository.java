package com.ihm.backend.repository;

import com.ihm.backend.entity.User;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserSearchRepository extends ElasticsearchRepository<User, UUID> {
}
