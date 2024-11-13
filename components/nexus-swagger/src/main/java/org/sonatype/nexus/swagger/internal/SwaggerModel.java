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
package org.sonatype.nexus.swagger.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.rest.Component;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.swagger.SwaggerContributor;

import com.google.common.collect.ImmutableSet;
import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.converter.ModelConverters;
import io.swagger.jaxrs.Reader;
import io.swagger.models.Info;
import io.swagger.models.Model;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Swagger model component.
 *
 * @since 3.3
 */
@Named
@Singleton
public class SwaggerModel
    implements Component
{
  private final ApplicationVersion applicationVersion;

  private final Reader reader;

  private final List<SwaggerContributor> contributors;

  @Inject
  public SwaggerModel(final ApplicationVersion applicationVersion,
                      final List<SwaggerContributor> contributors)
  {
    this.applicationVersion = checkNotNull(applicationVersion);
    this.contributors = checkNotNull(contributors);

    // filter banned types from model, such as Groovy's MetaClass
    ModelConverters.getInstance().addConverter(new ModelFilter());

    this.reader = new Reader(createSwagger());
  }

  public void scan(final Class<Resource> resourceClass) {
    reader.read(resourceClass);
    contributors.forEach(c -> c.contribute(getSwagger()));
  }

  public Swagger getSwagger() {
    return reader.getSwagger();
  }

  private Swagger createSwagger() {
    return new Swagger().info(new Info()
        .title("Nexus Repository Manager REST API")
        .version(applicationVersion.getVersion()));
  }

  private static class ModelFilter
      implements ModelConverter
  {
    private static final Set<String> BANNED_TYPE_NAMES = ImmutableSet.of(
        "[simple type, class groovy.lang.MetaClass]" // groovy's MetaClass typeName
    );

    @Override
    public Model resolve(final Type type,
                         final ModelConverterContext context,
                         final Iterator<ModelConverter> chain)
    {
      if (!BANNED_TYPE_NAMES.contains(type.getTypeName()) && chain.hasNext()) {
        return chain.next().resolve(type, context, chain);
      }
      return null;
    }

    @Override
    public Property resolveProperty(final Type type,
                                    final ModelConverterContext context,
                                    final Annotation[] annotations,
                                    final Iterator<ModelConverter> chain)
    {
      if (!BANNED_TYPE_NAMES.contains(type.getTypeName()) && chain.hasNext()) {
        return chain.next().resolveProperty(type, context, annotations, chain);
      }
      return null;
    }
  }
}
