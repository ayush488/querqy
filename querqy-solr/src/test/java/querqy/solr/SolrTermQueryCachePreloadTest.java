package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.BeforeClass;
import org.junit.Test;

public class SolrTermQueryCachePreloadTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty("tests.codec", "Lucene46");
        initCore("solrconfig-cache-preloaded.xml", "schema.xml");
    }
     
    @Test
    public void testThatCacheIsAvailableAndPrefilledAndNotUpdated() throws Exception {
         
         SolrQueryRequest req = req(
               CommonParams.QT, "/admin/mbeans",
               "cat", "CACHE",
               "stats", "true"
               );
         assertQ("Missing querqy cache",
               req,
               "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']");
         // only one generated term in one field is preloaded for firstSearcher:
         assertQ("Querqy cache not prefilled",
                 req,
               "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']"
                       + "/lst[@name='stats']/long[@name='size'][text()='1']");

         req.close();
         
         assertU(adoc("id", "1", "f1", "a"));
         assertU(commit());
         
         SolrQueryRequest req2 = req(
                 CommonParams.QT, "/admin/mbeans",
                 "cat", "CACHE",
                 "stats", "true"
                 );
         
         // one generated term in two field is preloaded for newSearcher:
         assertQ("Querqy cache not prefilled",
                 req2,
                 "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']"
                         + "/lst[@name='stats']/long[@name='size'][text()='2']");

         req2.close();
         
         String q = "a b c";
         SolrQueryRequest req3 = req(
                 
                 CommonParams.Q, q,
                 DisMaxParams.QF, "f1 f2",
                 QueryParsing.OP, "OR",
                 "defType", "querqy"
                 );
         
         assertQ("Could not execute query",
                 req3);

         req3.close();
         
         
         SolrQueryRequest reqStats = req(
                 CommonParams.QT, "/admin/mbeans",
                 "cat", "CACHE",
                 "stats", "true"
                 );
         
         assertQ("Querqy cache was updated unexpectedly",
                 reqStats,
                 "//lst[@name='CACHE']/lst[@name='querqyTermQueryCache']"
                         + "/lst[@name='stats']/long[@name='size'][text()='2']");

         reqStats.close();
         
         
         
    }
   
}