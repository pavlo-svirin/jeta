package org.javameta.apt;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.*;
import org.javameta.pubsub.Publish;
import org.javameta.pubsub.PublisherMetacode;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

/**
 *
 */
public class PublishProcessor extends SimpleProcessor {

    public PublishProcessor() {
        super(Publish.class);
    }

    @Override
    public boolean process(RoundEnvironment roundEnv, ProcessorContext ctx, TypeSpec.Builder builder, int round) {
        MetacodeContext context = ctx.metacodeContext;
        ClassName masterClassName = ClassName.bestGuess(context.getMasterCanonicalName());
        builder.addSuperinterface(ParameterizedTypeName.get(
                ClassName.get(PublisherMetacode.class), masterClassName));

        MethodSpec.Builder applyMethodSpecBuilder = MethodSpec.methodBuilder("applyPublisher")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(masterClassName, "master");

        for (Element element : ctx.elements) {
            String fieldName = element.getSimpleName().toString();

            String monitorFiledName = fieldName + "_MONITOR";
            builder.addField(FieldSpec.builder(Object.class, monitorFiledName)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new Object()")
                    .build());

            TypeName subscribersTypeName = TypeName.get(element.asType());
            FieldSpec subscribersFieldSpec = FieldSpec.builder(subscribersTypeName, fieldName)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                    .build();
            builder.addField(subscribersFieldSpec);

            String eventTypeStr = subscribersTypeName.toString();
            int i = eventTypeStr.indexOf('<');
            if (i == -1)
                throw new IllegalArgumentException("Not valid @Publish usage, define event type as generic of Subscribers");

            eventTypeStr = eventTypeStr.substring(i + 1, eventTypeStr.lastIndexOf('>'));
            String methodHashName = "get" +
                    CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL,
                            CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, eventTypeStr)
                                    .replaceAll("\\.", "_")) + "Subscribers";

            MethodSpec getObserversMethodSpec = MethodSpec.methodBuilder(methodHashName)
                    .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                    .returns(subscribersTypeName)
                    .beginControlFlow("if ($L == null)", fieldName)
                    .beginControlFlow("synchronized ($L)", monitorFiledName)
                    .beginControlFlow("if ($L == null)", fieldName)
                    .addStatement("$L = new $T()", fieldName, subscribersTypeName)
                    .endControlFlow()
                    .endControlFlow()
                    .endControlFlow()
                    .addStatement("return $L", fieldName)
                    .build();
            builder.addMethod(getObserversMethodSpec);

            applyMethodSpecBuilder.addStatement("master.$L = $L()", fieldName, methodHashName);
        }
        builder.addMethod(applyMethodSpecBuilder.build());

        return false;
    }
}