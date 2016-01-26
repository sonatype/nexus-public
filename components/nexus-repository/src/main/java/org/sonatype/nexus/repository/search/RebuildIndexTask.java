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
package org.sonatype.nexus.repository.search;

import javax.inject.Named;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;

/**
 * Internal task to rebuild index of given repository.
 *
 * @since 3.0
 */
@Named
public class RebuildIndexTask
    extends RepositoryTaskSupport
{

  @Override
  protected void execute(final Repository repository) {
    repository.facet(SearchFacet.class).rebuildIndex();
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    return repository.optionalFacet(SearchFacet.class).isPresent();
  }

  @Override
  public String getMessage() {
    return "Rebuilding index of " + getRepositoryField();
  }

}
