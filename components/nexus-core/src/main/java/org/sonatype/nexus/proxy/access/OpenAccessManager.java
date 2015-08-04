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
package org.sonatype.nexus.proxy.access;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * A simple AccessManager implementation that allows everybody to access the Proximity core. This is the default imple,
 * does nothing.
 *
 * @author t.cservenak
 */
@Named("open")
@Singleton
public class OpenAccessManager
    implements AccessManager
{
  public void decide(Repository repository, ResourceStoreRequest request, Action action)
      throws AccessDeniedException
  {
    // this access manager is open, everybody has access to everything since
    // it never throws AccessDeniedEx
  }
}
