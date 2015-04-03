/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.guid;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

/**
 * A simplified Spring TransactionTemplate that also provides the {@link JdbcTemplate} used to run
 * queries
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
@SuppressWarnings("serial")
class GuidTransactionTemplate extends
        org.springframework.transaction.support.TransactionTemplate {

    JdbcTemplate jt;

    public GuidTransactionTemplate(PlatformTransactionManager transactionManager, JdbcTemplate jt) {
        super(transactionManager);
        this.jt = jt;
    }

    public <T> T execute(final GuidTransactionCallback<T> action) throws TransactionException {
        return super.execute(new TransactionCallback<T>() {

            @Override
            public T doInTransaction(TransactionStatus status) {
                return action.doInTransaction(status, jt);
            }
        });
    }

    static interface GuidTransactionCallback<T> {
        T doInTransaction(TransactionStatus status, JdbcTemplate jt) throws TransactionException;
    }

}
