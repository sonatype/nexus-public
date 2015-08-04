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
package org.sonatype.nexus.proxy.item;

import java.util.List;

/**
 * The Interface StorageCompositeItem. These items are "composed" from multiple other items, like merged
 * or processed in some other way.
 */
public interface StorageCompositeItem
    extends StorageItem
{
  /**
   * Returns unmodifiable list of sources that makes up this composite item. The order is important, as it is the order used in composition
   * (actually depends on actual operation used to create this item.
   */
  List<StorageItem> getSources();
}
