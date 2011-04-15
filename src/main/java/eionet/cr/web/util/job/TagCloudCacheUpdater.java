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
* Enriko Käsper, Tieto Eesti*/
package eionet.cr.web.util.job;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.TagsDAO;
import eionet.cr.web.util.ApplicationCache;

/**
 * TagCloud cache updater job.
 * @author <a href="mailto:enriko.kasper@tieto.com">Enriko Käsper</a>
 *
 */
public class TagCloudCacheUpdater implements StatefulJob {

    /** */
    private static Log logger = LogFactory.getLog(TagCloudCacheUpdater.class);

    /**
     * Executes the job.
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     * @param context current context.
     * @throws JobExecutionException if execution fails.
     */
    public void execute(final JobExecutionContext context) throws JobExecutionException {

        try {
            TagsDAO dao = DAOFactory.get().getDao(TagsDAO.class);
            ApplicationCache.updateTagCloudCache(dao.getTagCloud());
            logger.debug("Tag cloud cache updated!");
        } catch (DAOException e) {
            logger.error("Error when updating tag cloud cache: ", e);
        }
    }
}
