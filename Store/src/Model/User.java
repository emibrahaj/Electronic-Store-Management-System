package Model;

import java.io.Serializable;

public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    protected String phoneNumber;
    private Role role;
    protected String email;
/// ////////////////////////
    protected String name;
    protected String accessLevel;


    // Constructor
    public User(String username, String password,String name,String email, String phoneNumber) {
        this.username = username;
        this.password = password;
        this.name=name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }



	protected boolean loggedIn;



    // Login method
    public boolean login(String username, String password) {
        loggedIn = false;
        boolean matches;
        try {
            matches = this.password.contains(":") ? DAO.PasswordManager.verifyPassword(password, this.password)
                    : this.password.equals(password);
        } catch (Exception e) { matches = false; }
        if (this.username.equals(username) && matches) {
            loggedIn = true;
            System.out.println(username + " " + name + " logged in successfully as " + role + "\n");
        } else {
            System.out.println("Invalid username or password." + "\n");
        }
		return loggedIn;
    }

    // Logout method
    public void logout() {
        if (loggedIn) {
            loggedIn = false;
            System.out.println(username + " " + name +  " logged out." + "\n");
        } else {
            System.out.println("User is not logged in." + "\n");
        }
    }

    // toString method to display user details
    @Override
    public String toString() {
        return "Username: " + username + "\n" +"Role: " + role + "\n";
    }

	public Role getRole() {
		return role;
	}

    public void setRole(Role role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

}
