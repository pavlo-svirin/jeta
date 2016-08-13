/*
 *  Copyright 2016 Oleg Khalidov
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
 *
 */

package org.brooth.jeta.apt.processors;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import org.brooth.jeta.apt.MetacodeUtils;
import org.brooth.jeta.apt.ProcessingContext;
import org.brooth.jeta.apt.ProcessingException;
import org.brooth.jeta.apt.RoundContext;
import org.brooth.jeta.inject.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Oleg Khalidov (brooth@gmail.com)
 */

public class ModuleProcessor extends AbstractProcessor {

    private Set<Element> allProducers;

    @Nullable
    private String defaultScopeStr;

    public ModuleProcessor() {
        super(Module.class);
    }

    @Override
    public void init(ProcessingContext processingContext) {
        super.init(processingContext);
        defaultScopeStr = processingContext.processingProperties().getProperty("inject.scope.default", null);
    }

    @Override
    public boolean process(TypeSpec.Builder builder, RoundContext context) {
        if (allProducers != null)
            throw new ProcessingException("Multiple modules defined");

        TypeElement moduleElement = (TypeElement) context.elements().iterator().next();
        final Module annotation = moduleElement.getAnnotation(Module.class);
        List<String> scopesStr = getScopesClasses(annotation);

        List<TypeElement> scopes = Lists.transform(scopesStr, new Function<String, TypeElement>() {
            @Override
            public TypeElement apply(String input) {
                TypeElement scopeElement = processingContext.processingEnv().getElementUtils().getTypeElement(input);
                Scope scopeAnnotation = scopeElement.getAnnotation(Scope.class);
                if (scopeAnnotation == null)
                    throw new ProcessingException(input + " is not a scope class. Put @Scope on it");
                return scopeElement;
            }
        });

        allProducers = new HashSet<>();
        allProducers.addAll(context.roundEnv().getElementsAnnotatedWith(Producer.class));

        List<AnnotationSpec> scopeAnnotationBuilders = new ArrayList<>(scopes.size());
        AnnotationSpec.Builder moduleConfigAnnotationBuilder = AnnotationSpec.builder(ModuleConfig.class);

        for (TypeElement scopeElement : scopes) {
            String scopeClassStr = scopeElement.getQualifiedName().toString();
            TypeElement metaScopeElement = processingContext.processingEnv().getElementUtils().getTypeElement(
                    MetacodeUtils.toMetacodeName(scopeClassStr));
            ScopeConfig metaScopeConfigAnnotation = metaScopeElement != null ? metaScopeElement.getAnnotation(ScopeConfig.class) : null;
            if (metaScopeConfigAnnotation != null) {
                String scopeModuleStr = getModuleClass(metaScopeConfigAnnotation);
                if (!moduleElement.getQualifiedName().toString().equals(scopeModuleStr)) {
                    processingContext.logger().debug(scopeClassStr + " belongs to ext module " + scopeModuleStr);
                    scopeAnnotationBuilders.add(AnnotationSpec.builder(ModuleConfig.ScopeConfig.class)
                            .addMember("\nscope", scopeClassStr + ".class")
                            .addMember("\nmodule", scopeModuleStr + ".class\n")
                            .build());
                    continue;
                }
            }

            Set<? extends Element> scopeEntities = getScopeEntities(scopeElement);
            Iterable<ClassName> entities = Iterables.transform(scopeEntities, new Function<Element, ClassName>() {
                @Override
                public ClassName apply(Element input) {
                    try {
                        final Producer producerAnnotation = input.getAnnotation(Producer.class);
                        String ofTypeStr = getOfClass(producerAnnotation);
                        if (isVoid(ofTypeStr))
                            return ClassName.get((TypeElement) input);
                        if (ofTypeStr.equals(getExtClass(producerAnnotation)))
                            return ClassName.get(Void.class);

                        return ClassName.bestGuess(ofTypeStr);

                    } catch (Exception e) {
                        throw new ProcessingException("Failed to get meta entity info of element " + input.toString(), e);
                    }
                }
            });

            scopeAnnotationBuilders.add(AnnotationSpec.builder(ModuleConfig.ScopeConfig.class)
                    .addMember("\nscope", scopeClassStr + ".class")
                    .addMember("\nentities", !entities.iterator().hasNext() ? "{}" :
                                    "{\n" + Joiner.on(".class,\n").join(entities) + ".class\n}",
                            entities)
                    .build());
        }

        builder.addAnnotation(moduleConfigAnnotationBuilder
                .addMember("scopes", "{\n" + Joiner.on(",\n").join(scopeAnnotationBuilders) + "}",
                        scopeAnnotationBuilders)
                .build());

        return false;
    }

    @Nonnull
    protected String getModuleClass(final ScopeConfig scopeConfig) {
        return MetacodeUtils.extractClassName(new Runnable() {
            @Override
            public void run() {
                scopeConfig.module();
            }
        });
    }

    @Nonnull
    private String getOfClass(final Producer annotation) {
        return MetacodeUtils.extractClassName(new Runnable() {
            public void run() {
                annotation.of();
            }
        });
    }

    @Nonnull
    private String getExtClass(final Producer annotation) {
        return MetacodeUtils.extractClassName(new Runnable() {
            public void run() {
                annotation.ext();
            }
        });
    }

    private List<String> getScopesClasses(final Module annotation) {
        return MetacodeUtils.extractClassesNames(new Runnable() {
            @Override
            public void run() {
                annotation.scopes();
            }
        });
    }

    private Set<? extends Element> getScopeEntities(TypeElement scopeElement) {
        final String scopeClassStr = scopeElement.getQualifiedName().toString();
        final boolean isDefaultScope = defaultScopeStr != null && defaultScopeStr.equals(scopeClassStr);

        return Sets.filter(allProducers, new Predicate<Element>() {
            public boolean apply(Element input) {
                final Producer producerAnnotation = input.getAnnotation(Producer.class);
                String scope = MetacodeUtils.extractClassName(new Runnable() {
                    public void run() {
                        producerAnnotation.scope();
                    }
                });

                if (scopeClassStr.equals(scope))
                    return true;

                if (isVoid(scope)) {
                    if (defaultScopeStr == null)
                        throw new ProcessingException("Scope undefined for '" + input.getSimpleName().toString() + "'. " +
                                "You need to set the scope via @Producer(scope) or define default one as 'inject.scope.default' property");
                    if (isDefaultScope)
                        return true;
                }

                return false;
            }
        });
    }

    private boolean isVoid(String str) {
        return str.equals(Void.class.getCanonicalName());
    }

    @Override
    public boolean ignoreUpToDate() {
        return true;
    }
}