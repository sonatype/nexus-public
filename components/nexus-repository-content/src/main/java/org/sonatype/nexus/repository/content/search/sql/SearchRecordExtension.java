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
package org.sonatype.nexus.repository.content.search.sql;

import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.search.sql.SearchRecord;

/**
 * Allows contribution of data from a component to {@link SearchRecord}
 *
 * This abstraction allows content to be contributed to a {@link SearchRecord}
 * from modules outside the nexus-repository-content (such as the nexus-tags-plugin module)
 *
 */
public interface SearchRecordExtension
{
  void contribute(final SearchRecord searchRecord, final FluentComponent component);
}
