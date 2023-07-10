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
package org.sonatype.nexus.repository.content.fluent;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Continuation;

/**
 * Fluent API to count/browse elements in a repository.
 *
 * @since 3.26
 */
public interface FluentQuery<T>
{
  /**
   * Count the elements in the repository that match the current query.
   */
  int count();

  /**
   * Browse through elements in the repository that match the current query.
   */
  Continuation<T> browse(int limit, @Nullable String continuationToken);

  Continuation<T> browseEager(int limit, @Nullable String continuationToken);
}
