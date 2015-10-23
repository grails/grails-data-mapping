/*
 * Copyright 2015 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.gorm.query;

import java.io.Closeable;
import java.util.*;

/**
 * An abstract result list for initializing objects lazily from a cursor
 *
 * @author Graeme Rocher
 * @since 5.0
 */
public abstract class AbstractResultList extends AbstractList implements Closeable {
    protected int offset = 0;
    protected List initializedObjects = new ArrayList();
    private int internalIndex;
    private Integer size;
    protected boolean initialized = false;
    protected Iterator<Object> cursor;

    public AbstractResultList(int offset, Iterator<Object> cursor) {
        this.offset = offset;
        this.cursor = cursor;
    }

    public Iterator<Object> getCursor() {
        return cursor;
    }


    protected void initializeFully() {
        if (initialized) return;

        while (cursor.hasNext()) {
            Object current = convertObject();
            initializedObjects.add(current);
        }
        initialized = true;
    }


    @Override
    public boolean isEmpty() {
        if (initialized) return initializedObjects.isEmpty();
        else {
            return initializedObjects.isEmpty() && !cursor.hasNext();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object get(int index) {
        if (initializedObjects.size() > index) {
            return initializedObjects.get(index);
        } else if (!initialized) {
            while (cursor.hasNext()) {
                if (internalIndex > index)
                    throw new ArrayIndexOutOfBoundsException("Cannot retrieve element at index " + index + " for cursor size " + size());
                Object o = convertObject();
                initializedObjects.add(internalIndex, o);
                if (index == internalIndex++) {
                    return o;
                }
            }
            initialized = true;
        }
        throw new ArrayIndexOutOfBoundsException("Cannot retrieve element at index " + index + " for cursor size " + size());
    }

    protected Object convertObject() {
        return nextDecoded();
    }

    protected abstract Object nextDecoded();

    @Override
    public Object set(int index, Object o) {
        if (index > (size() - 1)) {
            throw new ArrayIndexOutOfBoundsException("Cannot set element at index " + index + " for cursor size " + size());
        } else {
            // initialize
            get(index);
            return initializedObjects.set(index, o);
        }
    }

    @Override
    public ListIterator listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator listIterator(int index) {
        initializeFully();
        return initializedObjects.listIterator(index);
    }

    /**
     * Override to transform elements if necessary during iteration.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    @Override
    public Iterator iterator() {
        if (initialized || !cursor.hasNext()) {
            if(!initialized) {
                initializeFully();
            }
            return initializedObjects.iterator();
        }


        return new Iterator() {
            Object current;
            int index = initializedObjects.size();

            public boolean hasNext() {

                boolean hasMore = cursor.hasNext();
                if (!hasMore) {
                    initialized = true;
                }
                return hasMore;
            }

            @SuppressWarnings("unchecked")
            public Object next() {
                current = convertObject();
                if (index < initializedObjects.size()){

                    initializedObjects.set(index++, current);
                }
                else {
                    index++;
                    initializedObjects.add(current);
                }
                return current;
            }

            public void remove() {
                initializedObjects.remove(current);
            }
        };
    }

    @Override
    public int size() {
        if (initialized) {
            return initializedObjects.size();
        }
        else if (this.size == null) {
            initializeFully();
            this.size = initializedObjects.size();
        }
        return size;
    }


}
