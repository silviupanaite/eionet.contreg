package eionet.cr.dao.virtuoso.helpers;

import java.text.ParseException;

import junit.framework.TestCase;
import eionet.cr.common.Predicates;
import eionet.cr.dao.helpers.FreeTextSearchHelper.FilterType;
import eionet.cr.dao.util.SearchExpression;
import eionet.cr.util.SortOrder;
import eionet.cr.util.SortingRequest;
import eionet.cr.util.pagination.PagingRequest;
import eionet.cr.util.sql.VirtuosoFullTextQuery;

/**
 * VirtuosoFreetextSearchHelper tests.
 */
public class VirtuosoFreetextSearchHelperTest extends TestCase {

    /**
     * Test creating ordered query string.
     */
    public static void testNonExactAnyObject() {
        PagingRequest pagingRequest = PagingRequest.create(1);
        SortingRequest sortingRequest = new SortingRequest(null, SortOrder.parse(SortOrder.ASCENDING.toString()));
        boolean exactMatch = false;
        SearchExpression expression = new SearchExpression("water");
        VirtuosoFullTextQuery virtExpression = null;
        try {
            virtExpression = VirtuosoFullTextQuery.parse(expression);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        VirtuosoFreeTextSearchHelper helper =
                new VirtuosoFreeTextSearchHelper(expression, virtExpression, exactMatch, pagingRequest, sortingRequest);
        helper.setFilter(FilterType.ANY_OBJECT);
        String sparql = helper.getQuery(null);

        assertEquals("select distinct ?s where { ?s ?p ?o .   FILTER bif:contains(?o, ?objectValue). } limit 15 offset 0", sparql);
        assertTrue(helper.getQueryBindings().toString().indexOf("objectValue='water'") != -1);
    }

    /**
     * Test creating unordered query string.
     */
    public static void testExactAnyObject() {
        PagingRequest pagingRequest = PagingRequest.create(2);
        SortingRequest sortingRequest = new SortingRequest(null, SortOrder.parse(SortOrder.ASCENDING.toString()));
        boolean exactMatch = true;
        SearchExpression expression = new SearchExpression("water");
        VirtuosoFullTextQuery virtExpression = null;
        try {
            virtExpression = VirtuosoFullTextQuery.parse(expression);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        VirtuosoFreeTextSearchHelper helper =
                new VirtuosoFreeTextSearchHelper(expression, virtExpression, exactMatch, pagingRequest, sortingRequest);
        helper.setFilter(FilterType.ANY_OBJECT);
        String sparql = helper.getQuery(null);
        assertEquals("select distinct ?s where { ?s ?p ?o .   FILTER (?o = ?objectValue).} limit 15 offset 15", sparql);
        assertTrue(helper.getQueryBindings().toString().indexOf("objectValue=water") != -1);
    }

    /**
     * test Freetext search by any file.
     */
    public static void testAnyFile() {
        PagingRequest pagingRequest = PagingRequest.create(1);
        SortingRequest sortingRequest = new SortingRequest(null, SortOrder.parse(SortOrder.ASCENDING.toString()));
        boolean exactMatch = false;
        SearchExpression expression = new SearchExpression("report");
        VirtuosoFullTextQuery virtExpression = null;
        try {
            virtExpression = VirtuosoFullTextQuery.parse(expression);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        VirtuosoFreeTextSearchHelper helper =
                new VirtuosoFreeTextSearchHelper(expression, virtExpression, exactMatch, pagingRequest, sortingRequest);
        helper.setFilter(FilterType.ANY_FILE);
        String sparql = helper.getQuery(null);

        // assertEquals("select distinct ?s where { ?s ?p ?o .
        // ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://cr.eionet.europa.eu/ontologies/contreg.rdf#File> .
        // FILTER bif:contains(?o, \"'report'\"). } limit 15 offset 0", sparql);
        assertEquals("select distinct ?s where { ?s ?p ?o .  ?s a ?subjectType .  FILTER bif:contains(?o, ?objectValue). } "
                + "limit 15 offset 0", sparql);

        assertTrue(helper.getQueryBindings().toString()
                .indexOf("subjectType=http://cr.eionet.europa.eu/ontologies/contreg.rdf#File") != -1);
        assertTrue(helper.getQueryBindings().toString().indexOf("objectValue='report") != -1);
    }

    /**
     * test freetext sort by label.
     */
    public static void testSortedQueryLabel() {
        PagingRequest pagingRequest = PagingRequest.create(1);
        SortingRequest sortingRequest =
                new SortingRequest("http://www.w3.org/2000/01/rdf-schema#label", SortOrder.parse(SortOrder.ASCENDING.toString()));
        boolean exactMatch = false;
        SearchExpression expression = new SearchExpression("ippc");
        VirtuosoFullTextQuery virtExpression = null;
        try {
            virtExpression = VirtuosoFullTextQuery.parse(expression);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        VirtuosoFreeTextSearchHelper helper =
                new VirtuosoFreeTextSearchHelper(expression, virtExpression, exactMatch, pagingRequest, sortingRequest);
        helper.setFilter(FilterType.ANY_OBJECT);
        String sparql = helper.getQuery(null);

        // assertEquals("select distinct ?s where {?s ?p ?o . FILTER bif:contains(?o, \"'ippc'\").
        // optional {?s <http://www.w3.org/2000/01/rdf-schema#label> ?ord} }ORDER BY asc(bif:lcase(bif:either(bif:isnull(?ord),
        // (bif:subseq (bif:replace (?s, '/', '#'), bif:strrchr (bif:replace (?s, '/', '#'), '#')+1)), ?ord)))
        // limit 15 offset 0", sparql);
        assertEquals("select distinct ?s where {?s ?p ?o .   FILTER bif:contains(?o, ?objectValue). "
                + "optional {?s ?sortPredicate ?ord} }ORDER BY asc(bif:lcase(bif:either(bif:isnull(?ord), "
                + "(bif:subseq (bif:replace (?s, '/', '#'), "
                + "bif:strrchr (bif:replace (?s, '/', '#'), '#')+1)), ?ord))) limit 15 offset 0", sparql);
        assertTrue(helper.getQueryBindings().toString().indexOf("objectValue='ippc'") != -1);
        assertTrue(helper.getQueryBindings().toString().indexOf("sortPredicate=http://www.w3.org/2000/01/rdf-schema#label") != -1);
    }

    /**
     * test if sort by last modified works.
     */
    public static void testSortedLastModified() {
        PagingRequest pagingRequest = PagingRequest.create(0);
        SortingRequest sortingRequest =
                new SortingRequest(Predicates.CR_LAST_MODIFIED, SortOrder.parse(SortOrder.DESCENDING.toString()));
        boolean exactMatch = true;
        SearchExpression expression = new SearchExpression("ippc");
        VirtuosoFullTextQuery virtExpression = null;
        try {
            virtExpression = VirtuosoFullTextQuery.parse(expression);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        VirtuosoFreeTextSearchHelper helper =
                new VirtuosoFreeTextSearchHelper(expression, virtExpression, exactMatch, pagingRequest, sortingRequest);
        helper.setFilter(FilterType.ANY_OBJECT);
        String sparql = helper.getQuery(null);

        // assertEquals("select ?s where { graph ?g {select ?s max(?time) AS ?order where
        // {?s ?p ?o . FILTER (?o = 'ippc'). optional
        // {?g <http://cr.eionet.europa.eu/ontologies/contreg.rdf#contentLastModified> ?time} } GROUP BY ?s }}
        // ORDER BY desc(?order) limit 15 offset 0", sparql);
        assertEquals("select ?s where { graph ?g {select ?s max(?time) AS ?order where {?s ?p ?o ."
                + "  FILTER (?o = ?objectValue). optional {?g "
                + "<http://cr.eionet.europa.eu/ontologies/contreg.rdf#contentLastModified> ?time} } GROUP BY ?s }} "
                + "ORDER BY desc(?order) limit 15 offset 0", sparql);
        assertTrue(helper.getQueryBindings().toString().indexOf("objectValue=ippc") != -1);
    }

    /**
     * test exact match search. Search expression is a URI.
     */
    public static void testExactUri() {
        PagingRequest pagingRequest = PagingRequest.create(2);
        SortingRequest sortingRequest = new SortingRequest(null, SortOrder.parse(SortOrder.ASCENDING.toString()));
        boolean exactMatch = true;
        SearchExpression expression = new SearchExpression("http://uri.com");
        VirtuosoFullTextQuery virtExpression = null;
        try {
            virtExpression = VirtuosoFullTextQuery.parse(expression);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        VirtuosoFreeTextSearchHelper helper =
                new VirtuosoFreeTextSearchHelper(expression, virtExpression, exactMatch, pagingRequest, sortingRequest);
        helper.setFilter(FilterType.ANY_OBJECT);
        String sparql = helper.getQuery(null);
        // assertEquals("select distinct ?s where { ?s ?p ?o . FILTER (?o = <http://uri.com> || ?o = 'http://uri.com').}
        // limit 15 offset 15", sparql);
        assertEquals("select distinct ?s where { ?s ?p ?o .   "
                + "FILTER (?o = ?objectValueUri || ?o = ?objectValueLit).} limit 15 offset 15", sparql);
        assertTrue(helper.getQueryBindings().toString().indexOf("objectValueUri=http://uri.com") != -1);
        assertTrue(helper.getQueryBindings().toString().indexOf("objectValueLit=http://uri.com") != -1);
    }
}
