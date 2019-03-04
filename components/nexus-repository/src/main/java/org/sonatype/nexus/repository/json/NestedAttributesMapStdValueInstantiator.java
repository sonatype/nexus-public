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

import org.sonatype.nexus.common.collect.NestedAttributesMap;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of {@link StdValueInstantiator} that forces the providing and using of one root {@link
 * NestedAttributesMap}, this allows jackson {@link ObjectMapper}'s that deserialize into maps to always use
 * the shared map.
 *
 * @since 3.next
 */
public class NestedAttributesMapStdValueInstantiator
    extends StdValueInstantiator
{
  private final NestedAttributesMap root;

  public NestedAttributesMapStdValueInstantiator(final StdValueInstantiator src, final NestedAttributesMap root) {
    super(checkNotNull(src));
    this.root = checkNotNull(root);
  }

  /**
   * Overwritten to always pass back one and the same map for adding to
   */
  @Override
  public Object createUsingDefault(final DeserializationContext context) {
    return root.backing();
  }
}
