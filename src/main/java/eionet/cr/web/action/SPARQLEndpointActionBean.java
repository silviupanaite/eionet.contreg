package eionet.cr.web.action;

import java.io.IOException;
import java.io.OutputStreamWriter;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.openrdf.OpenRDFException;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.rdfxml.RDFXMLWriter;

import virtuoso.sesame2.driver.VirtuosoRepository;

import eionet.cr.config.GeneralConfig;
import eionet.cr.web.sparqlClient.helpers.CRJsonWriter;
import eionet.cr.web.sparqlClient.helpers.CRXmlWriter;
import eionet.cr.web.sparqlClient.helpers.QueryResult;

/**
 * 
 * @author altnyris
 * 
 */
@UrlBinding("/sparql")
public class SPARQLEndpointActionBean extends AbstractActionBean {

    private static final String FORMAT_XML = "xml";
    private static final String FORMAT_JSON = "json";
    private static final String FORMAT_HTML = "html";

    /** */
    private static final String FORM_PAGE = "/pages/sparqlClient.jsp";

    private static List<String> xmlFormats = new ArrayList<String>();

    static {
        xmlFormats.add("application/sparql-results+xml");
        xmlFormats.add("application/rdf+xml");
        xmlFormats.add("application/xml");
        xmlFormats.add("text/xml");
        xmlFormats.add("application/x-binary-rdf-results-table");
        xmlFormats.add("text/boolean"); // For ASK query
    }

    /** */
    private String query;
    private String newQuery;
    private String format;
    private String inputMode;
    private int nrOfHits;
    private long executionTime;

    private boolean useInferencing;
    boolean isAskQuery = false;
    boolean isConstructQuery = false;

    /** */
    private QueryResult result;
    private String resultAsk;

    /**
     * 
     * @return Resolution
     * @throws OpenRDFException
     */
    @DefaultHandler
    public Resolution execute() throws OpenRDFException {

        String acceptHeader = getContext().getRequest().getHeader("accept");
        String[] accept = {null};
        if (acceptHeader != null && acceptHeader.length() > 0) {
            accept = acceptHeader.split(",");
            if (accept != null && accept.length > 0) {
                accept = accept[0].split(";");
            }
        }

        // Check if ASK query
        if ((accept != null && accept[0].equals("text/boolean")) || (inputMode != null && inputMode.equals("ASK"))) {
            isAskQuery = true;
        }

        // Check if CONSTRUCT query
        if ((accept != null && accept[0].equals("application/x-trig"))
                || (query != null && (query.contains("construct") || query.contains("CONSTRUCT")))) {
            isConstructQuery = true;
            setInputMode("CONSTRUCT");
        }

        // If CONSTRUCT query, but output format is HTML then evaluate as simple SELECT query
        if (isConstructQuery && format != null && format.equals("text/html")) {
            isConstructQuery = false;
        }

        if (!StringUtils.isBlank(format)) {
            accept[0] = format;
        }

        if (nrOfHits == 0) {
            nrOfHits = 20;
        }

        // If user has marked CR Inferencing checkbox,
        // then add inferencing command to the query
        newQuery = query;
        query = StringEscapeUtils.escapeHtml(query);
        if (useInferencing && !StringUtils.isBlank(query)) {
            String infCommand = "DEFINE input:inference '" + GeneralConfig.getProperty(GeneralConfig.VIRTUOSO_CR_RULESET_NAME)
                    + "'";
            newQuery = infCommand + "\n" + newQuery;
        }

        if (isConstructQuery) {
            return new StreamingResolution("application/rdf+xml") {
                public void stream(HttpServletResponse response) throws Exception {
                    runQuery(newQuery, FORMAT_XML, response.getOutputStream());
                }
            };
        } else if (accept != null && xmlFormats.contains(accept[0])) {
            return new StreamingResolution("application/sparql-results+xml") {
                public void stream(HttpServletResponse response) throws Exception {
                    runQuery(newQuery, FORMAT_XML, response.getOutputStream());
                }
            };
        } else if (accept != null && accept[0].equals("application/sparql-results+json")) {
            return new StreamingResolution("application/sparql-results+json") {
                public void stream(HttpServletResponse response) throws Exception {
                    runQuery(newQuery, FORMAT_JSON, response.getOutputStream());
                }
            };
        } else {
            if (!StringUtils.isBlank(query)) {
                runQuery(newQuery, FORMAT_HTML, null);
            }
            return new ForwardResolution(FORM_PAGE);
        }
    }

