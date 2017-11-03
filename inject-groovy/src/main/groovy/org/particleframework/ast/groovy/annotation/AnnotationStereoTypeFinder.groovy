package org.particleframework.ast.groovy.annotation

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.Memoized
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.runtime.memoize.LRUCache
import org.particleframework.ast.groovy.utils.AstAnnotationUtils
import org.particleframework.core.value.OptionalValues

import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Scope
import java.lang.annotation.Annotation
import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.Target

/**
 * Deals with annotation stereotypes
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class AnnotationStereoTypeFinder {
    private static final List<String> EXCLUDED_ANNOTATIONS = [Retention.name, Documented.name, Target.name, Inject.name, Qualifier.name, Scope.name]
    private static final Object NO_RESULT = new Object()
    private final LRUCache cache = new LRUCache(100)

    boolean hasStereoType(AnnotatedNode annotatedNode, Class<? extends Annotation> stereotype) {
        return hasStereoType(annotatedNode, stereotype.name)
    }

    boolean hasStereoType(AnnotatedNode annotatedNode, String... stereotypes) {
        return hasStereoType(annotatedNode, Arrays.asList(stereotypes))
    }

    boolean hasStereoType(AnnotatedNode annotatedNode, List<String> stereotypes) {
        for(stereotype in stereotypes) {
            if(findAnnotationWithStereoType(annotatedNode, stereotype) ) {
                return true
            }
        }
        return false
    }

    AnnotationNode findAnnotationWithStereoType(AnnotatedNode annotatedNode, Class<? extends Annotation> stereotype) {
        return findAnnotationWithStereoType(annotatedNode, stereotype.name)
    }

    AnnotationNode findAnnotationWithStereoType(AnnotatedNode annotatedNode, String stereotype) {
        def key = new Key(annotatedNode, stereotype)
        def result = cache.get(key)
        if(result == NO_RESULT) {
            return null
        }
        else if(result != null) {
            return (AnnotationNode)result
        }
        else {
            AnnotationNode node = findAnnotationWithStereoTypeNoCache(annotatedNode, stereotype)
            if(node != null) {
                cache.put(key, node)
            }
            if(node == null) {
                cache.put(key, NO_RESULT)
            }
            return node
        }
    }

    private AnnotationNode findAnnotationWithStereoTypeNoCache(AnnotatedNode annotatedNode, String stereotype) {
        List<AnnotationNode> annotations = annotatedNode.getAnnotations()
        for(AnnotationNode ann in annotations) {
            ClassNode annotationClassNode = ann.classNode
            if(annotationClassNode.name == stereotype) {
                return ann
            }
            else if(!(annotationClassNode.name in EXCLUDED_ANNOTATIONS) && annotatedNode != annotationClassNode) {
                if(findAnnotationWithStereoType(annotationClassNode, stereotype) != null) {
                    return ann
                }
            }
        }
        if (annotatedNode instanceof MethodNode) {
            MethodNode method = (MethodNode) annotatedNode
            if (AstAnnotationUtils.findAnnotation(method, Override.class) != null) {
                MethodNode overridden = findOverriddenMethod(method)
                if (overridden != null) {
                    AnnotationNode ann = findAnnotationWithStereoType(overridden, stereotype)
                    if(ann != null) {
                        return ann
                    }
                    else {
                        return findAnnotationWithStereoType(overridden.declaringClass, stereotype)
                    }
                }
            }
        }
        return null
    }
    /**
     * Find all the annotations for the given stereotype
     *
     * @param annotatedNode The annotated node
     * @param stereotype The stereotype
     * @return A list of annotations
     */
    List<AnnotationNode> findAnnotationsWithStereoType(AnnotatedNode annotatedNode, Class<? extends Annotation> stereotype) {
        List<AnnotationNode> foundAnnotations = []
        findAnnotationsInternal(annotatedNode, stereotype.getName(), foundAnnotations)
        return foundAnnotations.unique()
    }
    /**
     * Find all the annotations for the given stereotype
     *
     * @param annotatedNode The annotated node
     * @param stereotype The stereotype
     * @return A list of annotations
     */
    List<AnnotationNode> findAnnotationsWithStereoType(AnnotatedNode annotatedNode, String stereotype) {
        List<AnnotationNode> foundAnnotations = []
        findAnnotationsInternal(annotatedNode, stereotype, foundAnnotations)
        return foundAnnotations.unique()
    }
    private void findAnnotationsInternal(AnnotatedNode annotatedNode, String stereotype, List<AnnotationNode>
            foundAnnotations) {
        AnnotationNode foundAnn = findAnnotationWithStereoType(annotatedNode, stereotype)
        if(foundAnn != null) {
            foundAnnotations.add(foundAnn)
        }
        List<AnnotationNode> annotations = annotatedNode.getAnnotations()
        for (AnnotationNode ann in annotations) {
            ClassNode annotationClassNode = ann.classNode
            if (findAnnotationWithStereoType(ann.classNode, stereotype) != null) {
                foundAnnotations.add(ann)
            } else if (!(annotationClassNode.name in EXCLUDED_ANNOTATIONS)) {
                findAnnotationsInternal(ann.classNode, stereotype, foundAnnotations)
            }
        }
    }

    private MethodNode findOverriddenMethod(MethodNode methodNode) {
        ClassNode classNode = methodNode.getDeclaringClass()

        String methodName = methodNode.name
        Parameter[] methodParameters = methodNode.parameters

        while(classNode != null && classNode.name != Object.name) {

            for(i in classNode.getAllInterfaces()) {
                MethodNode parent = i.getDeclaredMethod(methodName, methodParameters)
                if(parent != null) {
                    return parent
                }
            }
            classNode = classNode.superClass
            if(classNode != null && classNode.name != Object.name) {
                MethodNode parent = classNode.getDeclaredMethod(methodName, methodParameters)
                if(parent != null) {
                    return parent
                }
            }
        }

        return null
    }

    /**
     * Resolves all of the attribute values from an annotation of the given type
     * @param type The type
     * @param node The The node
     * @param annotationType The annotation type
     * @param <T> The {@link OptionalValues}
     * @return An {@link OptionalValues}
     */
    def <T> OptionalValues<T> resolveAttributesOfType(Class<T> type, AnnotatedNode node, String annotationType) {
        List<AnnotationNode> annotations = findAnnotationsWithStereoType(node, annotationType).reverse()
        if(!annotations.isEmpty()) {
            Map<CharSequence, T> values = [:]
            for(ann in annotations) {
                if(ann.classNode.name != annotationType) {
                    ann = AstAnnotationUtils.findAnnotation(ann.classNode, annotationType)
                }
                if(ann != null) {

                    for(entry in ann.members) {
                        def v = entry.value
                        if(v instanceof ConstantExpression) {
                            v = ((ConstantExpression)v).value
                        }
                        if(v != null && type.isInstance(v)) {
                            values.put(entry.key, (T)v )
                        }
                    }
                }
            }
            return OptionalValues.of(type, values)
        }
        return OptionalValues.empty()
    }

    @EqualsAndHashCode
    private static class Key {
        AnnotatedNode annotatedNode; String stereotype

        Key(AnnotatedNode annotatedNode, String stereotype) {
            this.annotatedNode = annotatedNode
            this.stereotype = stereotype
        }
    }

}