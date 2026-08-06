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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.swagger.SwaggerContributor;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fixes the OpenAPI response schemas for privilege GET operations (NEXUS-47972).
 *
 * <p>
 * The two GET operations in {@code PrivilegeApiResourceDoc} declare their return type as the
 * abstract {@code ApiPrivilege}, so the generated spec exposes only the 4 base fields. At runtime
 * the endpoint returns one of six concrete subtypes. This contributor patches the 200 response
 * schemas of both operations to use a {@code oneOf} union of those subtypes after scanning.
 *
 * <p>
 * A {@link io.swagger.v3.core.converter.ModelConverter} approach was intentionally avoided:
 * replacing the {@code ApiPrivilege} component schema with a {@code oneOf} creates circular
 * {@code allOf} references in the subtype schemas that inherit from {@code ApiPrivilege}.
 *
 * <p>
 * Cross-module subtypes are resolved via {@link Thread#getContextClassLoader()} (TCCL) to
 * work correctly in OSGi environments where the nexus-swagger bundle classloader cannot
 * directly see classes from nexus-repository-services or nexus-script-plugin.
 *
 * <p>
 * <strong>Adding a new privilege subtype:</strong> update both {@link #DEFAULT_SUBTYPE_FQCNS}
 * and {@link #TYPE_VALUE_BY_SIMPLE_NAME} together — one entry in each, keyed by the same
 * simple class name. The {@code type} value comes from the subtype's {@code PrivilegeDescriptor.TYPE}
 * constant. A contract test in {@code PrivilegeApiSwaggerContributorTest} asserts the two
 * maps are in sync.
 */
@Component
public class PrivilegeApiSwaggerContributor
    implements SwaggerContributor
{
  private static final Logger log = LoggerFactory.getLogger(PrivilegeApiSwaggerContributor.class);

  private static final String LIST_PATH = "/v1/security/privileges";

  private static final String SINGLE_PATH = "/v1/security/privileges/{privilegeName}";

  // Cross-module subtypes resolved at runtime via TCCL to work in OSGi environments.
  // Dependency direction: nexus-repository-services → nexus-security ← nexus-swagger,
  // so nexus-swagger cannot import the repository/script privilege classes at compile time.
  // When adding a new privilege subtype, also add its type-value entry to TYPE_VALUE_BY_SIMPLE_NAME.
  // Package-private for use by the test constructor.
  static final List<String> DEFAULT_SUBTYPE_FQCNS = List.of(
      "org.sonatype.nexus.security.privilege.rest.ApiPrivilegeApplication",
      "org.sonatype.nexus.security.privilege.rest.ApiPrivilegeWildcard",
      "org.sonatype.nexus.repository.security.rest.ApiPrivilegeRepositoryAdmin",
      "org.sonatype.nexus.repository.security.rest.ApiPrivilegeRepositoryView",
      "org.sonatype.nexus.repository.security.rest.ApiPrivilegeRepositoryContentSelector",
      "org.sonatype.nexus.script.plugin.internal.rest.ApiPrivilegeScript");

  // Discriminator type-value for each subtype keyed by simple class name.
  // Values are the runtime `type` field values (from each subtype's PrivilegeDescriptor.TYPE).
  // Keep in sync with DEFAULT_SUBTYPE_FQCNS: one entry per class, same simple name as key.
  // Discriminator mappings are built dynamically at runtime from only the subtypes that
  // actually resolve — so a missing entry here causes a silently absent mapping; a spurious
  // entry causes no harm (no resolved ref to attach it to).
  static final Map<String, String> TYPE_VALUE_BY_SIMPLE_NAME = Map.of(
      "ApiPrivilegeApplication", "application",
      "ApiPrivilegeWildcard", "wildcard",
      "ApiPrivilegeRepositoryAdmin", "repository-admin",
      "ApiPrivilegeRepositoryView", "repository-view",
      "ApiPrivilegeRepositoryContentSelector", "repository-content-selector",
      "ApiPrivilegeScript", "script");

  private final List<String> subtypeFqcns;

  public PrivilegeApiSwaggerContributor() {
    this(DEFAULT_SUBTYPE_FQCNS);
  }

  // Package-private constructor for testing with a controlled subtype list
  PrivilegeApiSwaggerContributor(final List<String> subtypeFqcns) {
    this.subtypeFqcns = subtypeFqcns;
  }

  @Override
  public void contribute(final OpenAPI openApi) {
    if (openApi.getPaths() == null) {
      return;
    }

    PathItem listPathItem = openApi.getPaths().get(LIST_PATH);
    PathItem singlePathItem = openApi.getPaths().get(SINGLE_PATH);

    if (listPathItem == null && singlePathItem == null) {
      return;
    }

    Supplier<Schema<?>> schemaSupplier = Suppliers.memoize(() -> buildPrivilegeUnionSchema(openApi));

    if (listPathItem != null && listPathItem.getGet() != null) {
      Schema<?> listOneOf = schemaSupplier.get();
      if (listOneOf != null) {
        patchListResponse(listPathItem.getGet(), listOneOf);
      }
    }

    if (singlePathItem != null && singlePathItem.getGet() != null) {
      Schema<?> singleOneOf = schemaSupplier.get();
      if (singleOneOf != null) {
        patchSingleResponse(singlePathItem.getGet(), singleOneOf);
      }
    }
  }

  private Schema<?> buildPrivilegeUnionSchema(final OpenAPI openApi) {
    List<Schema<?>> refs = new ArrayList<>();
    Discriminator discriminator = new Discriminator().propertyName("type");
    ClassLoader tccl = Thread.currentThread().getContextClassLoader();

    for (String fqcn : subtypeFqcns) {
      String simpleName = fqcn.substring(fqcn.lastIndexOf('.') + 1);
      try {
        Class<?> clazz = Class.forName(fqcn, true, tccl);
        simpleName = clazz.getSimpleName();
        ensureSchemaRegistered(openApi, clazz, simpleName);
        refs.add(new Schema<>().$ref("#/components/schemas/" + simpleName));
        // Only populate discriminator mapping for subtypes that actually resolved —
        // avoids dangling $ref entries in the discriminator when a plugin is absent.
        String typeValue = TYPE_VALUE_BY_SIMPLE_NAME.get(simpleName);
        if (typeValue != null) {
          discriminator.mapping(typeValue, "#/components/schemas/" + simpleName);
        }
      }
      catch (Exception | NoClassDefFoundError e) {
        // NoClassDefFoundError (missing transitive dep) or runtime failure in readAll()
        // must not abort spec generation for unrelated endpoints.
        log.debug("Failed to register privilege subtype schema for {}: {}", simpleName, e.getMessage(), e);
      }
    }

    if (refs.isEmpty()) {
      return null;
    }

    ComposedSchema oneOf = new ComposedSchema();
    refs.forEach(oneOf::addOneOfItem);
    oneOf.setDiscriminator(discriminator);
    return oneOf;
  }

  private static void ensureSchemaRegistered(final OpenAPI openApi, final Class<?> clazz, final String name) {
    if (openApi.getComponents() == null) {
      openApi.setComponents(new Components());
    }
    Components components = openApi.getComponents();
    if (components.getSchemas() == null) {
      components.setSchemas(new LinkedHashMap<>());
    }
    if (!components.getSchemas().containsKey(name)) {
      // putIfAbsent avoids overwriting schemas already registered by the JAX-RS scanner
      ModelConverters.getInstance()
          .readAll(clazz)
          .forEach(components.getSchemas()::putIfAbsent);
    }
  }

  private static void patchListResponse(final Operation op, final Schema<?> itemsSchema) {
    ApiResponse response200 = ensureResponse200(op);
    if (response200.getContent() == null) {
      response200.setContent(new Content()
          .addMediaType("application/json",
              new MediaType().schema(new ArraySchema().items(itemsSchema))));
    }
    else {
      response200.getContent().values().forEach(media -> {
        Schema<?> existing = media.getSchema();
        if (existing instanceof ArraySchema arraySchema) {
          arraySchema.setItems(itemsSchema);
        }
        else {
          media.setSchema(new ArraySchema().items(itemsSchema));
        }
      });
    }
  }

  private static void patchSingleResponse(final Operation op, final Schema<?> schema) {
    ApiResponse response200 = ensureResponse200(op);
    if (response200.getContent() == null) {
      response200.setContent(new Content()
          .addMediaType("application/json", new MediaType().schema(schema)));
    }
    else {
      response200.getContent().values().forEach(media -> media.setSchema(schema));
    }
  }

  private static ApiResponse ensureResponse200(final Operation op) {
    if (op.getResponses() == null) {
      op.setResponses(new ApiResponses());
    }
    return op.getResponses()
        .computeIfAbsent("200",
            k -> new ApiResponse().description("successful operation"));
  }
}
