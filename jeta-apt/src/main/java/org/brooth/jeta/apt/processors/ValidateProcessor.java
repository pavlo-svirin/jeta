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

import com.squareup.javapoet.*;
import org.brooth.jeta.apt.MetacodeUtils;
import org.brooth.jeta.apt.ProcessingContext;
import org.brooth.jeta.apt.ProcessingException;
import org.brooth.jeta.apt.RoundContext;
import org.brooth.jeta.validate.MetaValidator;
import org.brooth.jeta.validate.Validate;
import org.brooth.jeta.validate.ValidatorMetacode;
import org.brooth.jeta.validate.alias.NotBlank;
import org.brooth.jeta.validate.alias.NotEmpty;
import org.brooth.jeta.validate.alias.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * @author Oleg Khalidov (brooth@gmail.com)
 */
public class ValidateProcessor extends AbstractProcessor {

    private Map<Class<? extends Annotation>, String> aliases;

    public ValidateProcessor() {
        super(Validate.class);
    }

    @Override
    public void init(ProcessingContext processingContext) {
        super.init(processingContext);

        aliases = new HashMap<>();
        aliases.put(NotBlank.class, "org.brooth.jeta.validate.NotBlank");
        aliases.put(NotEmpty.class, "org.brooth.jeta.validate.NotEmpty");
        aliases.put(NotNull.class, "org.brooth.jeta.validate.NotNull");

        for (String key : processingContext.processingProperties().stringPropertyNames()) {
            if (key.startsWith("validator.alias.")) {
                String annStr = key.substring("validator.alias.".length());
                String valStr = processingContext.processingProperties().getProperty(key);
                try {
                    Class<?> aliasClass = Class.forName(annStr);
                    if (aliasClass.isAssignableFrom(Annotation.class))
                        throw new IllegalArgumentException(annStr + " is not a annotation type.");

                    @SuppressWarnings("unchecked")
                    Class<? extends Annotation> aliasAnnotation = (Class<? extends Annotation>) aliasClass;
                    aliases.put(aliasAnnotation, valStr);

                } catch (Exception e) {
                    throw new ProcessingException("Failed to load '" + annStr + "' validator alias.", e);
                }
            }
        }
    }

    @Override
    public Set<Class<? extends Annotation>> collectElementsAnnotatedWith() {
        Set<Class<? extends Annotation>> result = new HashSet<>();
        result.add(Validate.class);
        result.addAll(aliases.keySet());
        return result;
    }

    public boolean process(TypeSpec.Builder builder, RoundContext context) {
        ClassName masterClassName = ClassName.get(context.metacodeContext().masterElement());
        builder.addSuperinterface(ParameterizedTypeName.get(
                ClassName.get(ValidatorMetacode.class), masterClassName));

        ParameterizedTypeName listTypeName = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class));
        ParameterizedTypeName arrayListTypeName = ParameterizedTypeName.get(ClassName.get(ArrayList.class), ClassName.get(String.class));

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("applyValidation")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(listTypeName)
                .addParameter(masterClassName, "master", Modifier.FINAL)
                .addStatement("$T errors = new $T()", listTypeName, arrayListTypeName);

        Elements elementUtils = processingContext.processingEnv().getElementUtils();
        int i = 0;
        for (Element element : context.elements()) {
            String fieldNameStr = element.getSimpleName().toString();

            final Validate annotation = element.getAnnotation(Validate.class);
            List<String> validators;
            if (annotation != null) {
                validators = MetacodeUtils.extractClassesNames(new Runnable() {
                    public void run() {
                        annotation.value();
                    }
                });

            } else {
                validators = new ArrayList<>();
            }

            for (Class<? extends Annotation> alias : aliases.keySet()) {
                if (element.getAnnotation(alias) != null)
                    validators.add(aliases.get(alias));
            }

            for (String validatorClassNameStr : validators) {
                String validatorVarName = "validator_" + i++;
                TypeElement validatorTypeElement = elementUtils.getTypeElement(validatorClassNameStr);
                if (validatorTypeElement == null)
                    throw new ProcessingException("Validator '" + validatorClassNameStr + "' not found");
                TypeName validatorTypeName = TypeName.get(validatorTypeElement.asType());

                // Class Validator
                if (validatorTypeElement.getKind() == ElementKind.CLASS) {
                    methodBuilder
                            .addStatement("$T $L = new $T()", validatorTypeName, validatorVarName, validatorTypeName)
                            .beginControlFlow("if(!($L.validate(master, master.$L, $S)))", validatorVarName, fieldNameStr, fieldNameStr)
                            .addStatement("errors.add($L.describeError())", validatorVarName)
                            .endControlFlow();

                    // MetacodeValidator
                } else {
                    MetaValidator metaValidator = validatorTypeElement.getAnnotation(MetaValidator.class);
                    if (metaValidator == null)
                        throw new IllegalArgumentException("Not valid Validator usage. '" + validatorClassNameStr
                                + "' must be implementation of Validator"
                                + " or interface annotated with org.brooth.jeta.validate.MetacodeValidator");

                    String expression = metaValidator.emitExpression()
                            .replaceAll("\\$f", "master." + fieldNameStr)
                            .replaceAll("\\$m", "master");
                    String error = metaValidator.emitError()
                            .replaceAll("\\$f", "master." + fieldNameStr)
                            .replaceAll("\\$n", fieldNameStr)
                            .replaceAll("\\$m", "master")
                            .replaceAll("\\$\\{([^}]*)}", "\" + ($1) + \"");

                    methodBuilder.beginControlFlow("if(!($L)) ", expression)
                            .addStatement("errors.add(\"$L\")", error)
                            .endControlFlow();
                }
            }
        }
        methodBuilder.addStatement("return errors");
        builder.addMethod(methodBuilder.build());

        return false;
    }
}

