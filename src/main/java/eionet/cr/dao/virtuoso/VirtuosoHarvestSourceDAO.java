package eionet.cr.dao.virtuoso;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

import eionet.cr.config.GeneralConfig;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.HarvestSourceDAO;
import eionet.cr.dao.readers.HarvestSourceDTOReader;
import eionet.cr.dao.readers.SubjectReader;
import eionet.cr.dto.HarvestSourceDTO;
import eionet.cr.dto.ObjectDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.harvest.statistics.dto.HarvestUrgencyScoreDTO;
import eionet.cr.harvest.statistics.dto.HarvestedUrlCountDTO;
import eionet.cr.util.Bindings;
import eionet.cr.util.Hashes;
import eionet.cr.util.Pair;
import eionet.cr.util.SortingRequest;
import eionet.cr.util.URLUtil;
import eionet.cr.util.Util;
import eionet.cr.util.YesNoBoolean;
import eionet.cr.util.pagination.PagingRequest;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.sql.SQLUtil;
import eionet.cr.util.sql.SingleObjectReader;

/**
 * Harvester methods in Virtuoso.
 *
 * @author kaido
 */

// TODO remove extending when all the methods have been copied to VirtuosoDAO
public class VirtuosoHarvestSourceDAO extends VirtuosoBaseDAO implements HarvestSourceDAO {

