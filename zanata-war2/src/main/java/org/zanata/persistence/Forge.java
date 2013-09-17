package org.zanata.persistence;

import javax.inject.Qualifier;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;

@Qualifier
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER,
      ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Forge
{
}