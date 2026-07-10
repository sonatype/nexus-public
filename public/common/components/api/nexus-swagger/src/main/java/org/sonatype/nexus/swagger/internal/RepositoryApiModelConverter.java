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

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.slf4j.LoggerFactory;

/**
 * ModelConverter to fix missing OpenAPI schema fields and incorrect examples for Repository API models.
 * Addresses missing format, type, and url fields in repository API schemas and corrects hardcoded "npm" examples.
 *
 * <p>
 * NEXUS-46395: migrated from Swagger 1.x ModelConverter SPI to OpenAPI 3.x:
 * <ul>
 * <li>{@code Model} \u2192 {@link Schema}, {@code ModelImpl} \u2192 {@link Schema} (no separate impl)</li>
 * <li>{@code Property}/{@code StringProperty} \u2192 {@link Schema}/{@link StringSchema}</li>
 * <li>Single {@code resolve(AnnotatedType, ...)} method replaces both {@code resolve(Type)}
 * and {@code resolveProperty(Type)} from Swagger 1.x.</li>
 * </ul>
 */
public class RepositoryApiModelConverter
    implements ModelConverter
{
  private static final String ABSTRACT_API_REPOSITORY_CLASS =
      "org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository";

  private static final org.slf4j.Logger log = LoggerFactory.getLogger(RepositoryApiModelConverter.class);

  @Override
  public Schema<?> resolve(
      final AnnotatedType annotatedType,
      final ModelConverterContext context,
      final Iterator<ModelConverter> chain)
  {
    Schema<?> schema = chain.hasNext() ? chain.next().resolve(annotatedType, context, chain) : null;

    if (schema != null && isRepositoryApiModel(annotatedType.getType())) {
      fixRepositoryApiModel(schema, annotatedType.getType());
    }

    return schema;
  }

  private boolean isRepositoryApiModel(final Type type) {
    String typeName = typeName(type);

    // Check if it's a repository API model by name pattern
    if (typeName.contains("ApiRepository") && !typeName.contains("Request")) {
      return true;
    }

    try {
      Class<?> clazz = Class.forName(typeName);
      return isAssignableFromAbstractApiRepository(clazz);
    }
    catch (ClassNotFoundException e) {
      return false;
    }
  }

  private static String typeName(final Type type) {
    if (type instanceof JavaType javaType) {
      return javaType.getRawClass().getName();
    }
    return type.getTypeName();
  }

  private boolean isAssignableFromAbstractApiRepository(final Class<?> clazz) {
    try {
      Class<?> abstractApiRepository = Class.forName(ABSTRACT_API_REPOSITORY_CLASS);
      return abstractApiRepository.isAssignableFrom(clazz);
    }
    catch (ClassNotFoundException e) {
      return false;
    }
  }

  private void fixRepositoryApiModel(final Schema<?> schema, final Type type) {
    String className = getSimpleClassName(typeName(type));
    String format = determineFormat(className);

    // Ensure format, type, and url fields are visible and have correct examples
    ensureFieldVisible(schema, "format", format);
    ensureFieldVisible(schema, "type", getTypeExample(className));
    ensureFieldVisible(schema, "url", "http://localhost:8081/repository/" + format + "-example");
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void ensureFieldVisible(final Schema<?> schema, final String fieldName, final String example) {
    if (schema.getProperties() == null) {
      schema.setProperties(new java.util.LinkedHashMap<>());
    }

    Schema property = schema.getProperties().get(fieldName);

    if (property == null) {
      // Add missing property
      StringSchema stringProperty = new StringSchema();
      stringProperty.setExample(example);
      stringProperty.setDescription(getFieldDescription(fieldName));
      ((java.util.Map) schema.getProperties()).put(fieldName, stringProperty);
    }
    else {
      // Update the existing property example
      property.setExample(example);
    }
  }

  private String getFieldDescription(final String fieldName) {
    return switch (fieldName) {
      case "format" -> "Component format held in this repository";
      case "type" -> "Repository type";
      case "url" -> "URL to the repository";
      default -> "";
    };
  }

  private String determineFormat(final String className) {
    // Try reflection-based format discovery first
    String format = getFormatFromReflection(className);
    if (format != null) {
      return format;
    }

    // Fallback to pattern extraction if reflection fails
    return extractFormatFromClassName(className);
  }

  private String getFormatFromReflection(final String className) {
    try {
      // Extract format prefix from class name (e.g., MavenHostedApiRepository -> Maven)
      String formatPrefix = className.replaceAll("(Hosted|Proxy|Group)ApiRepository$", "");

      // Try multiple Format class naming patterns dynamically
      String[] formatClassPatterns = {
          formatPrefix + "2Format",
          formatPrefix + "Format",
          formatPrefix.toLowerCase() + "Format",
          formatPrefix.toUpperCase() + "Format",
      };

      // Try each pattern until we find a working Format class
      for (String formatClassName : formatClassPatterns) {
        Class<?> formatClass = findFormatClass(formatClassName);
        if (formatClass != null) {
          Field nameField = formatClass.getField("NAME");
          return (String) nameField.get(null); // Get static field value
        }
      }
    }
    catch (Exception e) {
      log.debug("Failed to determine format for class {} using reflection: {}", className, e.getMessage(), e);
    }
    return null;
  }

  private Class<?> findFormatClass(final String formatClassName) {
    // Extract format name for package construction (e.g., Maven2Format -> maven)
    String formatPackage = formatClassName
        .replaceAll("(\\d+)?Format$", "") // Remove "Format" or "2Format" suffix
        .toLowerCase();

    // Try comprehensive package patterns dynamically
    String[] packagePatterns = {
        // OSS repository formats
        "org.sonatype.nexus.repository." + formatPackage + ".internal." + formatClassName,
        "org.sonatype.nexus.repository." + formatPackage + "." + formatClassName,

        // Pro repository formats
        "com.sonatype.nexus.repository." + formatPackage + ".internal." + formatClassName,
        "com.sonatype.nexus.repository." + formatPackage + "." + formatClassName,

        // Alternative internal package patterns
        "org.sonatype.nexus.repository." + formatPackage + ".internal." + formatPackage + "." + formatClassName,
        "com.sonatype.nexus.repository." + formatPackage + ".internal." + formatPackage + "." + formatClassName,

        // Direct class name attempts
        "org.sonatype.nexus.repository." + formatClassName,
        "com.sonatype.nexus.repository." + formatClassName,
    };

    for (String fullClassName : packagePatterns) {
      try {
        return Class.forName(fullClassName);
      }
      catch (ClassNotFoundException e) {
        log.debug("Format class not found: {}", fullClassName);
      }
    }
    return null;
  }

  private String extractFormatFromClassName(final String className) {
    // Fallback: extract format from ClassName and pattern
    String formatName = className
        .replaceAll("(Hosted|Proxy|Group)ApiRepository$", "")
        .toLowerCase();

    // Handle known special cases
    if ("maven".equals(formatName)) {
      return "maven2";
    }
    if ("golang".equals(formatName)) {
      return "go";
    }

    return formatName;
  }

  private String getTypeExample(final String className) {
    if (className.contains("Hosted")) {
      return "hosted";
    }
    else if (className.contains("Proxy")) {
      return "proxy";
    }
    else if (className.contains("Group")) {
      return "group";
    }
    return "hosted"; // default
  }

  private String getSimpleClassName(final String fullClassName) {
    return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
  }
}
