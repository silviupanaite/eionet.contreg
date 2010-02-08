/*
* The contents of this file are subject to the Mozilla Public
* 
* License Version 1.1 (the "License"); you may not use this file
* except in compliance with the License. You may obtain a copy of
* the License at http://www.mozilla.org/MPL/
* 
* Software distributed under the License is distributed on an "AS
* IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
* implied. See the License for the specific language governing
* rights and limitations under the License.
* 
* The Original Code is Content Registry 2.0.
* 
* The Initial Owner of the Original Code is European Environment
* Agency. Portions created by Tieto Eesti are Copyright
* (C) European Environment Agency. All Rights Reserved.
* 
* Contributor(s):
* Jaanus Heinlaid, Tieto Eesti*/
package eionet.cr.harvest.persist.postgresql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eionet.cr.dto.UnfinishedHarvestDTO;
import eionet.cr.harvest.CurrentHarvests;
import eionet.cr.harvest.RDFHandler;
import eionet.cr.harvest.persist.IHarvestPersister;
import eionet.cr.harvest.persist.PersisterConfig;
import eionet.cr.harvest.persist.PersisterException;
import eionet.cr.harvest.persist.mysql.MySQLDerivationEngine;
import eionet.cr.harvest.scheduled.HarvestingJob;
import eionet.cr.harvest.util.HarvestLog;
import eionet.cr.util.Hashes;
import eionet.cr.util.Util;
import eionet.cr.util.YesNoBoolean;
import eionet.cr.util.sql.ConnectionUtil;
import eionet.cr.util.sql.SQLUtil;

/**
 * 
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 *
 */
public class PostgreSQLPersister implements IHarvestPersister{

	/** */
	private String spoTempTableName = "SPO_TEMP";
	private String resourceTempTableName = "RESOURCE_TEMP";
	private static final int TRIPLE_PROGRESS_INTERVAL = 50000;
	private static final int BULK_INSERT_SIZE = 50000;

	/** */
	private Log logger;
	
	//fields initialized through PersisterConfig object
	private long sourceUrlHash;
	private long genTime;
	private String instantHarvestUser;
	private String sourceUrl;

	private Connection connection;
	
	/** */
	private PreparedStatement preparedStatementForTriples;
	private PreparedStatement preparedStatementForResources;
	private int tripleCounter;
	private PersisterConfig config;

	/** */
	private int storedTriplesCount;

	/**
	 * 
	 */
	public PostgreSQLPersister(){
	}

	/**
	 * 
	 * @param config
	 */
	public PostgreSQLPersister(PersisterConfig config){
		
		this.config = config;
		sourceUrl = config.getSourceUrl();
		genTime = config.getGenTime();
		sourceUrlHash = config.getSourceUrlHash();
		instantHarvestUser = config.getInstantHarvestUser();
		
		logger = new HarvestLog(config.getSourceUrl(), config.getGenTime(), LogFactory.getLog(this.getClass()));
	}

