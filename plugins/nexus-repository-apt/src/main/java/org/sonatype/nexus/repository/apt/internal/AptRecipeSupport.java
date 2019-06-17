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
package org.sonatype.nexus.repository.apt.internal;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.view.handlers.HighAvailabilitySupportChecker;

/**
 * Support for Apt recipes
 *
 * @since 3.17
 */
public abstract class AptRecipeSupport
    extends RecipeSupport
{
  private HighAvailabilitySupportChecker highAvailabilitySupportChecker;

  static {
    Configuration.addSensitiveFieldName("aptSigning");
  }

  protected AptRecipeSupport(final HighAvailabilitySupportChecker highAvailabilitySupportChecker,
                             final Type type,
                             final Format format)
  {
    super(type, format);
    this.highAvailabilitySupportChecker = highAvailabilitySupportChecker;
  }

  @Override
  public boolean isFeatureEnabled() {
    final boolean formatSupportHighAvailability =
        highAvailabilitySupportChecker.isSupported(getFormat().getValue());
    return formatSupportHighAvailability;
  }
}