    private static final String GET_SOURCES_SQL =
        "SELECT<pagingParams> * FROM HARVEST_SOURCE WHERE URL NOT IN (SELECT URL FROM REMOVE_SOURCE_QUEUE) ";
    private static final String SEARCH_SOURCES_SQL =
        "SELECT<pagingParams> * FROM HARVEST_SOURCE WHERE URL like (?) AND URL NOT IN (SELECT URL FROM REMOVE_SOURCE_QUEUE) ";
    private static final String getHarvestSourcesFailedSQL =
        "SELECT<pagingParams> * FROM HARVEST_SOURCE WHERE LAST_HARVEST_FAILED = 'Y' AND URL NOT IN (SELECT URL FROM REMOVE_SOURCE_QUEUE)";
    private static final String searchHarvestSourcesFailedSQL =
        "SELECT<pagingParams> * FROM HARVEST_SOURCE WHERE LAST_HARVEST_FAILED = 'Y' AND URL LIKE(?) AND"
        + " URL NOT IN (SELECT URL FROM REMOVE_SOURCE_QUEUE)";
    private static final String getHarvestSourcesUnavailableSQL = "SELECT * FROM HARVEST_SOURCE WHERE COUNT_UNAVAIL > "
        + HarvestSourceDTO.COUNT_UNAVAIL_THRESHOLD + " AND URL NOT IN (SELECT URL FROM REMOVE_SOURCE_QUEUE)";
    private static final String searchHarvestSourcesUnavailableSQL =
        "SELECT<pagingParams> * FROM HARVEST_SOURCE WHERE URL LIKE (?) AND COUNT_UNAVAIL > " + HarvestSourceDTO.COUNT_UNAVAIL_THRESHOLD
        + " AND URL NOT IN (SELECT URL FROM REMOVE_SOURCE_QUEUE)";
    private static final String getPrioritySources =
        "SELECT<pagingParams> * FROM HARVEST_SOURCE WHERE PRIORITY_SOURCE = 'Y' AND URL NOT IN (SELECT URL FROM REMOVE_SOURCE_QUEUE)  ";
    private static final String searchPrioritySources =
        "SELECT<pagingParams> * FROM HARVEST_SOURCE WHERE PRIORITY_SOURCE = 'Y' and URL like(?) AND "
        + "URL NOT IN (SELECT URL FROM REMOVE_SOURCE_QUEUE) ";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#getHarvestSources(java.lang.String, eionet.cr.util.PagingRequest,
     * eionet.cr.util.SortingRequest)
     */
    @Override
    public Pair<Integer, List<HarvestSourceDTO>> getHarvestSources(String searchString, PagingRequest pagingRequest,
            SortingRequest sortingRequest) throws DAOException {

        return getSources(StringUtils.isBlank(searchString) ? GET_SOURCES_SQL : SEARCH_SOURCES_SQL, searchString, pagingRequest,
                sortingRequest);
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#getHarvestSourcesFailed(java.lang.String, eionet.cr.util.PagingRequest,
     * eionet.cr.util.SortingRequest)
     */
    @Override
    public Pair<Integer, List<HarvestSourceDTO>> getHarvestSourcesFailed(String searchString, PagingRequest pagingRequest,
            SortingRequest sortingRequest) throws DAOException {

        return getSources(StringUtils.isBlank(searchString) ? getHarvestSourcesFailedSQL : searchHarvestSourcesFailedSQL,
                searchString, pagingRequest, sortingRequest);
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#getHarvestSourcesUnavailable(java.lang .String, eionet.cr.util.PagingRequest,
     * eionet.cr.util.SortingRequest)
     */
    @Override
    public Pair<Integer, List<HarvestSourceDTO>> getHarvestSourcesUnavailable(String searchString, PagingRequest pagingRequest,
            SortingRequest sortingRequest) throws DAOException {

        return getSources(
                StringUtils.isBlank(searchString) ? getHarvestSourcesUnavailableSQL : searchHarvestSourcesUnavailableSQL,
                        searchString, pagingRequest, sortingRequest);
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#getPrioritySources(java.lang.String, eionet.cr.util.PagingRequest,
     * eionet.cr.util.SortingRequest)
     */
    @Override
    public Pair<Integer, List<HarvestSourceDTO>> getPrioritySources(String searchString, PagingRequest pagingRequest,
            SortingRequest sortingRequest) throws DAOException {

        return getSources(StringUtils.isBlank(searchString) ? getPrioritySources : searchPrioritySources, searchString,
                pagingRequest, sortingRequest);
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#getInferenceSources(java.lang.String, eionet.cr.util.PagingRequest,
     * eionet.cr.util.SortingRequest)
     */
    @Override
    public Pair<Integer, List<HarvestSourceDTO>> getInferenceSources(String searchString, PagingRequest pagingRequest,
            SortingRequest sortingRequest, String sourceUris) throws DAOException {

        if (StringUtils.isBlank(sourceUris)) {
            return new Pair<Integer, List<HarvestSourceDTO>>(0, new ArrayList<HarvestSourceDTO>());
        }

        String query =
            "SELECT<pagingParams> * FROM HARVEST_SOURCE WHERE URL IN (<sources>) AND URL NOT IN (SELECT URL FROM REMOVE_SOURCE_QUEUE) ";
        if (!StringUtils.isBlank(searchString)) {
            query =
                "SELECT<pagingParams> * FROM HARVEST_SOURCE WHERE URL like (?) AND URL IN (<sources>) AND "
                + " URL NOT IN (SELECT URL FROM REMOVE_SOURCE_QUEUE) ";
        }
        query = query.replace("<sources>", sourceUris);

        return getSources(query, searchString, pagingRequest, sortingRequest);
    }

    /**
     *
     * @param sql
     * @param searchString
     * @param pagingRequest
     * @param sortingRequest
     * @return
     * @throws DAOException
     */
    private Pair<Integer, List<HarvestSourceDTO>> getSources(String sql, String searchString, PagingRequest pagingRequest,
            SortingRequest sortingRequest) throws DAOException {

        List<Object> inParams = new LinkedList<Object>();
        if (!StringUtils.isBlank(searchString)) {
            inParams.add(searchString);
        }
        String queryWithoutOrderAndLimit = sql.replace("<pagingParams>", "");
        List<Object> inParamsWithoutOrderAndLimit = new LinkedList<Object>(inParams);

        if (sortingRequest != null && sortingRequest.getSortingColumnName() != null) {
            sql += " ORDER BY " + sortingRequest.getSortingColumnName() + " " + sortingRequest.getSortOrder().toSQL();
        } else {
            sql += " ORDER BY URL ";
        }

        if (pagingRequest != null) {
            String pp = " TOP " + pagingRequest.getOffset() + ", " + pagingRequest.getItemsPerPage();
            sql = sql.replace("<pagingParams>", pp);
        } else {
            sql = sql.replace("<pagingParams>", "");
        }

        int rowCount = 0;
        List<HarvestSourceDTO> list = executeSQL(sql, inParams, new HarvestSourceDTOReader());
        if (list != null && !list.isEmpty()) {

            StringBuffer buf = new StringBuffer("select count(*) from (").append(queryWithoutOrderAndLimit).append(") as foo");

            rowCount =
                Integer.parseInt(executeUniqueResultSQL(buf.toString(), inParamsWithoutOrderAndLimit,
                        new SingleObjectReader<Object>()).toString());
        }

        return new Pair<Integer, List<HarvestSourceDTO>>(rowCount, list);
    }

    /**
     * Calculation of number of sources needed to be harvested in VirtuosoSQL syntax.
     */
    private static final String URGENCY_SOURCES_COUNT = "select count(*) from HARVEST_SOURCE where"
        + " INTERVAL_MINUTES > 0 AND -datediff('second', now(), coalesce(LAST_HARVEST,"
        + " dateadd('minute', -INTERVAL_MINUTES, TIME_CREATED))) / (INTERVAL_MINUTES*60) >= 1.0";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#getUrgencySourcesCount()
     */
    @Override
    public Long getUrgencySourcesCount() throws DAOException {

        Connection conn = null;
        try {
            conn = getSQLConnection();
            Object o = SQLUtil.executeSingleReturnValueQuery(URGENCY_SOURCES_COUNT, conn);
            return o == null ? new Long(0) : Long.valueOf(o.toString());
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /**
     * Insert a record into the the table of harvest sources in VirtuosoSQL syntax.
     * INSERT SOFT means that if such source already exists then don't insert (like MySQL INSERT IGNORE)
     */
    private static final String ADD_SOURCE_SQL = "insert soft HARVEST_SOURCE"
        + " (URL,URL_HASH,EMAILS,TIME_CREATED,INTERVAL_MINUTES,PRIORITY_SOURCE,SOURCE_OWNER)"
        + " VALUES (?,?,?,NOW(),?,?,?)";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#addSource(HarvestSourceDTO source)
     */
    @Override
    public Integer addSource(HarvestSourceDTO source) throws DAOException {

        if (source == null) {
            throw new IllegalArgumentException("harvest source must not be null");
        }

        if (StringUtils.isBlank(source.getUrl())) {
            throw new IllegalArgumentException("url must not be null");
        }

        // harvest sources where URL has fragment part, are not allowed
        String url = StringUtils.substringBefore(source.getUrl(), "#");
        long urlHash = Hashes.spoHash(url);

        List<Object> values = new ArrayList<Object>();
        values.add(url);
        values.add(Long.valueOf(urlHash));
        values.add(source.getEmails());
        values.add(Integer.valueOf(source.getIntervalMinutes()));
        values.add(YesNoBoolean.format(source.isPrioritySource()));
        if (source.getOwner() == null || source.getOwner().length() == 0) {
            values.add("harvester");
        } else {
            values.add(source.getOwner());
        }

        Connection conn = null;
        try {
            // execute the insert statement
            conn = getSQLConnection();
            int o = SQLUtil.executeUpdateReturnAutoID(ADD_SOURCE_SQL, values, conn);
            return new Integer(o);
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#addSourceIgnoreDuplicate(HarvestSourceDTO source)
     */
    @Override
    public void addSourceIgnoreDuplicate(HarvestSourceDTO source) throws DAOException {
        addSource(source);
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#deleteHarvestHistory(int)
     */
    @Override
    public void deleteHarvestHistory(int neededToRemain) throws DAOException {

        Connection conn = null;
        try {
            conn = getSQLConnection();
            conn.setAutoCommit(false);

            Object o = SQLUtil.executeSingleReturnValueQuery("select max(HARVEST_ID) from HARVEST", conn);
            Long maxId = o == null || StringUtils.isBlank(o.toString()) ? 0L : Long.valueOf(o.toString());

            if (maxId > neededToRemain) {
                SQLUtil.executeUpdate("delete from HARVEST where HARVEST_ID<=" + (maxId - neededToRemain), conn);
            }

            SQLUtil.executeUpdate("delete from HARVEST_MESSAGE where HARVEST_ID not in (select HARVEST_ID from HARVEST)", conn);

            conn.commit();
        } catch (SQLException e) {
            SQLUtil.rollback(conn);
            throw new DAOException(e.toString(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc) helper "temporary" method to be called from Virtuoso API
     *
     * @see eionet.cr.dao.HarvestSourceDAO#deleteSourceByUrl(java.lang.String)
     */
    public void deleteSourceByUrl(String url, Connection conn) throws DAOException {

        ArrayList<Long> hashList = new ArrayList<Long>();
        hashList.add(Hashes.spoHash(url));
        ArrayList<String> urlList = new ArrayList<String>();
        urlList.add(url);

        try {
            // get ID of the harvest source identified by the given URL
            List<Long> sourceIds =
                executeSQL("select HARVEST_SOURCE_ID from HARVEST_SOURCE where URL_HASH = ?", hashList,
                        new SingleObjectReader<Long>());
            String harvestSourceIdsCSV = Util.toCSV(sourceIds);

            // if harvest source ID found, delete harvests and harvest messages
            // by it
            if (!StringUtils.isBlank(harvestSourceIdsCSV)) {

                List<Long> harvestIds =
                    executeSQL("select HARVEST_ID from HARVEST where HARVEST_SOURCE_ID in (" + harvestSourceIdsCSV + ")",
                            null, new SingleObjectReader<Long>());

                String harvestIdsCSV = Util.toCSV(harvestIds);
                if (!StringUtils.isBlank(harvestIdsCSV)) {
                    SQLUtil.executeUpdate("delete from HARVEST_MESSAGE where HARVEST_ID in (" + harvestIdsCSV + ")", conn);
                }
                SQLUtil.executeUpdate("delete from HARVEST where HARVEST_SOURCE_ID in (" + harvestSourceIdsCSV + ")", conn);
            }

            // delete dependencies of this harvest source in other tables
            SQLUtil.executeUpdate("delete from HARVEST_SOURCE where URL_HASH=?", hashList, conn);
            SQLUtil.executeUpdate("delete from HARVEST_SOURCE where SOURCE=?", hashList, conn);
            SQLUtil.executeUpdate("delete from URGENT_HARVEST_QUEUE where URL=?", urlList, conn);
            SQLUtil.executeUpdate("delete from REMOVE_SOURCE_QUEUE where URL=?", urlList, conn);

            // end transaction
        } catch (Exception e) {
            throw new DAOException("Error deleting source " + url, e);
        }

    }

    /** */
    private static final String EDIT_SOURCE_SQL = "update HARVEST_SOURCE set URL=?, URL_HASH=?, EMAILS=?, INTERVAL_MINUTES=?,"
        + " PRIORITY_SOURCE=?, SOURCE_OWNER=?, MEDIA_TYPE=? where HARVEST_SOURCE_ID=?";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#editSource(eionet.cr.dto.HarvestSourceDTO)
     */
    @Override
    public void editSource(HarvestSourceDTO source) throws DAOException {

        List<Object> values = new ArrayList<Object>();
        values.add(source.getUrl());
        values.add(Long.valueOf(source.getUrl() == null ? 0 : Hashes.spoHash(source.getUrl())));
        values.add(source.getEmails());
        values.add(source.getIntervalMinutes());
        values.add(YesNoBoolean.format(source.isPrioritySource()));
        values.add(source.getOwner());
        values.add(source.getMediaType());
        values.add(source.getSourceId());

        Connection conn = null;
        try {
            conn = getSQLConnection();
            SQLUtil.executeUpdate(EDIT_SOURCE_SQL, values, conn);
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /** */
    private static final String getSourcesByIdSQL = "select * from HARVEST_SOURCE where HARVEST_SOURCE_ID=?";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#getHarvestSourceById(java.lang.Integer)
     */
    @Override
    public HarvestSourceDTO getHarvestSourceById(Integer harvestSourceID) throws DAOException {

        List<Object> values = new ArrayList<Object>();
        values.add(harvestSourceID);
        List<HarvestSourceDTO> list = executeSQL(getSourcesByIdSQL, values, new HarvestSourceDTOReader());
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    /** */
    private static final String getSourcesByUrlSQL = "select * from HARVEST_SOURCE where URL_HASH=?";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#getHarvestSourceByUrl(java.lang.String)
     */
    @Override
    public HarvestSourceDTO getHarvestSourceByUrl(String url) throws DAOException {

        List<Object> values = new ArrayList<Object>();
        values.add(Long.valueOf(Hashes.spoHash(url)));
        List<HarvestSourceDTO> list = executeSQL(getSourcesByUrlSQL, values, new HarvestSourceDTOReader());
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    /** */
    private static final String GET_NEXT_SCHEDULED_SOURCES_SQL =

        "select<limParam> * from CR.cr3user.HARVEST_SOURCE where"
        + " PERMANENT_ERROR = 'N' and COUNT_UNAVAIL < 5 and INTERVAL_MINUTES > 0"
        + " AND -datediff('second', now(), coalesce(LAST_HARVEST, dateadd('minute', -INTERVAL_MINUTES, TIME_CREATED)))"
        + " / (INTERVAL_MINUTES*60)order by -datediff('second', now(), coalesce(LAST_HARVEST,"
        + " dateadd('minute', -INTERVAL_MINUTES, TIME_CREATED))) / (INTERVAL_MINUTES*60) desc";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.harvest.scheduled.HarvestingJob#getNextScheduledSources()
     *
     * @see eionet.cr.dao.HarvestSourceDAO#getNextScheduledSources(int)
     */
    @Override
    public List<HarvestSourceDTO> getNextScheduledSources(int limit) throws DAOException {

        List<Object> values = new ArrayList<Object>();
        String query = GET_NEXT_SCHEDULED_SOURCES_SQL.replace("<limParam>", new Integer(limit).toString());
        return executeSQL(query, values, new HarvestSourceDTOReader());
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#getScheduledForDeletion()
     */
    @Override
    public List<String> getScheduledForDeletion() throws DAOException {

        return executeSQL("select URL from REMOVE_SOURCE_QUEUE", null, new SingleObjectReader<String>());
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#queueSourcesForDeletion(java.util.List)
     */
    @Override
    public void queueSourcesForDeletion(List<String> urls) throws DAOException {

        if (urls == null || urls.isEmpty()) {
            return;
        }
        StringBuffer sql = new StringBuffer("INSERT SOFT REMOVE_SOURCE_QUEUE (URL) VALUES ");
        List<Object> params = new LinkedList<Object>();
        int i = 0;
        for (String url : urls) {
            sql.append("(?)");
            if (++i < urls.size()) {
                sql.append(',');
            }
            params.add(url);
        }
        executeSQL(sql.toString(), params);
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#increaseUnavailableCount(int)
     */
    @Override
    public void increaseUnavailableCount(int sourceId) throws DAOException {
        Connection conn = null;
        String query = "update HARVEST_SOURCE set COUNT_UNAVAIL=(COUNT_UNAVAIL+1) where HARVEST_SOURCE_ID=" + sourceId;
        try {
            conn = getSQLConnection();
            SQLUtil.executeUpdate(query, conn);
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getLatestHarvestedURLs()
     */
    @Override
    public Pair<Integer, List<HarvestedUrlCountDTO>> getLatestHarvestedURLs(int days) throws DAOException {

        StringBuffer buf =
            new StringBuffer().append("select left (datestring(LAST_HARVEST), 10) AS HARVESTDAY,")
            .append(" COUNT(HARVEST_SOURCE_ID) AS HARVESTS FROM CR.cr3user.HARVEST_SOURCE where")
            .append(" LAST_HARVEST IS NOT NULL AND dateadd('day', " + days + ", LAST_HARVEST) > now()")
            .append(" GROUP BY (HARVESTDAY) ORDER BY HARVESTDAY DESC");

        List<HarvestedUrlCountDTO> result = new ArrayList<HarvestedUrlCountDTO>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            conn = getSQLConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(buf.toString());
            while (rs.next()) {
                HarvestedUrlCountDTO resultRow = new HarvestedUrlCountDTO();
                try {
                    resultRow.setHarvestDay(sdf.parse(rs.getString("HARVESTDAY")));
                } catch (ParseException ex) {
                    throw new DAOException(ex.toString(), ex);
                }
                resultRow.setHarvestCount(rs.getLong("HARVESTS"));
                result.add(resultRow);
            }
        } catch (SQLException e) {
            throw new DAOException(e.toString(), e);
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(stmt);
            SQLUtil.close(conn);
        }

        return new Pair<Integer, List<HarvestedUrlCountDTO>>(result.size(), result);
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getUrgencyOfComingHarvests()
     */
    @Override
    public Pair<Integer, List<HarvestUrgencyScoreDTO>> getUrgencyOfComingHarvests(int amount) throws DAOException {

        StringBuffer buf =
            new StringBuffer().append("select top " + amount + " url, last_harvest, interval_minutes,")
            .append(" (-datediff('second', now(), coalesce(LAST_HARVEST, dateadd('minute', -INTERVAL_MINUTES, TIME_CREATED)))")
            .append(" / (INTERVAL_MINUTES*60)) AS urgency")
            .append(" from CR.cr3user.HARVEST_SOURCE")
            .append(" where interval_minutes > 0 ORDER BY urgency DESC");

        List<HarvestUrgencyScoreDTO> result = new ArrayList<HarvestUrgencyScoreDTO>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

            conn = getSQLConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(buf.toString());
            while (rs.next()) {
                HarvestUrgencyScoreDTO resultRow = new HarvestUrgencyScoreDTO();
                resultRow.setUrl(rs.getString("url"));
                try {
                    resultRow.setLastHarvest(sdf.parse(rs.getString("last_harvest") + ""));
                } catch (ParseException ex) {
                    resultRow.setLastHarvest(null);
                    // throw new DAOException(ex.toString(), ex);
                }
                resultRow.setIntervalMinutes(rs.getLong("interval_minutes"));
                resultRow.setUrgency(rs.getDouble("urgency"));
                result.add(resultRow);
            }
        } catch (SQLException e) {
            throw new DAOException(e.toString(), e);
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(stmt);
            SQLUtil.close(conn);
        }

        return new Pair<Integer, List<HarvestUrgencyScoreDTO>>(result.size(), result);

    }

    /** */
    private static final String UPDATE_SOURCE_HARVEST_FINISHED_SQL =
        "update HARVEST_SOURCE set EMAILS=?, STATEMENTS=?, COUNT_UNAVAIL=?, LAST_HARVEST=?, INTERVAL_MINUTES=?," +
        " LAST_HARVEST_FAILED=?, PRIORITY_SOURCE=?, SOURCE_OWNER=?," +
        " PERMANENT_ERROR=? where URL_HASH=?";
    /**
     *
     */
    @Override
    public void updateSourceHarvestFinished(HarvestSourceDTO sourceDTO) throws DAOException {

        if (sourceDTO == null) {
            throw new IllegalArgumentException("Source DTO must not be null!");
        } else if (sourceDTO.getUrl() == null) {
            throw new IllegalArgumentException("Source URL must not be null!");
        }

        ArrayList<Object> values = new ArrayList<Object>();
        values.add(sourceDTO.getEmails());
        values.add(sourceDTO.getStatements() == null ? (int) 0 : sourceDTO.getStatements());
        values.add(sourceDTO.getCountUnavail() == null ? (int) 0 : sourceDTO.getCountUnavail());
        values.add(sourceDTO.getLastHarvest() == null ? null : new Timestamp(sourceDTO.getLastHarvest().getTime()));
        values.add(sourceDTO.getIntervalMinutes() == null ? (int) 0 : sourceDTO.getIntervalMinutes());
        values.add(YesNoBoolean.format(sourceDTO.isLastHarvestFailed()));
        values.add(YesNoBoolean.format(sourceDTO.isPrioritySource()));
        values.add(sourceDTO.getOwner());
        values.add(YesNoBoolean.format(sourceDTO.isPermanentError()));
        values.add(Hashes.spoHash(sourceDTO.getUrl()));

        Connection conn = null;
        try {
            conn = getSQLConnection();
            SQLUtil.executeUpdate(UPDATE_SOURCE_HARVEST_FINISHED_SQL, values, conn);
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /** */
    private static final String DELETE_NONPRIORITY_UNAVAIL_SOURCES = "insert soft REMOVE_SOURCE_QUEUE (URL)"
        + " select URL from HARVEST_SOURCE where PRIORITY_SOURCE='N' and COUNT_UNAVAIL>=5";
    /**
     * @see eionet.cr.dao.HarvestSourceDAO#queueNonPriorityUnavailableSourcesForDeletion()
     */
    @Override
    public int queueNonPriorityUnavailableSourcesForDeletion() throws DAOException {

        Connection conn = null;
        try {
            conn = getSQLConnection();
            return SQLUtil.executeUpdate(DELETE_NONPRIORITY_UNAVAIL_SOURCES, conn);
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
        }

    }

    /**
     * SPARQL for deleting graph data.
     */
    private static final String CLEAR_GRAPH_SPARQL = "CLEAR GRAPH ?graph";
    @Override
    public void deleteSourceByUrl(String url) throws DAOException {

        boolean isSuccess = false;
        RepositoryConnection conn = null;

        Connection sqlConn = null;

        try {
            Bindings bindings = new Bindings();
            bindings.setURI("graph", URLUtil.replaceURLSpaces(url));

            // 2 updates brought here together to handle rollbacks correctly in
            // both DBs
            sqlConn = getSQLConnection();
            sqlConn.setAutoCommit(false);

            deleteSourceByUrl(url, sqlConn);

            conn = SesameUtil.getRepositoryConnection();
            conn.setAutoCommit(false);
            executeUpdateSPARQL(CLEAR_GRAPH_SPARQL, conn, bindings);

            // remove source metadata from http://cr.eionet.europa.eu/harvetser graph
            ValueFactory valueFactory = conn.getValueFactory();
            URI subject = valueFactory.createURI(URLUtil.replaceURLSpaces(url));
            URI context = valueFactory.createURI(GeneralConfig.HARVESTER_URI);
            conn.remove(subject, null, null, (Resource)context);

            sqlConn.commit();
            conn.commit();
            isSuccess = true;
        } catch (Exception e) {
            throw new DAOException("Error deleting source " + url, e);
        } finally {
            if (!isSuccess && conn != null) {
                try {
                    conn.rollback();
                } catch (RepositoryException re) {
                }
            }
            if (!isSuccess && sqlConn != null) {
                try {
                    sqlConn.rollback();
                } catch (SQLException e) {
                }
            }

            SQLUtil.close(sqlConn);
            SesameUtil.close(conn);

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#deleteSourceTriples(String)
     */
    @Override
    public void deleteSourceTriples(String url) throws DAOException {

        boolean isSuccess = false;
        RepositoryConnection conn = null;

        try {

            Bindings bindings = new Bindings();
            bindings.setURI("graph", URLUtil.replaceURLSpaces(url));

            conn = SesameUtil.getRepositoryConnection();
            conn.setAutoCommit(false);
            executeUpdateSPARQL(CLEAR_GRAPH_SPARQL, conn, bindings);

            conn.commit();
            isSuccess = true;
        } catch (Exception e) {
            throw new DAOException("Error deleting source triples " + url, e);
        } finally {
            if (!isSuccess && conn != null) {
                try {
                    conn.rollback();
                } catch (RepositoryException re) {
                }
            }
            SesameUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#getSourcesInInferenceRules()
     */
    @Override
    public String getSourcesInInferenceRules() throws DAOException {

        Connection conn = null;
        ResultSet rs = null;
        String ret = "";
        PreparedStatement stmt = null;
        try {
            conn = SesameUtil.getSQLConnection();
            stmt = conn.prepareStatement("SELECT RS_URI FROM DB.DBA.sys_rdf_schema where RS_NAME = ?");
            stmt.setString(1, GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_CR_RULESET_NAME));
            rs = stmt.executeQuery();

            StringBuffer sb = new StringBuffer();
            while (rs.next()) {
                String graphUri = rs.getString("RS_URI");
                if (!StringUtils.isBlank(graphUri)) {
                    sb.append("'").append(graphUri).append("'");
                    sb.append(",");
                }
            }

            ret = sb.toString();
            // remove last comma
            if (!StringUtils.isBlank(ret)) {
                ret = ret.substring(0, ret.lastIndexOf(","));
            }
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
            SQLUtil.close(rs);
            SQLUtil.close(stmt);
        }

        return ret;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#isSourceInInferenceRule()
     */
    @Override
    public boolean isSourceInInferenceRule(String url) throws DAOException {

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        boolean ret = false;
        try {
            conn = SesameUtil.getSQLConnection();
            stmt = conn.prepareStatement("SELECT RS_NAME FROM DB.DBA.sys_rdf_schema where RS_NAME = ? AND RS_URI = ?");
            stmt.setString(1, GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_CR_RULESET_NAME));
            stmt.setString(2, url);
            rs = stmt.executeQuery();
            while (rs.next()) {
                ret = true;
            }
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
            SQLUtil.close(rs);
            SQLUtil.close(stmt);
        }
        return ret;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#addSourceIntoInferenceRule()
     */
    @Override
    public boolean addSourceIntoInferenceRule(String url) throws DAOException {

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean ret = false;

        try {
            conn = SesameUtil.getSQLConnection();
            stmt = conn.prepareStatement("DB.DBA.rdfs_rule_set (?, ?)");
            stmt.setString(1, GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_CR_RULESET_NAME));
            stmt.setString(2, url);
            ret = stmt.execute();

        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
            SQLUtil.close(stmt);
        }
        return ret;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#removeSourceFromInferenceRule()
     */
    @Override
    public boolean removeSourceFromInferenceRule(String url) throws DAOException {

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean ret = false;

        try {
            conn = SesameUtil.getSQLConnection();
            stmt = conn.prepareStatement("DB.DBA.rdfs_rule_set (?, ?, 1)");
            stmt.setString(1, GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_CR_RULESET_NAME));
            stmt.setString(2, url);
            ret = stmt.execute();
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
            SQLUtil.close(stmt);
        }
        return ret;
    }

    /**
     * @see eionet.cr.dao.virtuoso.VirtuosoHarvestSourceDAO#loadIntoRepository(java.io.File, RDFFormat, java.lang.String, boolean)
     */
    @Override
    public int loadIntoRepository(File file, RDFFormat rdfFormat, String graphUrl, boolean clearPreviousGraphContent) throws IOException, OpenRDFException {

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return loadIntoRepository(inputStream, rdfFormat, graphUrl, clearPreviousGraphContent);

        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * @see eionet.cr.dao.virtuoso.VirtuosoHarvestSourceDAO#loadIntoRepository(java.io.InputStream, RDFFormat, java.lang.String, boolean)
     */
    @Override
    public int loadIntoRepository(InputStream inputStream, RDFFormat rdfFormat, String graphUrl, boolean clearPreviousGraphContent) throws IOException, OpenRDFException {

        int storedTriplesCount = 0;
        boolean isSuccess = false;
        RepositoryConnection conn = null;
        try {

            conn = SesameUtil.getRepositoryConnection();

            // convert graph URL into OpenRDF Resource
            org.openrdf.model.Resource graphResource = conn.getValueFactory().createURI(graphUrl);

            // start transaction
            conn.setAutoCommit(false);

            // if required, clear previous triples of this graph
            if (clearPreviousGraphContent){
                conn.clear(graphResource);
            }

            // add the stream content into repository under the given graph
            conn.add(inputStream, graphUrl, rdfFormat==null ? RDFFormat.RDFXML : rdfFormat, graphResource);

            long tripleCount = conn.size(graphResource);

            // commit transaction
            conn.commit();

            // set total stored triples count
            storedTriplesCount = Long.valueOf(tripleCount).intValue();

            // no transaction rollback needed, when reached this point
            isSuccess = true;
        } finally {
            if (!isSuccess) {
                SesameUtil.rollback(conn);
            }
            SesameUtil.close(conn);
        }

        return storedTriplesCount;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#addSourceMetadata(SubjectDTO)
     */
    @Override
    public void addSourceMetadata(SubjectDTO sourceMetadata) throws DAOException, RDFParseException, RepositoryException,
    IOException {

        if (sourceMetadata.getPredicateCount() > 0) {

            boolean isSuccess = false;
            RepositoryConnection conn = null;

            try {
                conn = SesameUtil.getRepositoryConnection();
                conn.setAutoCommit(false);

                // The contextURI is always the harvester URI
                URI harvesterContext = conn.getValueFactory().createURI(GeneralConfig.HARVESTER_URI);

                if (sourceMetadata != null) {
                    URI subject = conn.getValueFactory().createURI(sourceMetadata.getUri());

                    // Remove old predicates
                    conn.remove(subject, null, null, harvesterContext);

                    if (sourceMetadata.getPredicateCount() > 0) {
                        insertMetadata(sourceMetadata, conn, harvesterContext, subject);
                    }
                }
                // commit transaction
                conn.commit();

                // no transaction rollback needed, when reached this point
                isSuccess = true;
            } finally {
                if (!isSuccess && conn != null) {
                    try {
                        conn.rollback();
                    } catch (RepositoryException e) {
                    }
                }
                SesameUtil.close(conn);
            }
        }
    }

    /**
     * @param subjectDTO
     * @param conn
     * @param contextURI
     * @param subject
     * @throws RepositoryException
     */
    private void insertMetadata(SubjectDTO subjectDTO, RepositoryConnection conn, URI contextURI, URI subject)
    throws RepositoryException {

        for (String predicateUri : subjectDTO.getPredicates().keySet()) {

            Collection<ObjectDTO> objects = subjectDTO.getObjects(predicateUri);
            if (objects != null && !objects.isEmpty()) {

                URI predicate = conn.getValueFactory().createURI(predicateUri);
                for (ObjectDTO object : objects) {
                    if (object.isLiteral()) {
                        Literal literalObject = conn.getValueFactory().createLiteral(object.toString(), object.getDatatype());
                        conn.add(subject, predicate, literalObject, contextURI);
                    } else {
                        URI resourceObject = conn.getValueFactory().createURI(object.toString());
                        conn.add(subject, predicate, resourceObject, contextURI);
                    }
                }
            }
        }
    }

    /**
     * SPARQL for getting new sources based on the given source.
     */
    private static final String NEW_SOURCES_SPARQL = "DEFINE input:inference 'CRInferenceRule' PREFIX cr: "
        + "<http://cr.eionet.europa.eu/ontologies/contreg.rdf#> SELECT ?s FROM ?sourceUrl FROM ?deploymentHost WHERE "
        + "{ ?s a cr:File . OPTIONAL { ?s cr:lastRefreshed ?refreshed } FILTER( !BOUND(?refreshed)) }";

    /**
     * @see eionet.cr.dao.virtuoso.VirtuosoHarvestSourceDAO#getNewSources(java.lang.String)
     */
    @Override
    public List<String> getNewSources(String sourceUrl) throws DAOException{

        if (!StringUtils.isBlank(sourceUrl)) {
            String deploymentHost = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);

            Bindings bindings = new Bindings();
            bindings.setURI("sourceUrl", sourceUrl);
            bindings.setURI("deploymentHost", deploymentHost);

            SubjectReader matchReader = new SubjectReader();
            matchReader.setBlankNodeUriPrefix(VirtuosoBaseDAO.BNODE_URI_PREFIX);
            return executeSPARQL(NEW_SOURCES_SPARQL, bindings, matchReader);

        }

        return null;
    }
    /**
     * SPARQL for getting source metadata.
     */
    private static final String SOURCE_METADATA_SPARQL = "select ?o where {graph ?g { ?subject ?predicate ?o . "
        + "filter (?g = ?deploymentHost)}}";
    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#getSourceMetadata(String, String)
     */
    @Override
    public String getHarvestSourceMetadata(String harvestSourceUri, String predicateUri) throws DAOException, RepositoryException, IOException {

        String ret = null;
        RepositoryConnection conn = null;
        try {

            Bindings bindings = new Bindings();
            bindings.setURI("deploymentHost", GeneralConfig.HARVESTER_URI);
            bindings.setURI("subject", harvestSourceUri);
            bindings.setURI("predicate", predicateUri);

            Object resultObject = executeUniqueResultSPARQL(SOURCE_METADATA_SPARQL, bindings, new SingleObjectReader<Long>());
            ret = (String) resultObject;
        } finally {
            SesameUtil.close(conn);
        }
        return ret;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestSourceDAO#insertUpdateSourceMetadata(String, String, ObjectDTO)
     */
    @Override
    public void insertUpdateSourceMetadata(String subject, String predicate, ObjectDTO object) throws DAOException,
    RepositoryException, IOException {
        RepositoryConnection conn = null;
        try {

            conn = SesameUtil.getRepositoryConnection();

            URI harvesterContext = conn.getValueFactory().createURI(GeneralConfig.HARVESTER_URI);
            URI sub = conn.getValueFactory().createURI(subject);
            URI pred = conn.getValueFactory().createURI(predicate);

            conn.remove(sub, pred, null, harvesterContext);
            if (object.isLiteral()) {
                Literal literalObject = conn.getValueFactory().createLiteral(object.toString(), object.getDatatype());
                conn.add(sub, pred, literalObject, harvesterContext);
            } else {
                URI resourceObject = conn.getValueFactory().createURI(object.toString());
                conn.add(sub, pred, resourceObject, harvesterContext);
            }

        } finally {
            SesameUtil.close(conn);
        }
    }

    /**
     *
     */
    @Override
    public void deleteSubjectTriplesInSource(String subjectUri, String sourceUri) throws DAOException {

        if (!StringUtils.isBlank(subjectUri)) {
            RepositoryConnection conn = null;
            try {

                conn = SesameUtil.getRepositoryConnection();
                ValueFactory valueFactory = conn.getValueFactory();
                conn.remove(valueFactory.createURI(subjectUri), null, null, valueFactory.createURI(sourceUri));

            } catch (RepositoryException e) {
                throw new DAOException(e.getMessage(), e);
            } finally {
                SesameUtil.close(conn);
            }
        }
    }

}
