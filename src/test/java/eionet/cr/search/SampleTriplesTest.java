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
package eionet.cr.search;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import eionet.cr.dao.SearchDAO;
import eionet.cr.dao.mysql.MySQLDAOFactory;
import eionet.cr.dto.RawTripleDTO;
import eionet.cr.util.Pair;
import eionet.cr.util.sql.ConnectionUtil;

/**
 * Unit test to test performance of sample triples search.
 * 
 * @author Aleksandr Ivanov
 * <a href="mailto:aleksandr.ivanov@tietoenator.com">contact</a>
 */
public class SampleTriplesTest extends TestCase {
	
	private String url = "http://cdr.eionet.europa.eu/referrals.rdf";

	@Test
	public void testGetSampleTriples() throws Exception {
		ConnectionUtil.setReturnSimpleConnection(true);
		Pair<Integer, List<RawTripleDTO>> result = MySQLDAOFactory.get().getDao(SearchDAO.class).getSampleTriples(url, 10);
		assertEquals(10, result.getValue().size());
	}
}
