/*
 * Copyright 2016 Oleg Khalidov
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.brooth.jeta.collector;

import org.brooth.jeta.MasterClassController;
import org.brooth.jeta.Provider;
import org.brooth.jeta.metasitory.Metasitory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Oleg Khalidov (brooth@gmail.com)
 */
public class ObjectCollectorController extends MasterClassController<Object, ObjectCollectorMetacode> {

    public ObjectCollectorController(Metasitory metasitory, Class<?> masterClass) {
        super(metasitory, masterClass, ObjectCollector.class);
    }

    public List<Provider<?>> getObjects(Class<? extends Annotation> annotation) {
        assert annotation != null;

        List<Provider<?>> result = new ArrayList<Provider<?>>();
        for (ObjectCollectorMetacode collector : metacodes) {
            List<Provider<?>> collection = collector.getObjectCollection(annotation);
            if (collection != null)
                result.addAll(collection);
        }

        return result;
    }
}
