package com.baizhi.poem.service;


import com.baizhi.poem.dao.JpaCatgoryDao;
import com.baizhi.poem.dao.JpaPoemDao;
import com.baizhi.poem.elastic.repository.PoemRepository;
import com.baizhi.poem.entity.Category;
import com.baizhi.poem.entity.JpaCategory;
import com.baizhi.poem.entity.JpaPoem;
import com.baizhi.poem.entity.Poem;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class PoemServiceImpl implements PoemService {

    @Autowired
    private JpaPoemDao poemDAO;
    @Autowired
    private JpaCatgoryDao catgoryDao;

    @Autowired
    private PoemRepository poemRepository;


    @Autowired
    private RestHighLevelClient elasticsearchClient;


    @Override
    public List<Poem> findByKeywords( String content, String type, String author, String subType ) {
        //定义list
        List<Poem> lists = null;
        try {
            SearchRequest searchRequest = new SearchRequest("poems");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            //搜索条件为空指定查询条件


            //指定过滤条件
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            if (!StringUtils.isEmpty(author)&&!author.equals( "所有" )) {
               boolQueryBuilder.filter(QueryBuilders.termQuery("author",author));
            }
            if (!StringUtils.isEmpty(type)&&!type.equals( "所有" )) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("type",type));
            }
            if(!StringUtils.isEmpty( subType )&&!subType.equals( "所有" )){
                boolQueryBuilder.filter( QueryBuilders.nestedQuery( "category",QueryBuilders.termQuery( "category.name",subType ), ScoreMode.None));
            }
            if (StringUtils.isEmpty(content)) {
               /* //设置为查询所有
                boolQueryBuilder.should( QueryBuilders.matchAllQuery() )
                sourceBuilder.query(QueryBuilders.matchAllQuery());*/
            } else {
                //设置为多字段检索
                String[] fields = {"name", "content", "author"};
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                for (String field : fields) {
                    MatchQueryBuilder queryBuilder = QueryBuilders.matchQuery( field, content );
                    queryBuilder.analyzer( "ik_smart" );
                    if (field.equals( "content" )) {
                        queryBuilder.boost(2f);
                    }
                    if (field.equals( "name" )||field.equals( "author" )) {
                        queryBuilder.boost( 1.5f );
                    }
                    boolQuery.should( queryBuilder );
                }
                DisMaxQueryBuilder disMaxQueryBuilder = QueryBuilders.disMaxQuery();
                disMaxQueryBuilder.add( boolQuery );
                disMaxQueryBuilder.tieBreaker( 0.4f );
                boolQueryBuilder.should( disMaxQueryBuilder );
                boolQueryBuilder.minimumShouldMatch( 1 );
            }

            //指定过滤
            //sourceBuilder.postFilter(boolQueryBuilder);
            sourceBuilder.query( boolQueryBuilder );
            //指定高亮
            sourceBuilder.highlighter(new HighlightBuilder().
                    field("name").field( "author" ).field( "content" ).requireFieldMatch(false).preTags("<span style='color:red;'>").postTags("</span>"));
            //指定显示记录
            sourceBuilder.size(20);

            //指定搜索类型
            searchRequest.source(sourceBuilder);

            SearchResponse searchResponse = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);


            //获取返回结果
            if (searchResponse.getHits().getTotalHits().value > 0) lists = new ArrayList<>();
            SearchHit[] hits = searchResponse.getHits().getHits();
            for (SearchHit hit : hits) {
                Poem poem = new Poem();
                //获取原始字段
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                //获取高亮字段
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                //id
                poem.setId(hit.getId());
                //name
                poem.setName(sourceAsMap.get("name").toString());
                if (highlightFields.containsKey("name")) {
                    poem.setName(highlightFields.get("name").fragments()[0].toString());
                }
                //作者
                poem.setAuthor(sourceAsMap.get("author").toString());
                if (highlightFields.containsKey("author")) {
                    poem.setAuthor(highlightFields.get("author").fragments()[0].toString());
                }
                //作者简介
                poem.setAuthordes(sourceAsMap.get("authordes").toString());
                if (highlightFields.containsKey("authordes")) {
                    poem.setAuthordes(highlightFields.get("authordes").fragments()[0].toString());
                }
                //分类
                poem.getCategory().setName(sourceAsMap.get("category").toString());
                //内容
                poem.setContent(sourceAsMap.get("content").toString());
                if (highlightFields.containsKey("content")) {
                    poem.setContent(highlightFields.get("content").fragments()[0].toString());
                }
                //地址
                poem.setHref(sourceAsMap.get("href").toString());
                //类型
                poem.setType(sourceAsMap.get("type").toString());

                lists.add(poem);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return lists;
    }

    //清空所有文档
    @Override
    public void deleteAll() {
        //先判断ES中是否存在文档
        long count = poemRepository.count();
        if (count>0) {//如果存在文档删除
            poemRepository.deleteAll();
        } else {
            throw new RuntimeException("当前索引中已经没有任何文档!");
        }
    }

    //重建文档
    @Override
    public void saveAll() {
        //清空所有文档
        poemRepository.deleteAll();
        
        //重新创建
        List<JpaCategory> categorys = catgoryDao.findAll();
        Map<String, String> collect = categorys.stream().collect( Collectors.toMap( new Function<JpaCategory, String>() {
            @Override
            public String apply( JpaCategory jpaCategory ) {
                return jpaCategory.getId();
            }
        }, new Function<JpaCategory, String>() {
            @Override
            public String apply( JpaCategory jpaCategory ) {
                return jpaCategory.getName();
            }
        } ) );
        //查询数据库中所有数据
        List<JpaPoem> poems = poemDAO.findAll();
        //导入ES索引库
        List<Poem> poemList = poems.stream().map( new Function<JpaPoem, Poem>() {

            @Override
            public Poem apply( JpaPoem jpaPoem ) {
                Poem poem = new Poem();
                BeanUtils.copyProperties( jpaPoem, poem );
                String categoryId = jpaPoem.getCategoryId();
                String value = collect.get( categoryId );
                Category category = poem.getCategory();
                category.setId( categoryId );
                category.setName( value );
                return poem;
            }
        } ).collect( Collectors.toList() );
        poemRepository.saveAll(poemList);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Long findTotalCounts() {
        return poemDAO.count();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<JpaPoem> findByPage(Integer page, Integer size) {
        return poemDAO.findAll( PageRequest.of( page,size ) ).getContent();
    }

}
