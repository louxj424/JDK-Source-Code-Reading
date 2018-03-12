package sun.net.www.protocol.http;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Arrays;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * @since 1.6
 */
public class NegotiateCallbackHandler implements CallbackHandler {
    
    private String username;
    private char[] password;
    
    public void handle(Callback[] callbacks) throws
            UnsupportedCallbackException, IOException {
        for (int i=0; i<callbacks.length; i++) {
            Callback callBack = callbacks[i];
            
            if (callBack instanceof NameCallback) {
                if (username == null) {
                    PasswordAuthentication passAuth =
                            Authenticator.requestPasswordAuthentication(
                            null, null, 0, null,
                            null, "Negotiate");
                    username = passAuth.getUserName();
                    password = passAuth.getPassword();
                }
                NameCallback nameCallback =
                        (NameCallback)callBack;
                nameCallback.setName(username);
                
            } else if (callBack instanceof PasswordCallback) {
                PasswordCallback passwordCallback =
                        (PasswordCallback)callBack;
                if (password == null) {
                    PasswordAuthentication passAuth =
                            Authenticator.requestPasswordAuthentication(
                            null, null, 0, null,
                            null, "Negotiate");
                    username = passAuth.getUserName();
                    password = passAuth.getPassword();
                }
                passwordCallback.setPassword(password);
                Arrays.fill(password, ' ');
            } else {
                throw new UnsupportedCallbackException(callBack,
                        "Call back not supported");
            }//else
        }//for
    }    
}