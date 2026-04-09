package com.ihm.backend.repository.elasticsearch;

import com.ihm.backend.entity.CourseClass;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseClassSearchRepository extends ElasticsearchRepository<CourseClass, Long> {
}
