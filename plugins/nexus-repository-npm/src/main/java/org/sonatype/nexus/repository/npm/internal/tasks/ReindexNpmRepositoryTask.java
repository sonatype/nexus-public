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
package org.sonatype.nexus.repository.npm.internal.tasks;

import org.sonatype.nexus.scheduling.Task;

/**
 * Task that reindexes npm proxy and hosted repositories by opening each tarball and extracting the contents of the
 * {@code package.json} as format attributes. This task is necessary to "upgrade" existing npm repositories to contain
 * the search-indexed format attributes necessary for npm v1 search.
 *
 * @since 3.7
 */
public interface ReindexNpmRepositoryTask
    extends Task
{
  public static final String NPM_V1_SEARCH_UNSUPPORTED = "npm_v1_search_unsupported";
}
