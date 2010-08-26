package org.springframework.datastore.core;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 25, 2010
 * Time: 10:48:34 AM
 * To change this template use File | Settings | File Templates.
 */
public interface SessionImplementor {
    Collection<Runnable> getPendingInserts();

    Collection<Runnable> getPendingUpdates();

    Collection<Runnable> getPendingDeletes();
}
