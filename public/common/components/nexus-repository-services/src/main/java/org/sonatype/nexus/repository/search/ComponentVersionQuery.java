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

import javax.annotation.Nullable;

/**
 * A request for one page of a component's distinct versions.
 *
 * @param format the component format, required
 * @param namespace the component namespace, may be null for formats without one
 * @param name the component name, required
 * @param versionFilter optional version substring filter
 * @param page zero-based page index
 * @param size page size
 * @param sort sort key: version, lastUpdated, or repositories
 * @param direction sort direction
 */
public record ComponentVersionQuery(
    String format,
    @Nullable String namespace,
    String name,
    @Nullable String versionFilter,
    int page,
    int size,
    String sort,
    SortDirection direction)
{
}
