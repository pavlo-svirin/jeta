/*
 * Copyright 2015 Oleg Khalidov
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

package org.brooth.jeta.apt.processors;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.*;
import org.brooth.jeta.apt.MetacodeUtils;
import org.brooth.jeta.apt.RoundContext;
import org.brooth.jeta.observer.EventObserver;
import org.brooth.jeta.observer.Observe;
import org.brooth.jeta.observer.ObserverHandler;
import org.brooth.jeta.observer.ObserverMetacode;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import java.util.List;

/**
 * @author Oleg Khalidov (brooth@gmail.com)
 */public class ObserverProcessor extends AbstractProcessor {

    public ObserverProcessor() {
        super(Observe.class);
    }

    public boolean process(TypeSpec.Builder builder, RoundContext context) {
        ClassName masterClassName = ClassName.get(context.metacodeContext().masterElement());
        builder.addSuperinterface(ParameterizedTypeName.get(
                ClassName.get(ObserverMetacode.class), masterClassName));
        ClassName handlerClassName = ClassName.get(ObserverHandler.class);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("applyObservers")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(handlerClassName)
                .addParameter(masterClassName, "master", Modifier.FINAL)
                .addParameter(Object.class, "observable")
                .addParameter(Class.class, "observableClass")
                .addStatement("$T handler = new $T()", handlerClassName, handlerClassName);

        for (Element element : context.elements()) {
            final Observe annotation = element.getAnnotation(Observe.class);
            String observableClass = MetacodeUtils.extractClassName(new Runnable() {
                public void run() {
                    annotation.value();
                }
            });
            ClassName observableTypeName = ClassName.bestGuess(observableClass);
            ClassName metacodeTypeName = ClassName.bestGuess(MetacodeUtils.
                    getMetacodeOf(processingContext.processingEnv().getElementUtils(), observableClass));

            List<? extends VariableElement> params = ((ExecutableElement) element).getParameters();
            if (params.size() != 1)
                throw new IllegalArgumentException("Observer method must have one parameter (event)");
            TypeName eventTypeName = TypeName.get(params.get(0).asType());

            String methodHashName = "get" +
                    CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL,
                            CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, eventTypeName.toString())
                                    .replaceAll("\\.", "_")) + "Observers";

            TypeSpec eventObserverTypeSpec = TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(ParameterizedTypeName.get(
                            ClassName.get(EventObserver.class), eventTypeName))
                    .addMethod(MethodSpec.methodBuilder("onEvent")
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(eventTypeName, "event")
                            .returns(void.class)
                            .addStatement("master.$N(event)", element.getSimpleName().toString())
                            .build())
                    .build();

            methodBuilder
                    .beginControlFlow("if ($T.class == observableClass)", observableTypeName)
                    .addStatement("handler.add($T.class, $T.class,\n$T.$L(($T) observable).\nregister($L))",
                            observableTypeName, eventTypeName, metacodeTypeName, methodHashName,
                            observableTypeName, eventObserverTypeSpec)
                    .endControlFlow();
        }
        methodBuilder.addStatement("return handler");
        builder.addMethod(methodBuilder.build());
        return false;
    }
}
