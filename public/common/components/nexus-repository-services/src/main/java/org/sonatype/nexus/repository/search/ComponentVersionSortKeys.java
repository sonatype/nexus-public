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
package org.sonatype.nexus.repository.search;

import java.util.Set;

/**
 * Canonical sort keys for the version-browsing API.
 *
 * <p>
 * This is the single source of truth. The resource layer validates against these keys,
 * and the SQL layer maps from them to SQL expressions.
 */
public final class ComponentVersionSortKeys
{
  private ComponentVersionSortKeys() {
  }

  public static final String VERSION = "version";

  public static final String LAST_UPDATED = "lastUpdated";

  public static final String REPOSITORIES = "repositories";

  public static final Set<String> ALL = Set.of(VERSION, LAST_UPDATED, REPOSITORIES);
}
