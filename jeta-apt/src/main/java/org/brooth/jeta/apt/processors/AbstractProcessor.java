/*
 * Copyright 2016 Oleg Khalidov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.brooth.jeta.apt.processors;

import org.brooth.jeta.apt.MetacodeUtils;
import org.brooth.jeta.apt.ProcessingContext;
import org.brooth.jeta.apt.Processor;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

/**
 * @author Oleg Khalidov (brooth@gmail.com)
 */
public abstract class AbstractProcessor implements Processor {

    protected Class<? extends Annotation> annotation;
    protected TypeElement annotationElement;

    protected ProcessingContext processingContext;

    public AbstractProcessor(Class<? extends Annotation> annotation) {
        this.annotation = annotation;
    }

    public AbstractProcessor(TypeElement annotationElement) {
        this.annotationElement = annotationElement;
    }

    public void init(ProcessingContext processingContext) {
        this.processingContext = processingContext;
        if (annotationElement == null && annotation != null)
            this.annotationElement = processingContext.processingEnv().getElementUtils().
                    getTypeElement(annotation.getCanonicalName());
    }

    public Set<TypeElement> collectElementsAnnotatedWith() {
        TypeElement t = processingContext.processingEnv().getElementUtils().getTypeElement(annotation.getCanonicalName());
        return Collections.singleton(t);
    }

    public Set<TypeElement> applicableMastersOfElement(Element element) {
        return Collections.singleton(MetacodeUtils.typeElementOf(element));
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean needReclaim() {
        return false;
    }

    public boolean ignoreUpToDate() {
        return false;
    }
}
