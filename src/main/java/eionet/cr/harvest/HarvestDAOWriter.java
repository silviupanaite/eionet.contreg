/*
 * The contents of this file are subject to the Mozilla Public
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
 * Agency.  Portions created by Tieto Eesti are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 * Jaanus Heinlaid, Tieto Eesti
 */
package eionet.cr.harvest;

import static eionet.cr.dao.mysql.MySQLDAOFactory.get;

import java.util.Iterator;
import java.util.List;

import eionet.cr.dao.DAOException;
import eionet.cr.dao.HarvestDAO;
import eionet.cr.dao.HarvestMessageDAO;
import eionet.cr.dao.HarvestSourceDAO;
import eionet.cr.dto.HarvestMessageDTO;
import eionet.cr.harvest.util.HarvestMessageType;
import eionet.cr.util.Util;


/**
 * 
 * @author heinljab
 *
 */
public class HarvestDAOWriter {
	
	/** */
	private int sourceId;
	private String harvestType;
	private int numOfResources;
	private String userName;	
	
	/** */
	private int harvestId;

	/**
	 * @param numOfResources TODO
	 * 
	 */
	public HarvestDAOWriter(int sourceId, String harvestType, int numOfResources, String userName){
		
		this.sourceId = sourceId;
		this.harvestType = harvestType;
		this.numOfResources = numOfResources;
		this.userName = userName;		
	}

	/**
	 * 
	 * @throws DAOException
	 */
	protected void writeStarted(Harvest harvest) throws DAOException{

		harvestId = get().getDao(HarvestDAO.class).insertStartedHarvest(sourceId, harvestType, userName, Harvest.STATUS_STARTED);
		get().getDao(HarvestSourceDAO.class).updateHarvestStarted(sourceId);
	}

	/**
	 * 
	 * @param harvest
	 * @param numResourcesInSource
	 * @throws DAOException
	 */
	protected void writeFinished(Harvest harvest) throws DAOException{
		
		get().getDao(HarvestDAO.class).updateFinishedHarvest(harvestId, Harvest.STATUS_FINISHED,
				harvest.getStoredTriplesCount(),
				harvest.getDistinctSubjectsCount(),
				0,
				0);
		//harvest failed IFF it has a fatal error || warnings || errors
		boolean failed = harvest.getFatalError() != null 
				|| (harvest.getErrors() != null && !harvest.getErrors().isEmpty())
				|| (harvest.getWarnings() != null && !harvest.getWarnings().isEmpty());
		
		if (harvest instanceof PullHarvest){
			get().getDao(HarvestSourceDAO.class)
					.updateHarvestFinished(
							sourceId,
							null,
							harvest.getDistinctSubjectsCount(),
							((PullHarvest)harvest).getSourceAvailable(),
							failed);
		}
		else{
			get().getDao(HarvestSourceDAO.class)
					.updateHarvestFinished(
							sourceId,
							null,
							numOfResources + harvest.getDistinctSubjectsCount(),
							null,
							failed);
		}
	}

	/**
	 * 
	 * @param harvest
	 * @throws DAOException
	 */
	protected void writeMessages(Harvest harvest) throws DAOException{
		
		// save the fatal exception
		writeThrowable(harvest.getFatalError(), HarvestMessageType.FATAL.toString());
		
		// save errors and warnings
		writeThrowables(harvest.getErrors(), HarvestMessageType.ERROR.toString());
		writeThrowables(harvest.getWarnings(), HarvestMessageType.WARNING.toString());
		
		// save infos
		List<String> infos = harvest.getInfos();
		if (infos!=null && !infos.isEmpty()){
			for (Iterator<String> i=infos.iterator(); i.hasNext();){
				
				HarvestMessageDTO harvestMessageDTO = new HarvestMessageDTO();
				harvestMessageDTO.setHarvestId(new Integer(harvestId));
				harvestMessageDTO.setType(HarvestMessageType.INFO.toString());
				harvestMessageDTO.setMessage(i.next());
				harvestMessageDTO.setStackTrace("");
				get().getDao(HarvestMessageDAO.class).insertHarvestMessage(harvestMessageDTO);
			}
		}
	}
	
	/**
	 * 
	 * @param throwable
	 * @param type
	 * @throws DAOException 
	 */
	protected void writeThrowable(Throwable throwable, String type) throws DAOException{
		
		if (throwable==null)
			return;
		
		HarvestMessageDTO harvestMessageDTO = new HarvestMessageDTO();
		harvestMessageDTO.setHarvestId(new Integer(this.harvestId));
		harvestMessageDTO.setType(type);
		harvestMessageDTO.setMessage(throwable.toString());
		harvestMessageDTO.setStackTrace(Util.getStackTrace(throwable));
		get().getDao(HarvestMessageDAO.class).insertHarvestMessage(harvestMessageDTO);
	}
	
	/**
	 * 
	 * @param throwables
	 * @param type
	 * @throws DAOException
	 */
	protected void writeThrowables(List<Throwable> throwables, String type) throws DAOException{
		
		if (throwables==null)
			return;
		
		for (int i=0; i<throwables.size(); i++){
			writeThrowable(throwables.get(i), type);
		}
	}
}
