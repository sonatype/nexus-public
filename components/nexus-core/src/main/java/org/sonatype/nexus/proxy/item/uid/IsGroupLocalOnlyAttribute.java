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
package org.sonatype.nexus.proxy.item.uid;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;

/**
 * Attribute yielding "true" for paths (and it's subpaths) belonging -- or referred as -- "not subject of group merge"
 * or simply "group local" content. Used by Group repository implementations, to decide should process member
 * repositories too, or just group's locol storage. Logically equivalent to
 * {@link ResourceStoreRequest#isRequestGroupLocalOnly()}, but while that allows per-request control, this one as
 * attribute allows programmatical control over it too.
 *
 * @author cstamas
 */
public class IsGroupLocalOnlyAttribute
    implements Attribute<Boolean>
{
  public Boolean getValueFor(RepositoryItemUid subject) {
    // stuff being group-local
    // /.meta
    // /.index
    // /.nexus
    // we are specific about these for a good reason (see future)

    if (subject.getPath() != null) {
      return subject.getPath().startsWith("/.meta") || subject.getPath().startsWith("/.index")
          || subject.getPath().startsWith("/.nexus");
    }
    else {
      return false;
    }
  }
}
