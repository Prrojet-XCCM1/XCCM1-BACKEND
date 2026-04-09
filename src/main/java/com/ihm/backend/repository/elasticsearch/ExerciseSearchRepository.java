package com.ihm.backend.repository.elasticsearch;

import com.ihm.backend.entity.Exercise;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseSearchRepository extends ElasticsearchRepository<Exercise, Integer> {
}
