/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.datastore.mybatis;

import java.lang.annotation.Annotation;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

/**
 * Introspector that overrides {@link JsonIgnoreType} annotation for specific types.
 */
public class OverrideIgnoreTypeIntrospector
    extends JacksonAnnotationIntrospector
{
  private final List<Class<?>> overriddenTypes;

  public OverrideIgnoreTypeIntrospector(final List<Class<?>> overriddenTypes) {
    super();
    this.overriddenTypes = overriddenTypes;
  }

  @Override
  protected <A extends Annotation> A _findAnnotation(final Annotated ann, final Class<A> annoClass) {
    if (annoClass == JsonIgnoreType.class && shouldOverride(ann.getRawType())) {
      return null;
    }

    return super._findAnnotation(ann, annoClass);
  }

  private boolean shouldOverride(final Class<?> type) {
    return overriddenTypes.stream().anyMatch(overriddenType -> overriddenType.isAssignableFrom(type));
  }
}

