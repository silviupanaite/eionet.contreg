package eionet.cr.errors;

public class LdapDaoException extends Exception {

    public LdapDaoException() {
    }

    public LdapDaoException(String message) {
        super(message);
    }

    public LdapDaoException(Exception ex) {
        super(ex);
    }
}
