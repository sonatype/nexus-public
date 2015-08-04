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

import org.sonatype.nexus.proxy.item.RepositoryItemUid;

public class IsHiddenAttribute
    implements Attribute<Boolean>
{
  public Boolean getValueFor(final RepositoryItemUid subject) {
    if (subject.getBooleanAttributeValue(IsMetacontentAttribute.class)) {
      // metacontent is hidden
      return true;
    }
    else if (subject.getPath() != null
        && (subject.getPath().indexOf("/.") > -1 || subject.getPath().startsWith("."))) {
      // paths that start with a . in any directory (or filename)
      // are considered hidden.
      // This check will catch (for example):
      // .metadata
      // /.meta/something.jar
      // /something/else/.hidden/something.jar
      return true;
    }
    else {
      return false;
    }
  }
}
