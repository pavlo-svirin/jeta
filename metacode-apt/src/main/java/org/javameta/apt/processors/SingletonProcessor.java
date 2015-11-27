/*
 * Copyright 2015 Oleg Khalidov
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
 */

package org.javameta.apt.processors;

import com.squareup.javapoet.*;
import org.javameta.apt.MetacodeContext;
import org.javameta.apt.ProcessorEnvironment;
import org.javameta.util.Singleton;
import org.javameta.util.SingletonMetacode;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

/**
 * @author Oleg Khalidov (brooth@gmail.com)
 */
public class SingletonProcessor extends SimpleProcessor {

    public SingletonProcessor() {
        super(Singleton.class);
    }

    @Override
    public boolean process(ProcessorEnvironment env, TypeSpec.Builder builder) {
        MetacodeContext context = env.metacodeContext();
        ClassName masterClassName = ClassName.bestGuess(context.getMasterCanonicalName());
        builder.addSuperinterface(ParameterizedTypeName.get(
                ClassName.get(SingletonMetacode.class), masterClassName));

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("applySingleton")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class);

        for (Element element : env.elements()) {
            String fieldName = element.getSimpleName().toString();
            String monitorFiledName = fieldName + "_MONITOR";

            builder.addField(FieldSpec.builder(Object.class, monitorFiledName)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new Object()")
                    .build());

            String initStr = element.getAnnotation(Singleton.class).staticConstructor();
            if (initStr.isEmpty())
                initStr = "new $T()";
            else
                initStr = "$T." + initStr + "()";

            methodBuilder
                    .beginControlFlow("if($T.$L == null)", masterClassName, fieldName)
                    .beginControlFlow("synchronized ($L)", monitorFiledName)
                    .beginControlFlow("if($T.$L == null)", masterClassName, fieldName)
                    .addStatement("$T.$L = " + initStr, masterClassName, fieldName, masterClassName)
                    .endControlFlow()
                    .endControlFlow()
                    .endControlFlow();
        }

        builder.addMethod(methodBuilder.build());
        return false;
    }
}