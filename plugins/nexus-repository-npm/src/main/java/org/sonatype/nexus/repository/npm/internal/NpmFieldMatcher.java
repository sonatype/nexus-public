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
package org.sonatype.nexus.repository.npm.internal;

import java.io.IOException;
import java.util.regex.Pattern;

import org.sonatype.nexus.repository.json.CurrentPathJsonParser;
import com.fasterxml.jackson.core.JsonParser;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;
import static java.util.regex.Pattern.compile;

/**
 * A NPM Json Field Matcher
 *
 * @since 3.16
 */
public class NpmFieldMatcher
{
  private final String fieldName;

  private final Pattern pattern;

  private final NpmFieldDeserializer deserializer;

  /**
   * Constructor.
   *
   * @param fieldName String exact field name, e.g. : "tarball"
   * @param pathRegex String that matches by regex, e.g. : "/versions/(.*)/dist/tarball"
   */
  public NpmFieldMatcher(final String fieldName, final String pathRegex, final NpmFieldDeserializer deserializer) {
    this.fieldName = checkNotNull(fieldName);
    this.pattern = compile(checkNotNull(pathRegex));
    this.deserializer = checkNotNull(deserializer);
  }

  /**
   * Indicate whether the field matcher is allowed to be deserialized if it matched.
   *
   * @return true by default, implementers can override default behaviour.
   */
  public boolean allowDeserializationOnMatched() {
    return true;
  }

  /**
   * Test if the {@link #fieldName} and {@link #pattern} matches the current path on the {@link JsonParser}
   *
   * @param parser {@link JsonParser}
   * @return true if the field name and regex matches the current path on the {@link JsonParser}, false otherwise.
   */
  public boolean matches(final JsonParser parser) throws IOException {
    String currentName = parser.getCurrentName();
    // match on field name first, as it's much faster then matching on path, we eliminate fields that don't match fast.
    return nonNull(currentName) && matchesFieldName(currentName) && matchesPath(parser);
  }

  private boolean matchesFieldName(final String fieldName) {
    return this.fieldName.endsWith(fieldName);
  }

  private boolean matchesPath(final JsonParser parser) {
    if (parser instanceof CurrentPathJsonParser) {
      return matchesPath(((CurrentPathJsonParser) parser).currentPath());
    }

    return false;
  }

  private boolean matchesPath(final String path) {
    return this.pattern.matcher(path).matches();
  }

  public NpmFieldDeserializer getDeserializer() {
    return deserializer;
  }

  public String getFieldName() {
    return fieldName;
  }
}
