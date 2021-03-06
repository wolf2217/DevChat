package com.commandcenter.devchat.Model;

/**
 * Created by Command Center on 11/9/2017.
 */

public class User {

    private String Username;
    private String Rank;
    private String Status;

    public User() {
    }

    public User(String username, String rank, String status) {
        Username = username;
        Rank = rank;
        Status = status;
    }

    public String getUsername() {
        return Username;
    }

    public void setUsername(String username) {
        Username = username;
    }

    public String getRank() {
        return Rank;
    }

    public void setRank(String rank) {
        Rank = rank;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }
}