	/*
	 * (non-Javadoc)
	 * @see eionet.cr.harvest.persist.IHarvestPersister#addTriple(long, boolean, long, java.lang.String, long, java.lang.String, boolean, boolean, long)
	 */
	public void addTriple(long subjectHash, boolean anonSubject, long predicateHash,
			String object, long objectHash, String objectLang, boolean litObject, boolean anonObject, long objSourceObject) throws PersisterException {
		
		try {
			preparedStatementForTriples.setLong  ( 1, subjectHash);
			preparedStatementForTriples.setLong  ( 2, predicateHash);
			preparedStatementForTriples.setString( 3, object);
			preparedStatementForTriples.setLong  ( 4, objectHash);
			preparedStatementForTriples.setObject( 5, Util.toDouble(object));
			preparedStatementForTriples.setString( 6, YesNoBoolean.format(anonSubject));
			preparedStatementForTriples.setString( 7, YesNoBoolean.format(anonObject));
			preparedStatementForTriples.setString( 8, YesNoBoolean.format(litObject));
			preparedStatementForTriples.setString( 9, objectLang==null ? "" : objectLang);		
			preparedStatementForTriples.setLong  (10, objSourceObject==0 ? 0 : sourceUrlHash);
			preparedStatementForTriples.setLong  (11, objSourceObject==0 ? 0 : genTime);
			preparedStatementForTriples.setLong  (12, objSourceObject);

			preparedStatementForTriples.addBatch();
			tripleCounter++;

			// if at BULK_INSERT_SIZE, execute the batch
			if (tripleCounter % BULK_INSERT_SIZE == 0){
				executeBatch();
			}
		}
		catch (SQLException e) {
			throw new PersisterException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see eionet.cr.harvest.persist.IHarvestPersister#addResource(java.lang.String, long)
	 */
	public void addResource(String uri, long uriHash) throws PersisterException {
		
		try {
			preparedStatementForResources.setString(1, uri);
			preparedStatementForResources.setLong(2, uriHash);
			preparedStatementForResources.addBatch();
		}
		catch (SQLException e) {
			throw new PersisterException(e.getMessage(), e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see eionet.cr.harvest.persist.IHarvestPersister#commit()
	 */
	public void commit() throws PersisterException {
		
		try {
			// clear previous content if required (for example it's not required when push-harvest
			if (config.isClearPreviousContent()){
				
				logger.debug("Deleting SPO rows of previous harvests");
				
				StringBuffer buf = new StringBuffer("delete from SPO where SOURCE=");
				buf.append(sourceUrlHash).append(" and GEN_TIME<").append(genTime);			
				SQLUtil.executeUpdate(buf.toString(), getConnection());
				
				buf = new StringBuffer("delete from SPO where OBJ_DERIV_SOURCE=");
				buf.append(sourceUrlHash).append(" and OBJ_DERIV_SOURCE_GEN_TIME<").append(genTime);			
				SQLUtil.executeUpdate(buf.toString(), getConnection());
			}

			// derive inferred triples, and extract new harvest sources 
			PostgreSQLDerivationEngine derivEngine = new PostgreSQLDerivationEngine(
					sourceUrl, sourceUrlHash, genTime, connection);
			if (config.isDeriveInferredTriples()){
				derivEngine.deriveLabels();
				derivEngine.deriveParentClasses();
				derivEngine.deriveParentProperties();
			}
			derivEngine.extractNewHarvestSources();
			
			// lower the "unfinished-harvest" flag
			deleteUnfinishedHarvestFlag();
			
			// commit all changes made during this harvest
			connection.commit();
		}
		catch (SQLException e) {
			throw new PersisterException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see eionet.cr.harvest.persist.IHarvestPersister#rollback()
	 */
	public void rollback() throws PersisterException {

		try {
			connection.rollback();
		}
		catch (SQLException e1) {
			throw new PersisterException(e1.getMessage(), e1);
		}
	}

	/**
	 * 
	 * @throws PersisterException
	 */
	public void executeBatch() throws PersisterException {
		try {
			int[] counts = preparedStatementForTriples.executeBatch();
			preparedStatementForTriples.clearParameters();
			System.gc();
			preparedStatementForResources.executeBatch();
			preparedStatementForResources.clearParameters();
			System.gc();
			
			int len = counts.length;
			for (int i=0; i<len; i++){
				storedTriplesCount = storedTriplesCount+ counts[i];
			}
			
			if (tripleCounter % TRIPLE_PROGRESS_INTERVAL == 0){
				logger.debug("Progress: " + String.valueOf(tripleCounter) + " triples processed");
			}
		} catch(SQLException e) {
			throw new PersisterException(e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see eionet.cr.harvest.persist.IHarvestPersister#endOfFile()
	 */
	public void endOfFile() throws PersisterException {
		
		// if there are any un-executed records left in the batch, execute them 
		if (tripleCounter % BULK_INSERT_SIZE != 0){
			executeBatch();
		}
		
		logger.debug("End of file, total of " + tripleCounter + " triples found in source");
	}

	/*
	 * (non-Javadoc)
	 * @see eionet.cr.harvest.persist.IHarvestPersister#openResources()
	 */
	public void openResources() throws PersisterException {
		try {
			connection = ConnectionUtil.getConnection();
			connection.setAutoCommit(false);
			
			// create unfinished harvest flag for the current harvest
			raiseUnfinishedHarvestFlag();
			
			// prepare statements
			prepareStatementForTriples();
			prepareStatementForResources();
	
			// store the hash of the source itself
			addResource(sourceUrl, sourceUrlHash);
			
			// let the debugger know that we have got our first triple
			logger.debug("Got first triple");
		} catch (SQLException fatal) {
			throw new PersisterException(fatal.getMessage(), fatal);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see eionet.cr.harvest.persist.IHarvestPersister#closeResources()
	 */
	public void closeResources(){
		
		SQLUtil.close(preparedStatementForTriples);
		SQLUtil.close(preparedStatementForResources);
		SQLUtil.close(connection);
	}

	/**
	 * 
	 * @throws SQLException
	 */
	private void prepareStatementForTriples() throws SQLException{
		
		StringBuffer buf = new StringBuffer().
		append("insert into SPO (SUBJECT,PREDICATE,OBJECT,OBJECT_HASH,OBJECT_DOUBLE").
		append(",ANON_SUBJ,ANON_OBJ,LIT_OBJ,OBJ_LANG").
		append(",OBJ_DERIV_SOURCE,OBJ_DERIV_SOURCE_GEN_TIME,OBJ_SOURCE_OBJECT,SOURCE,GEN_TIME)").
		append(" values (?,?,?,?,?,cast(? as ynboolean),cast(? as ynboolean),cast(? as ynboolean),?,?,?,?,").
		append(sourceUrlHash).append(",").append(genTime).append(")");

        preparedStatementForTriples = getConnection().prepareStatement(buf.toString());
	}

	/**
	 * 
	 * @throws SQLException 
	 */
	private void prepareStatementForResources() throws SQLException{

		StringBuffer buf = new StringBuffer().
		append("insert into RESOURCE (URI,URI_HASH,FIRSTSEEN_SOURCE,FIRSTSEEN_TIME,LASTMODIFIED_TIME)").
		append(" values (?,?,").append(sourceUrlHash).append(",").append(genTime).
		append(",").append(genTime).append(")");
		
        preparedStatementForResources = getConnection().prepareStatement(buf.toString());
	}

	/**
	 * 
	 * @throws SQLException
	 */
	private void raiseUnfinishedHarvestFlag() throws SQLException{
		
		StringBuffer buf = new StringBuffer();
		buf.append("insert into UNFINISHED_HARVEST (SOURCE, GEN_TIME) values (").
		append(sourceUrlHash).append(", ").append(genTime).append(")");
		
		SQLUtil.executeUpdate(buf.toString(), getConnection());
	}

	/**
	 * 
	 * @throws SQLException
	 */
	private void deleteUnfinishedHarvestFlag() throws SQLException{
		
		StringBuffer buf = new StringBuffer();
		buf.append("delete from UNFINISHED_HARVEST where SOURCE=").append(sourceUrlHash).append(" and GEN_TIME=").append(genTime);
		
		SQLUtil.executeUpdate(buf.toString(), connection);
	}

	/**
	 * 
	 * @throws SQLException
	 */
	private static void deleteUnfinishedHarvestFlag(long sourceUrlHash, long genTime, Connection conn) throws SQLException{
		
		StringBuffer buf = new StringBuffer();
		buf.append("delete from UNFINISHED_HARVEST where SOURCE=").append(sourceUrlHash).append(" and GEN_TIME=").append(genTime);
		
		SQLUtil.executeUpdate(buf.toString(), conn);
	}
	
	/**
	 * @return
	 */
	private Connection getConnection() {
		return connection;
	}

	/*
	 * (non-Javadoc)
	 * @see eionet.cr.harvest.persist.IHarvestPersister#rollbackUnfinishedHarvests()
	 */
	public void rollbackUnfinishedHarvests() throws PersisterException {

		ResultSet rs = null;
		Statement stmt = null;
		Connection conn = null;
		ArrayList<UnfinishedHarvestDTO> list = new ArrayList<UnfinishedHarvestDTO>();
		try{
			conn = ConnectionUtil.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select * from UNFINISHED_HARVEST");
			while (rs!=null && rs.next()){
				list.add(UnfinishedHarvestDTO.create(rs.getLong("SOURCE"), rs.getLong("GEN_TIME")));
			}

			if (!list.isEmpty()){
				
				LogFactory.getLog(RDFHandler.class).debug("Deleting leftovers from unfinished harvests");
				
				for (UnfinishedHarvestDTO unfinishedHarvestDTO:list){
					
					// if the source is not actually being currently harvested, only then roll it back
					if (!CurrentHarvests.contains(unfinishedHarvestDTO.getSource())){
						rollbackUnfinishedHarvest(unfinishedHarvestDTO.getSource(),
								unfinishedHarvestDTO.getGenTime(), conn);
					}
				}
			}
		}
		catch (SQLException fatal) {
			throw new PersisterException(fatal.getMessage(), fatal);
		}
		finally{
			SQLUtil.close(rs);
			SQLUtil.close(stmt);
			SQLUtil.close(conn);
		}
	}

	/**
	 * 
	 * @param sourceUrlHash
	 * @param genTime
	 * @param conn
	 * @throws SQLException
	 */
	private static void rollbackUnfinishedHarvest(
			long sourceUrlHash, long genTime, Connection conn) throws SQLException{

		try{
			// start transaction
			conn.setAutoCommit(false);

			// delete rows of given harvest from SPO
			StringBuffer buf = new StringBuffer("delete from SPO where (SOURCE=");
			buf.append(sourceUrlHash).append(" and GEN_TIME=").append(genTime).
			append(") or (OBJ_DERIV_SOURCE=").append(sourceUrlHash).
			append(" and OBJ_DERIV_SOURCE_GEN_TIME=").append(genTime).append(")");

			SQLUtil.executeUpdate(buf.toString(), conn);

			// delete rows of given harvest from RESOURCE
			buf = new StringBuffer("delete from RESOURCE where FIRSTSEEN_SOURCE=");
			buf.append(sourceUrlHash).append(" and FIRSTSEEN_TIME=").append(genTime);
			SQLUtil.executeUpdate(buf.toString(), conn);

			// delete new sources extracted from the given harvest
			buf = new StringBuffer("delete from HARVEST_SOURCE where SOURCE=").append(sourceUrlHash).
			append(" and GEN_TIME=").append(genTime);

			// 
			deleteUnfinishedHarvestFlag(sourceUrlHash, genTime, conn);

			conn.commit();
		}
		catch (SQLException e){
			try{conn.rollback();}catch (SQLException ee){}
			throw e;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see eionet.cr.harvest.persist.IHarvestPersister#getStoredTriplesCount()
	 */
	public int getStoredTriplesCount() {
		return storedTriplesCount;
	}
}
