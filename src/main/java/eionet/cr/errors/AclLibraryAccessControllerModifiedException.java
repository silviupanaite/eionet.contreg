package eionet.cr.errors;

public class AclLibraryAccessControllerModifiedException extends Exception {

    public AclLibraryAccessControllerModifiedException() {
    }

    public AclLibraryAccessControllerModifiedException(String message) {
        super(message);
    }

    public AclLibraryAccessControllerModifiedException(String message, Throwable cause) {
        super(message, cause);
    }
}
