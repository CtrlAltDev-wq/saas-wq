package com.wq.framework.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method whose invocation must be written to the tenant audit trail.
 *
 * <p>The annotation only carries the business taxonomy; the aspect resolves the actor,
 * the tenant, the request metadata and the outcome.</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    /**
     * Business action identifier, for example {@code CUSTOMER_CREATE}.
     *
     * @return action identifier
     */
    String action();

    /**
     * Affected resource, for example {@code CUSTOMER}.
     *
     * @return resource identifier
     */
    String resource();
}
