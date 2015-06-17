package hu.qualysoft.projtime.ui;

import java.util.Objects;

public abstract class LoginHolder {

    protected String username;
    private String password;

    public LoginHolder() {
    }

    public boolean setLogin(String username, String password) {
        if (!Objects.equals(this.username, username) || !Objects.equals(this.password, password)) {
            logout();
            if (login(username, password)) {
                this.username = username;
                this.password = password;
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean setLogin(String username, char[] password) {
        return setLogin(username, new String(password));
    }

    protected abstract boolean login(String username2, String password2);

    protected abstract void logout();

}
