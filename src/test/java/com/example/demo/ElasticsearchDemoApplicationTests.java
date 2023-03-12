package com.example.demo;

import com.alibaba.fastjson.JSONObject;
import com.example.demo.config.ElasticSearchConfig;
import com.example.demo.domain.User;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class ElasticsearchDemoApplicationTests {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 创建索引, 插入文档
     * @throws IOException
     */
    @Test
    public void test001() throws IOException {
        IndexRequest indexRequest = new IndexRequest("indexrequest");
        indexRequest.id(UUID.randomUUID().toString().replaceAll("-",""));
        String jsonString = "{" +
                "\"user\":\"kimchy\"," +
                "\"postDate\":\"2023-02-01\"," +
                "\"message\":\"trying out Elasticsearch\"" +
                "}";
        User user = User.builder().username("li.lian").password("123456")
                .timestamp(System.currentTimeMillis()).description("test").build();
        indexRequest.source(JSONObject.toJSONString(user), XContentType.JSON);
        IndexResponse index = restHighLevelClient.index(indexRequest, ElasticSearchConfig.COMMON_OPTIONS);
        DocWriteResponse.Result result = index.getResult();
        if(DocWriteResponse.Result.CREATED.equals(index.getResult())){
            System.out.println("创建索引 插入文档完毕！！");
        }
    }


    /**
     * 根据文档id获取文档
     * @throws IOException
     */
    @Test
    public void test002() throws IOException {
        GetRequest getRequest = new GetRequest("indexrequest");
        getRequest.id("0e057391f66940e9977b93463ba1f429");
        GetResponse documentFields = restHighLevelClient.get(getRequest, ElasticSearchConfig.COMMON_OPTIONS);
        Map<String, Object> source = documentFields.getSource();
        System.out.println(source);
    }


    /**
     * 判断连接es是否成功
     * @throws IOException
     */
    @Test
    public void test003() throws IOException {
        try {
            if(restHighLevelClient.ping(ElasticSearchConfig.COMMON_OPTIONS)){
                System.out.println("链接成功es");
            }
        }catch (Exception e){
            if(e instanceof ElasticsearchException){
                System.out.println("ConnectException链接失败");
            }
        }
    }


    /**
     * 模糊查询
     * @throws IOException
     */
    @Test
    public void test004() throws IOException {
        //创建搜索请求。如果没有参数，这将对所有索引运行。
        SearchRequest searchRequest = new SearchRequest("indexrequest");
        //大多数搜索参数都添加到SearchSourceBuilder中。它为进入搜索请求主体的所有内容提供了setter。
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("user", "kimchy");
        matchQueryBuilder.fuzziness(Fuzziness.AUTO); //开启模糊性查询
        matchQueryBuilder.prefixLength(3); //模糊前缀
        matchQueryBuilder.maxExpansions(10); //设置最大扩展选项
//        searchSourceBuilder.query(QueryBuilders.matchQuery("text","标题2"));
        searchSourceBuilder.query(matchQueryBuilder);
        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        searchRequest.source(searchSourceBuilder);
        SearchResponse search = restHighLevelClient.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);
        SearchHit[] hits = search.getHits().getHits();
        for (SearchHit hit : hits) {
            System.out.println(hit);
        }
    }


    /**
     * 高亮查询
     * @throws IOException
     */
    @Test
    public void test005() throws IOException {
        //指定搜素请求信息
        SearchRequest searchRequest = new SearchRequest("indexrequest"); //index
        //创建搜素源生成器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //匹配
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("user", "kimchy");
        //高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        HighlightBuilder.Field highlightTitle = new HighlightBuilder.Field("user");
        highlightBuilder.preTags("<span style='color:red' >");//设置前缀
        highlightBuilder.postTags("</span>");//设置后缀
        highlightBuilder.field(highlightTitle);
        //设置高亮
        searchSourceBuilder.highlighter(highlightBuilder);
        //匹配器设置匹配规则
        searchSourceBuilder.query(matchQueryBuilder);
        //设置排序
        //searchSourceBuilder.sort("createTime");
        //设置分页
        searchSourceBuilder.from(0); //页吗
        searchSourceBuilder.size(10);//默认命中10
        searchRequest.source(searchSourceBuilder);

        SearchResponse search = restHighLevelClient.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);
        for (SearchHit hit : search.getHits().getHits()) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            System.out.println(sourceAsMap);
        }
    }

}
