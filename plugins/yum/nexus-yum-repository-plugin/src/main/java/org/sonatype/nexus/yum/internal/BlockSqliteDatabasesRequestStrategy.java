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
package org.sonatype.nexus.yum.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.repository.AbstractRequestStrategy;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RequestStrategy;
import org.sonatype.nexus.yum.Yum;

/**
 * A {@link RequestStrategy} that applies to yum enabled repositories blocks access to sqlite databases.
 *
 * @since 2.11
 */
@Named
@Singleton
public class BlockSqliteDatabasesRequestStrategy
    extends AbstractRequestStrategy
{

  @Override
  public void onHandle(final Repository repository, final ResourceStoreRequest request, final Action action)
      throws ItemNotFoundException, IllegalOperationException
  {
    if (request.getRequestPath().matches("/" + Yum.PATH_OF_REPODATA + "/.*\\.sqlite\\.bz2")) {
      throw new ItemNotFoundException(
          ItemNotFoundException.reasonFor(request, repository, "Yum sqlite metadata databases are not supported")
      );
    }
  }

}
