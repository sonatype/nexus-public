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
package org.sonatype.nexus.testsuite.testsupport.raw;

import java.io.IOException;

import javax.inject.Inject;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.testsuite.testsupport.system.RestTestHelper;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class RawTestHelper
{
  @Inject
  private RestTestHelper rest;

  /**
   * Create a {@link RawClient} for the given repository using the provided authentication.
   */
  public RawClient rawClient(final Repository repository, final String username, final String password) throws Exception {
    checkNotNull(repository);
    return rawClient("/repository/" + repository.getName() + "/", username, password);
  }

  public RawClient rawClient(final String path, final String username, final String password) throws Exception {
    return new RawClient(
        rest.client(path, username, password),
        rest.clientContext(),
        rest.resolveNexusPath(path)
    );
  }

  public abstract Content read(Repository repository, String path) throws IOException;

  public abstract void assertRawComponent(Repository repository, String path, String group);

  public abstract EntityId createAsset(Repository repository, String componentName, String componentGroup, String assetName);
}
