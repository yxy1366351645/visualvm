/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.tools.visualvm.heapviewer.truffle;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import com.sun.tools.visualvm.heapviewer.HeapFragment;
import com.sun.tools.visualvm.heapviewer.utils.HeapUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class TruffleLanguageHeapFragment extends HeapFragment {

    protected TruffleLanguageHeapFragment(String name, String ID, Instance langInfo, Heap heap) throws IOException {
        super(ID, name, createFragmentDescription(langInfo, heap), heap);
    }

    public Iterator<Instance> getInstancesIterator(String javaClassFqn) {
        return new InstancesIterator(HeapUtils.getSubclasses(heap, javaClassFqn));
    }
    
    public Iterator<Instance> getLanguageInstancesIterator(String languageID) {
        Iterator<Instance> instIt = new InstancesIterator(HeapUtils.getSubclasses(heap, DynamicObject.DYNAMIC_OBJECT_FQN));
        
        return new LanguageInstanceFilterIterator(instIt, languageID);
    }

    public Iterator<DynamicObject> getDynamicObjectsIterator() {
        return new DynamicObjectsIterator(HeapUtils.getSubclasses(heap, DynamicObject.DYNAMIC_OBJECT_FQN));
    }

    public Iterator<DynamicObject> getDynamicObjectsIterator(String languageID) {
        Iterator<DynamicObject> dynIt = new DynamicObjectsIterator(HeapUtils.getSubclasses(heap, DynamicObject.DYNAMIC_OBJECT_FQN));

        return new LanguageFilterIterator(dynIt, languageID);
    }

    private static String createFragmentDescription(Instance langInfo, Heap heap) {
        return DetailsSupport.getDetailsString(langInfo, heap);
    }

    private static class LanguageFilterIterator implements Iterator<DynamicObject> {
        private final String languageID;
        private final Iterator<DynamicObject> dynamicObjIterator;
        private DynamicObject next;

        private LanguageFilterIterator(Iterator<DynamicObject> dynIt, String langID) {
            dynamicObjIterator = dynIt;
            languageID = langID;
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            while (dynamicObjIterator.hasNext()) {
                DynamicObject dynObj = dynamicObjIterator.next();
                if (languageID.equals(dynObj.getLanguageId().getName())) {
                    next = dynObj;
                    return true;
                }
            }
            return false;
        }

        @Override
        public DynamicObject next() {
            if (hasNext()) {
                DynamicObject dynObj = next;
                next = null;
                return dynObj;
            }
            throw new NoSuchElementException();
        }
    }
    
    private static class LanguageInstanceFilterIterator implements Iterator<Instance> {
        private final String languageID;
        private final Iterator<Instance> instancesIterator;
        private Instance next;

        private LanguageInstanceFilterIterator(Iterator<Instance> instIt, String langID) {
            instancesIterator = instIt;
            languageID = langID;
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            while (instancesIterator.hasNext()) {
                Instance inst = instancesIterator.next();
                JavaClass langId = DynamicObject.getLanguageId(inst);
                if (langId != null && languageID.equals(langId.getName())) {
                    next = inst;
                    return true;
                }
            }
            return false;
        }

        @Override
        public Instance next() {
            if (hasNext()) {
                Instance inst = next;
                next = null;
                return inst;
            }
            throw new NoSuchElementException();
        }
    }

    private class InstancesIterator implements Iterator<Instance> {
        Iterator<JavaClass> classIt;
        Iterator<Instance> instanceIt;

        private InstancesIterator(Collection<JavaClass> cls) {
            classIt = cls.iterator();
            instanceIt = Collections.EMPTY_LIST.iterator();
        }

        @Override
        public boolean hasNext() {
            if (instanceIt.hasNext()) {
                return true;
            }
            if (!classIt.hasNext()) {
                return false;
            }
            instanceIt = classIt.next().getInstancesIterator();
            return hasNext();
        }

        @Override
        public Instance next() {
            return instanceIt.next();
        }
    }

    private class DynamicObjectsIterator implements Iterator<DynamicObject> {
        InstancesIterator instancesIter;

        private DynamicObjectsIterator(Collection<JavaClass> cls) {
            instancesIter = new InstancesIterator(cls);
        }

        @Override
        public boolean hasNext() {
            return instancesIter.hasNext();
        }

        @Override
        public DynamicObject next() {
            return new DynamicObject(instancesIter.next());
        }
    }

}