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
package org.sonatype.nexus.testsuite.testsupport.system.repository.config;

import org.sonatype.nexus.repository.Repository;

public interface RepositoryConfig<THIS>
{
  THIS withName(final String name);

  String getName();

  String getRecipe();

  String getFormat();

  THIS withBlobstore(final String blobstore);

  String getBlobstore();

  THIS withDatastoreName(final String datastoreName);

  String getDatastoreName();

  THIS withOnline(final Boolean online);

  Boolean isOnline();

  THIS withStrictContentTypeValidation(final Boolean strictContentTypeValidation);

  Boolean isStrictContentTypeValidation();

  Repository create();
}
