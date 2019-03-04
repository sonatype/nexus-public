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
package org.sonatype.nexus.repository.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonStreamContext;

import static com.fasterxml.jackson.core.JsonPointer.forPath;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Decorator class of a {@link JsonParser} allowing for maintaining and tracking the current path.
 *
 * @since 3.next
 */
public class CurrentPathJsonParser
    extends JsonParserDecorator
{
  private static final String JSON_PATH_SEPARATOR = "/";

  public CurrentPathJsonParser(final JsonParser jsonParser) {
    super(jsonParser);
  }

  /**
   * Retrieve path of current spot of the {@link JsonParser}.
   *
   * @return String of current path
   */
  public String currentPath() {
    return currentPath(currentPointer());
  }

  /**
   * Retrieve path of current spot of the {@link JsonParser}, split by their path separator.
   *
   * @return String of current path
   */
  public String[] currentPathInParts() {
    return currentPath().substring(1).split(JSON_PATH_SEPARATOR);
  }

  /**
   * Retrieve pointer of current spot of the {@link JsonParser}, for fast parsing prevent calling this often as this
   * can potentially do a scan through the complete json object. Depending what the current point is it will
   * go traverse back up the path to the right path. See also {@link JsonPointer#forPath(JsonStreamContext, boolean)}
   *
   * @see JsonPointer#forPath(JsonStreamContext, boolean)
   * @return JsonPointer
   */
  public JsonPointer currentPointer() {
    return forPath(this.getParsingContext(), false);
  }

  /**
   * Return the current path of a {@link JsonPointer} or if no
   * path (like root) return {@link #JSON_PATH_SEPARATOR}.
   *
   * @param pointer JsonPointer
   * @return String of current path
   */
  private String currentPath(final JsonPointer pointer) {
    return toValidPath(pointer.toString());
  }

  private String toValidPath(final String path) {
    return isBlank(path) ? JSON_PATH_SEPARATOR : path;
  }
}
