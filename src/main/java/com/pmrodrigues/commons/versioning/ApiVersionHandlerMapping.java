package com.pmrodrigues.commons.versioning;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

/**
 * Custom {@link RequestMappingHandlerMapping} that attaches an {@link ApiVersionRequestCondition}
 * to every controller class or handler method annotated with {@link ApiVersion}.
 *
 * <p>Registered via {@code WebMvcRegistrations} so it replaces the default Spring Boot handler
 * mapping while preserving all standard behaviour (path variables, produces/consumes, etc.).
 * Controllers without {@link ApiVersion} are unaffected and continue to work normally.
 */
public class ApiVersionHandlerMapping extends RequestMappingHandlerMapping {

    /**
     * Returns an {@link ApiVersionRequestCondition} for classes annotated with {@link ApiVersion},
     * or {@code null} for unannotated classes.
     *
     * @param handlerType the controller class to inspect
     * @return a version condition or {@code null}
     */
    @Override
    protected ApiVersionRequestCondition getCustomTypeCondition(Class<?> handlerType) {
        ApiVersion ann = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
        return ann != null ? new ApiVersionRequestCondition(ann.value()) : null;
    }

    /**
     * Returns an {@link ApiVersionRequestCondition} for methods annotated with {@link ApiVersion},
     * or {@code null} for unannotated methods. A method-level annotation overrides the class-level one.
     *
     * @param method the handler method to inspect
     * @return a version condition or {@code null}
     */
    @Override
    protected ApiVersionRequestCondition getCustomMethodCondition(Method method) {
        ApiVersion ann = AnnotationUtils.findAnnotation(method, ApiVersion.class);
        return ann != null ? new ApiVersionRequestCondition(ann.value()) : null;
    }
}
