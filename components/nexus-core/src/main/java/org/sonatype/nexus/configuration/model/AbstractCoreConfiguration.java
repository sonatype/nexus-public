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
package org.sonatype.nexus.configuration.model;

import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;

public abstract class AbstractCoreConfiguration<C>
    extends AbstractRevertableConfiguration<C>
    implements CoreConfiguration<C>
{
  private ApplicationConfiguration applicationConfiguration;

  public AbstractCoreConfiguration(ApplicationConfiguration applicationConfiguration) {
    final C extracted = extractConfiguration(applicationConfiguration.getConfigurationModel());
    if (extracted != null) {
      setOriginalConfiguration(extracted);
    }
    else {
      setOriginalConfiguration(getDefaultConfiguration());
    }

    this.applicationConfiguration = applicationConfiguration;
  }

  protected ApplicationConfiguration getApplicationConfiguration() {
    return applicationConfiguration;
  }

  public C getDefaultConfiguration() {
    return null;
  }

  // ==

  protected abstract C extractConfiguration(Configuration configuration);
}
