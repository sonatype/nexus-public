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
package org.sonatype.nexus.repository.npm.internal

import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.group.GroupFacet
import org.sonatype.nexus.repository.group.GroupHandler
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Response

import static org.sonatype.nexus.repository.http.HttpConditions.makeConditional
import static org.sonatype.nexus.repository.http.HttpConditions.makeUnconditional
import static org.sonatype.nexus.repository.http.HttpStatus.OK
import static org.sonatype.nexus.repository.group.GroupHandler.DispatchedRepositories

/**
 * @since 3.next
 */
class NpmGroupHandler
    extends GroupHandler
{
  protected Map<Repository, Response> getResponses(final Context context,
                                                   final DispatchedRepositories dispatched,
                                                   final NpmGroupFacet groupFacet)
  {
    // Remove conditional headers before making "internal" requests: https://issues.sonatype.org/browse/NEXUS-13915
    makeUnconditional(context.getRequest())

    try {
      // get all and filter for HTTP OK responses
      return getAll(context, groupFacet.members(), dispatched).findAll { k, v -> v.status.code == OK }
    }
    finally {
      makeConditional(context.getRequest())
    }
  }

  protected NpmGroupFacet getGroupFacet(final Context context) {
    return context.getRepository().facet(GroupFacet.class) as NpmGroupFacet
  }
}
