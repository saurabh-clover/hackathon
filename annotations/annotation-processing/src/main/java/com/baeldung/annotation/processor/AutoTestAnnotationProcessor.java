package com.baeldung.annotation.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.sun.tools.javac.code.Type;
//import org.junit.Test;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sun.tools.javac.code.Symbol.*;

@SupportedAnnotationTypes("com.clover.autotest.annotation.AutoTest")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class AutoTestAnnotationProcessor extends AbstractProcessor {

    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {

            Set<? extends Element> annotatedMethods = roundEnv.getElementsAnnotatedWith(annotation);

            Map<Element, ? extends List<? extends Element>> result =
                    annotatedMethods.stream().collect(Collectors.groupingBy(Element::getEnclosingElement));

            result.forEach((element, list) -> {
                try {
                    produceJavaFile(((TypeElement)element).getQualifiedName().toString(), list.stream()
                            .collect(Collectors.toMap(method -> (MethodSymbol) method,
                                    method -> ((ExecutableType) method.asType()).getParameterTypes())));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        }

        return true;
    }

    private void produceJavaFile(String className, Map<MethodSymbol, List<? extends TypeMirror>> methodMap) throws IOException {
        String packageName = null;
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = className.substring(0, lastDot);
        }

        String testClassName = className + "Test";
        String testSimpleClassName = testClassName.substring(lastDot + 1);
        final TypeSpec.Builder testFileBuilder = TypeSpec.classBuilder(testSimpleClassName).addModifiers(Modifier.PUBLIC);
        methodMap.entrySet().forEach(entry -> {
            String methodName = entry.getKey().getSimpleName().toString();
            Type returnType = entry.getKey().getReturnType();
            List<? extends TypeMirror> parameterTypes = entry.getValue();
            testFileBuilder.addMethod(generateMethod(methodName, returnType, parameterTypes));
        });
        TypeSpec typeSpec = testFileBuilder.build();
        JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();

        javaFile.writeTo(filer);

    }

    private static MethodSpec generateMethod(String name, Type returnType, List<? extends TypeMirror> params) {
        long[] i = {1};
        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("test" + (char)(name.charAt(0) - 32) + name.substring(1));
//        params.stream().forEach(type -> {
//            methodSpecBuilder.addParameter(TypeName.get(type), "val" + i[0]++);
//        });
        if (!returnType.isPrimitiveOrVoid()) {
            methodSpecBuilder.addStatement("$T object = new $T()", returnType, returnType);
        }

//        methodSpecBuilder.addAnnotation(Test.class);
        return methodSpecBuilder.returns(TypeName.VOID).build();
    }

    private void writeBuilderFile(String className, Map<String, List<? extends TypeMirror>> methodMap)
            throws IOException {

        String packageName = null;
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = className.substring(0, lastDot);
        }

        String simpleClassName = className.substring(lastDot + 1);
        String builderClassName = className + "Builder";
        String builderSimpleClassName = builderClassName.substring(lastDot + 1);

        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(builderClassName);
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            if (packageName != null) {
                out.print("package ");
                out.print(packageName);
                out.println(";");
                out.println();
            }

            out.print("public class ");
            out.print(builderSimpleClassName);
            out.println(" {");
            out.println();

            out.print("    private ");
            out.print(simpleClassName);
            out.print(" object = new ");
            out.print(simpleClassName);
            out.println("();");
            out.println();

            out.print("    public ");
            out.print(simpleClassName);
            out.println(" build() {");
            out.println("        return object;");
            out.println("    }");
            out.println();

            methodMap.entrySet().forEach(setter -> {
                String methodName = setter.getKey();
                String argumentType = "int";

                out.print("    public ");
                out.print(builderSimpleClassName);
                out.print(" ");
                out.print(methodName);

                out.print("(");

                out.print(argumentType);
                out.println(" value) {");
                out.print("        object.");
                out.print(methodName);
                out.println("(value);");
                out.println("        return this;");
                out.println("    }");
                out.println();
            });

            out.println("}");

        }
    }

}
