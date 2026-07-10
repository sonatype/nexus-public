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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.swagger.SwaggerContributor;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * OpenAPI model component (formerly Swagger 1.x model).
 *
 * <p>
 * NEXUS-46395: migrated from Swagger 1.x to OpenAPI 3.x. Key changes:
 * <ul>
 * <li>{@code io.swagger.jaxrs.Reader} \u2192 {@link io.swagger.v3.jaxrs2.Reader}</li>
 * <li>{@code io.swagger.models.Swagger} \u2192 {@link OpenAPI}</li>
 * <li>{@code io.swagger.converter.ModelConverter#resolve(Type, ...)} \u2192
 * {@link ModelConverter#resolve(AnnotatedType, ModelConverterContext, Iterator)}</li>
 * <li>{@code Property} merged into {@link Schema} in OpenAPI 3.x; the
 * {@code resolveProperty()} method is gone (only {@code resolve()} now).</li>
 * <li>{@link Reader#read(Class)} returns the {@link OpenAPI} directly; we keep a
 * reference to it and apply contributors after each scan.</li>
 * </ul>
 */
@org.springframework.stereotype.Component
public class SwaggerModel
{
  private final ApplicationVersion applicationVersion;

  private final Reader reader;

  private final List<SwaggerContributor> contributors;

  @Autowired
  public SwaggerModel(
      final ApplicationVersion applicationVersion,
      final List<ModelConverter> converters,
      final List<SwaggerContributor> contributors)
  {
    this.applicationVersion = checkNotNull(applicationVersion);
    this.contributors = checkNotNull(contributors);

    registerConverters(converters);

    this.reader = new Reader(createOpenApi());
  }

  private static void registerConverters(final List<ModelConverter> converters) {
    ModelConverters instance = ModelConverters.getInstance();

    // filter banned types from model, such as Groovy's MetaClass
    instance.addConverter(new ModelFilter());

    // fix missing fields and incorrect examples in repository API models
    instance.addConverter(new RepositoryApiModelConverter());

    converters.forEach(instance::addConverter);
  }

  @EventListener
  public void on(final ContextRefreshedEvent event) {
    event.getApplicationContext()
        .getBeansOfType(Resource.class)
        .values()
        .stream()
        .map(Resource::getClass)
        .forEach(this::scan);
  }

  private void scan(final Class<? extends Resource> resourceClass) {
    // NEXUS-46395: restore Swagger 1.x's two behaviours that OpenAPI 3.x lost in the migration.
    //
    // 1. Skip resources without @Tag (was: skip without @Api). swagger-jaxrs 1.x's Reader skipped
    // classes with no @Api annotation by default (config.scanAllResources=false), which kept
    // /internal/* paths and other undocumented resources out of the public OpenAPI spec.
    // swagger-jaxrs2 has no equivalent default; everything with @Path is included. Replicate
    // the prior filter explicitly.
    //
    // 2. Find @Tag through the full type hierarchy. swagger-jaxrs2 2.2.35's
    // ReflectionUtils.getRepeatableAnnotationsArray walks the implemented interfaces but
    // returns the FIRST interface's result, even when it's an empty array - so for
    // `class Foo implements Resource, FooDoc` (with @Tag only on FooDoc), the @Tag from FooDoc
    // is never seen because Resource.class returns first with an empty array. This is why
    // e.g. /v1/repositories/{repositoryName} (RepositoriesResource implements ...DocResource)
    // landed under the "default" tag while /v1/repositories/raw/hosted (RawHostedRepositories-
    // ApiResource has @Tag directly on the class) was correctly grouped under "Repository
    // Management". We collect tags ourselves through a full transitive walk and, after the
    // Reader runs, apply them to any operation it left tagless.
    Set<String> classTags = collectTags(resourceClass);
    if (classTags.isEmpty()) {
      return;
    }

    // Snapshot identity of all operations already in the document so we can identify the
    // operations contributed by *this* resource's read() call.
    Set<Operation> preExistingOps = collectAllOperations(getOpenApi());

    reader.read(resourceClass);

    // Apply our resolved class-level tags to any newly-registered operation that came back tagless.
    Paths paths = getOpenApi().getPaths();
    if (paths != null) {
      for (PathItem item : paths.values()) {
        for (Operation op : item.readOperations()) {
          if (preExistingOps.contains(op)) {
            continue;
          }
          if (op.getTags() == null || op.getTags().isEmpty()) {
            op.setTags(new ArrayList<>(classTags));
          }
        }
      }
    }

    contributors.forEach(c -> c.contribute(getOpenApi()));
  }

  /**
   * Collect all {@link Tag#name() tag names} declared anywhere in the class's transitive type
   * hierarchy (the class itself, every superclass up to {@code Object}, and every implemented
   * interface plus its super-interfaces). Replaces swagger-jaxrs2's buggy interface walk.
   */
  private static Set<String> collectTags(final Class<?> cls) {
    Set<String> tags = new LinkedHashSet<>();
    collectTags(cls, tags, new HashSet<>());
    return tags;
  }

  private static void collectTags(final Class<?> cls, final Set<String> out, final Set<Class<?>> seen) {
    if (cls == null || cls == Object.class || !seen.add(cls)) {
      return;
    }
    for (Tag t : cls.getAnnotationsByType(Tag.class)) {
      String name = t.name();
      if (name != null && !name.isEmpty()) {
        out.add(name);
      }
    }
    collectTags(cls.getSuperclass(), out, seen);
    for (Class<?> iface : cls.getInterfaces()) {
      collectTags(iface, out, seen);
    }
  }

  private static Set<Operation> collectAllOperations(final OpenAPI openApi) {
    Paths paths = openApi.getPaths();
    if (paths == null) {
      return Collections.emptySet();
    }
    Set<Operation> ops = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    for (PathItem item : paths.values()) {
      ops.addAll(item.readOperations());
    }
    return ops;
  }

  public OpenAPI getOpenApi() {
    return reader.getOpenAPI();
  }

  private OpenAPI createOpenApi() {
    return new OpenAPI().info(new Info()
        .title("Nexus Repository Manager REST API")
        .version(applicationVersion.getVersion()));
  }

  /**
   * NEXUS-46395: ModelConverter SPI redesigned in OpenAPI 3.x. Single resolve(AnnotatedType)
   * method replaces both resolve(Type) and resolveProperty(Type) from Swagger 1.x.
   */
  private static class ModelFilter
      implements ModelConverter
  {
    private static final Set<String> BANNED_TYPE_NAMES = Set.of(
        "[simple type, class groovy.lang.MetaClass]" // groovy's MetaClass typeName
    );

    @Override
    public Schema<?> resolve(
        final AnnotatedType annotatedType,
        final ModelConverterContext context,
        final Iterator<ModelConverter> chain)
    {
      Type type = annotatedType.getType();
      String typeName;
      if (type instanceof JavaType) {
        typeName = type.toString();
      }
      else {
        typeName = type.getTypeName();
      }
      if (!BANNED_TYPE_NAMES.contains(typeName) && chain.hasNext()) {
        return chain.next().resolve(annotatedType, context, chain);
      }
      return null;
    }
  }
}
