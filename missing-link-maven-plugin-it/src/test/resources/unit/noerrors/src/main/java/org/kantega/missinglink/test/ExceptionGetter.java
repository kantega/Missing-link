package org.kantega.missinglink.test;

import org.apache.commons.dbcp.SQLNestedException;

import java.sql.SQLException;

public class ExceptionGetter {
    public SQLException getSQLException(){
        return new SQLNestedException("Lol", new SQLException("No reason"));
    }
}
