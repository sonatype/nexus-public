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
package org.sonatype.nexus.repository.replication;

import java.util.List;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Context;

/**
 * Contains any per-format customizations needed for pull replication. Not all formats will need this.
 */
public interface PullReplicationSupport
{
  String IS_REPLICATION_REQUEST = "IS_REPLICATION_REQUEST";

  List<String> translatePaths(final Repository repository, final List<String> paths);

  static boolean isReplicationRequest(final Context context) {
    if (context != null && context.getAttributes() != null) {
      return Boolean.TRUE.equals(context.getAttributes().get(IS_REPLICATION_REQUEST, Boolean.class, false));
    }
    return false;
  }
}
