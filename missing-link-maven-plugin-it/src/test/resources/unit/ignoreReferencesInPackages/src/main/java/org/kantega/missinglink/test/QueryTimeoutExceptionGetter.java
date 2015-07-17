package org.kantega.missinglink.test;

import org.springframework.dao.QueryTimeoutException;

public class QueryTimeoutExceptionGetter {
    public QueryTimeoutException getQueryTimeoutException(){
        return new QueryTimeoutException("QueryTimeoutException");
    }
}
