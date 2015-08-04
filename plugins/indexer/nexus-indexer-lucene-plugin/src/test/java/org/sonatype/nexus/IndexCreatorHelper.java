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
package org.sonatype.nexus;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.index.context.IndexCreator;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Creators that can be used in tests.
 */
public class IndexCreatorHelper
{
  private List<IndexCreator> m_fullCreators = new ArrayList<IndexCreator>();

  private List<IndexCreator> m_defaultCreators = new ArrayList<IndexCreator>();

  private List<IndexCreator> m_minCreators = new ArrayList<IndexCreator>();

  public IndexCreatorHelper(PlexusContainer testCaseContainer)
      throws ComponentLookupException
  {
    IndexCreator min = testCaseContainer.lookup(IndexCreator.class, "min");
    IndexCreator jar = testCaseContainer.lookup(IndexCreator.class, "jarContent");

    m_minCreators.add(min);

    m_fullCreators.add(min);
    m_fullCreators.add(jar);

    m_defaultCreators.addAll(m_fullCreators);
  }

  public List<IndexCreator> getDefaultCreators() {
    return m_defaultCreators;
  }

  public List<IndexCreator> getFullCreators() {
    return m_fullCreators;
  }

  public List<IndexCreator> getMinCreators() {
    return m_minCreators;
  }
}
