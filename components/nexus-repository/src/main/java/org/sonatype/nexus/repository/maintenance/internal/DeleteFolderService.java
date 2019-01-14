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
package org.sonatype.nexus.repository.maintenance.internal;

import java.util.function.BooleanSupplier;

import org.sonatype.nexus.repository.Repository;

import org.joda.time.DateTime;

/**
 * Deletes all assets under a given path, that were created before a given timestamp, in a repository based on
 * a user's privileges.
 *
 * @since 3.15
 */
public interface DeleteFolderService
{
  void deleteFolder(Repository repository, String path, DateTime timestamp, BooleanSupplier cancelledCheck);
}
