package org.kantega.missinglink.test;

import org.apache.commons.dbcp.SQLNestedException;

import java.sql.SQLException;

public class DBCPUser {
    public SQLException getSQLException(){
        return new SQLNestedException("Lol", new SQLException("No reason"));
    }
}
