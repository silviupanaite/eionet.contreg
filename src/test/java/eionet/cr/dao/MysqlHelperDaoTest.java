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
package eionet.cr.dao;

import java.util.List;

import org.junit.Test;

import eionet.cr.dao.mysql.MySQLDAOFactory;
import eionet.cr.util.Pair;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Aleksandr Ivanov
 * <a href="mailto:aleksandr.ivanov@tietoenator.com">contact</a>
 */
public class MysqlHelperDaoTest extends TestCase {
	
	@Test
	public void testGetRecentFiles() throws DAOException {
		
		// TODO this test needs to be fixed by extending DBTestCase, and loading a dataset that contains 10 recently discovered resources
		
//		HelperDAO dao = PostgreSQLDAOFactory.get().getDao(HelperDAO.class);
//		List<Pair<String, String>> result = dao.getRecentlyDiscoveredFiles(10);
//		Assert.assertNotNull(result);
//		Assert.assertEquals(10, result.size());
	}
}
