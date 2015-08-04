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
package org.sonatype.nexus.proxy.item.uid;

import org.sonatype.nexus.proxy.item.RepositoryItemUid;

/**
 * Core component doing "aggregation" of attribute sources, possibly contributed by plugins.
 *
 * @author cstamas
 */
public interface RepositoryItemUidAttributeManager
{
  /**
   * Returns the attribute belonging to passed in key or {@code null} if no such attribute.
   */
  <T extends Attribute<?>> T getAttribute(Class<T> attributeKey, RepositoryItemUid subject);

  /**
   * Causes to recollect all registered attributes.
   */
  void reset();
}
