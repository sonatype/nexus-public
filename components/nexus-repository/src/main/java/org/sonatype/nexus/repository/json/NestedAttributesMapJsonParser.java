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

import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;

import com.fasterxml.jackson.core.JsonParser;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * {@link JsonParser} that holds state for communicating with a {@link NestedAttributesMap}. This mostly will be
 * valuable for parsing and merging multiple json objects into a give {@link NestedAttributesMap}
 *
 * @since 3.next
 */
public class NestedAttributesMapJsonParser
    extends CurrentPathJsonParser
{
  private final NestedAttributesMap root;

  private boolean mappingInsideArray;

  private boolean defaultMapping;

  public NestedAttributesMapJsonParser(final JsonParser jsonParser, final NestedAttributesMap root) {
    super(checkNotNull(jsonParser));
    this.root = checkNotNull(root);
  }

  /**
   * Attempt to retrieve the {@link NestedAttributesMap} that is associated with
   * the current path, if parts of the path are not existing they will
   * be manually created by the {@link NestedAttributesMap#child(String)} for that path part.
   *
   * @return NestedAttributesMap or null if we are mapping inside an array.
   */
  @Nullable
  public NestedAttributesMap getChildFromRoot() {
    // if we are inside an array we don't have a map value to return
    if (isMappingInsideArray()) {
      return null;
    }

    // we start with the root as initial existing child to go down it's path
    NestedAttributesMap existingChild = root;

    // remove the first "/" and then split on any leftover
    for (String part : currentPathInParts()) {
      Object o = existingChild.get(part);

      // only create and/or retrieve other child json objects,
      if (isNull(o) || o instanceof Map) {
        existingChild = existingChild.child(part);
      }
    }

    return existingChild;
  }

  /**
   * Inform parser that the current position is inside an array.
   */
  public void markMappingInsideArray() {
    mappingInsideArray = true;
  }

  /**
   * Inform parser that the current position has existed an array.
   */
  public void unMarkMappingInsideArray() {
    mappingInsideArray = false;
  }

  /**
   * Inform parser that no special mapping should occur but the default Jackson mapping should be followed.
   */
  public void enableDefaultMapping() {
    defaultMapping = true;
  }

  /**
   * Inform parser that special mapping should occur over using the default Jackson mapping.
   */
  public void disableDefaultMapping() {
    defaultMapping = false;
  }

  public NestedAttributesMap getRoot() {
    return root;
  }

  public boolean isMappingInsideArray() {
    return mappingInsideArray;
  }

  public boolean isDefaultMapping() {
    return defaultMapping;
  }
}