    private void runQuery(String query, String format, OutputStream out) {

        if (!StringUtils.isBlank(query)) {
            String url = GeneralConfig.getProperty(GeneralConfig.VIRTUOSO_DB_URL);
            String username = GeneralConfig.getProperty(GeneralConfig.VIRTUOSO_DB_ROUSR);
            String password = GeneralConfig.getProperty(GeneralConfig.VIRTUOSO_DB_ROPWD);

            try {

                Repository myRepository = new VirtuosoRepository(url, username, password);
                myRepository.initialize();
                RepositoryConnection con = myRepository.getConnection();

                try {
                    // Evaluate ASK query
                    if (isAskQuery) {
                        BooleanQuery resultsTableBoolean = con.prepareBooleanQuery(QueryLanguage.SPARQL, query);
                        Boolean result = resultsTableBoolean.evaluate();

                        // ASK query in XML format
                        if (format != null && format.equals(FORMAT_XML)) {
                            OutputStreamWriter writer = new OutputStreamWriter(out);
                            writer.write("<?xml version=\"1.0\"?>");
                            writer.write("<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">");
                            writer.write("<head></head>");
                            writer.write("<boolean>");
                            writer.write(result.toString());
                            writer.write("</boolean>");
                            writer.write("</sparql>");
                            writer.flush();
                            // ASK query in JSON format
                        } else if (format != null && format.equals(FORMAT_JSON)) {
                            OutputStreamWriter writer = new OutputStreamWriter(out);
                            writer.write("{  \"head\": { \"link\": [] }, \"boolean\": ");
                            writer.write(result.toString());
                            writer.write("}");
                            writer.flush();
                            // ASK query in HTML format
                        } else if (format != null && format.equals(FORMAT_HTML)) {
                            resultAsk = result.toString();
                        }
                        // Evaluate CONSTRUCT query. Returns XML format
                    } else if (isConstructQuery && !format.equals(FORMAT_HTML)) {
                        GraphQuery resultsTable = con.prepareGraphQuery(QueryLanguage.SPARQL, query);
                        RDFXMLWriter writer = new RDFXMLWriter(out);
                        resultsTable.evaluate(writer);
                        // Evaluate SELECT query
                    } else {
                        TupleQuery resultsTable = con.prepareTupleQuery(QueryLanguage.SPARQL, query);

                        // Returns XML format
                        if (format != null && format.equals(FORMAT_XML)) {
                            CRXmlWriter sparqlWriter = new CRXmlWriter(out);
                            resultsTable.evaluate(sparqlWriter);
                            // Returns JSON format
                        } else if (format != null && format.equals(FORMAT_JSON)) {
                            CRJsonWriter sparqlWriter = new CRJsonWriter(out);
                            resultsTable.evaluate(sparqlWriter);
                            // Returns HTML format
                        } else if (format != null && format.equals(FORMAT_HTML)) {
                            long startTime = System.currentTimeMillis();
                            TupleQueryResult bindings = resultsTable.evaluate();
                            executionTime = System.currentTimeMillis() - startTime;
                            if (bindings != null) {
                                result = new QueryResult(bindings);
                            }
                        }
                    }
                } finally {
                    con.close();
                }
            } catch (RepositoryException rex) {
                rex.printStackTrace();
                addWarningMessage("Repository exception: '" + StringEscapeUtils.escapeHtml(rex.toString()) + "'");
                // throw new RuntimeException(rex.toString(), rex);
            } catch (Exception e) {
                e.printStackTrace();
                addWarningMessage("Error processing SPARQL: '" + StringEscapeUtils.escapeHtml(e.toString()) + "'");
                // throw new RuntimeException(e.toString(), e);
            } finally {
                try {
                    if (out != null)
                        out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query
     *            the query to set
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * @return the result
     */
    public QueryResult getResult() {
        return result;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getNrOfHits() {
        return nrOfHits;
    }

    public void setNrOfHits(int nrOfHits) {
        this.nrOfHits = nrOfHits;
    }

    public boolean isUseInferencing() {
        return useInferencing;
    }

    public void setUseInferencing(boolean useInferencing) {
        this.useInferencing = useInferencing;
    }

    public String getInputMode() {
        return inputMode;
    }

    public void setInputMode(String inputMode) {
        this.inputMode = inputMode;
    }

    public String getResultAsk() {
        return resultAsk;
    }

    public void setResultAsk(String resultAsk) {
        this.resultAsk = resultAsk;
    }

}
