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
package org.sonatype.nexus.repository.npm.internal.orient

import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.group.GroupFacet
import org.sonatype.nexus.repository.group.GroupFacetImpl
import org.sonatype.nexus.repository.group.GroupHandler
import org.sonatype.nexus.repository.npm.internal.orient.OrientNpmGroupFacet
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Response

import static org.sonatype.nexus.repository.http.HttpStatus.OK
import static org.sonatype.nexus.repository.group.GroupHandler.DispatchedRepositories

/**
 * @since 3.19
 */
class OrientNpmGroupHandler
    extends GroupHandler
{
  protected Map<Repository, Response> getResponses(final Context context,
                                                   final DispatchedRepositories dispatched,
                                                   final OrientNpmGroupFacet groupFacet)
  {
    // get all and filter for HTTP OK responses
    return getAll(context, groupFacet.members(), dispatched).findAll { k, v -> v.status.code == OK }
  }

  protected GroupFacetImpl getGroupFacet(final Context context) {
    return context.getRepository().facet(GroupFacet.class) as GroupFacetImpl
  }
}
