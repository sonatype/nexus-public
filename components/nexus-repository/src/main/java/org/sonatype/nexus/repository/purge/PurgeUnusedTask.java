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
package org.sonatype.nexus.repository.purge;

import javax.inject.Named;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;

/**
 * Task to purge unused components/assets of given repository.
 *
 * @since 3.0
 */
@Named
public class PurgeUnusedTask
    extends RepositoryTaskSupport
{
  public static final String LAST_USED_FIELD_ID = "lastUsed";

  @Override
  protected void execute(final Repository repository) {
    repository.facet(PurgeUnusedFacet.class).purgeUnused(getConfiguration().getInteger(LAST_USED_FIELD_ID, -1));
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    return repository.optionalFacet(PurgeUnusedFacet.class).isPresent();
  }

  @Override
  public String getMessage() {
    return "Purge unused components and assets from " + getRepositoryField();
  }
}
