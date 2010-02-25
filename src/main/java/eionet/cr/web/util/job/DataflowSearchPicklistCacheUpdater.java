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
 * Aleksandr Ivanov, Tieto Eesti
 */
package eionet.cr.web.util.job;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import eionet.cr.common.Predicates;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dao.mysql.MySQLDAOFactory;
import eionet.cr.dao.readers.DataflowPicklistReader;
import eionet.cr.web.util.ApplicationCache;

/**
 * @author Aleksandr Ivanov
 * <a href="mailto:aleksandr.ivanov@tietoenator.com">contact</a>
 */
public class DataflowSearchPicklistCacheUpdater  implements StatefulJob {

	/** */
	private static Log logger = LogFactory.getLog(DataflowSearchPicklistCacheUpdater.class);
			
	/*
	 * (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		try {
			Collection<String> localities = MySQLDAOFactory
					.get()
					.getDao(HelperDAO.class)
					.getPicklistForPredicate(Predicates.ROD_LOCALITY_PROPERTY);
			
			ApplicationCache.updateDataflowPicklistCache(
					DAOFactory.get().getDao(HelperDAO.class).getDataflowSearchPicklist(),
					localities);
			logger.debug("Dataflow picklist cache updated");
		}
		catch (Exception e) {
			logger.error("Error when updating dataflow picklist cache cache: ", e);
		}
	}

}
