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
package eionet.cr.util.sql;

/**
 *
 * @author heinljab
 *
 */
public class ParameterizedSQL {

    /** */
    private String sqlString;
    private String[] paramNames;

    /**
     *
     * @param sqlString
     * @param paramNames
     */
    public ParameterizedSQL(String sqlString, String paramNames) {
        this.sqlString = sqlString;
        this.paramNames = paramNames.split(",");
        for (int i = 0; i < this.paramNames.length; i++)
            this.paramNames[i] = this.paramNames[i].trim();
    }

    /**
     * @return the sqlString
     */
    public String getSqlString() {
        return sqlString;
    }

    /**
     * @return the paramNames
     */
    public String[] getParamNames() {
        return paramNames;
    }
}
