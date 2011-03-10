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
package eionet.cr.dao;

import java.util.List;

import eionet.cr.dto.HarvestMessageDTO;

/**
 *
 * @author heinljab
 *
 */
public interface HarvestMessageDAO extends DAO {

    /**
     *
     * @return
     * @throws DAOException
     */
    public List<HarvestMessageDTO> findHarvestMessagesByHarvestID(int harvestID) throws DAOException;

    /**
     *
     * @return
     * @throws DAOException
     */
    public HarvestMessageDTO findHarvestMessageByMessageID(int messageID) throws DAOException;

    /**
     *
     * @param harvestMessageDTO
     * @throws DAOException
     */
    public Integer insertHarvestMessage(HarvestMessageDTO harvestMessageDTO) throws DAOException;

    /**
     *
     * @param messageId
     * @throws DAOException
     */
    public void deleteMessage(Integer messageId) throws DAOException;
}
