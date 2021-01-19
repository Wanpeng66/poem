package com.baizhi.poem.elastic.repository;

import com.baizhi.poem.entity.Poem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PoemRepository extends ElasticsearchRepository<Poem,String> {
}
