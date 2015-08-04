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
package org.sonatype.nexus.configuration.application;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.sisu.goodies.common.ComponentSupport;

@Singleton
@Named
public class NfcConfigurationModifier
    extends ComponentSupport
    implements ConfigurationModifier
{
  /**
   * Flag to control how to interfere with configuration. If system property is not set at all, this
   * ConfigurationModifier will remain inactive (will not interfere with configuration at all). If is set, and it's
   * value evaluates to Boolean.TRUE (equals to string "true"), then the enforce will be applied, and NFC will be
   * forced to not-active on all non-proxy repository and to active on all proxy repositories present in
   * configuration. If is set, and it's value evaluates to Boolean.FALSE (is set with any other string than "true"),
   * it will revert to the "default" (all repositories will have NFC enabled). Using these, and bouncing Nexus
   * instance, you can easily "force" the configuration into a desired state (ie. to restore to old state, just set
   * the property to "false" and bounce Nexus). The presence of this component has no any other "side effect".
   */
  private final String operation = SystemPropertiesHelper.getString(
      "org.sonatype.nexus.configuration.application.NfcConfigurationModifier.enforce", null);

  @Override
  public boolean apply(final Configuration configuration) {
    if (operation == null) {
      // neglect all this, just ignore
      return false;
    }
    else if (Boolean.parseBoolean(operation)) {
      doForceNfcSetting(configuration);
      return true;
    }
    else {
      undoForceNfcSetting(configuration);
      return true;
    }
  }

  protected void doForceNfcSetting(final Configuration configuration) {
    log.info(
        "Enforcing proper NFC use: every non-proxy repository present in system will have NFC deactivated (system property override present).");

    // conservatively shut down NFC on any non-proxy repository
    for (CRepository repository : configuration.getRepositories()) {
      final boolean isProxyRepository =
          repository.getRemoteStorage() != null && repository.getRemoteStorage().getUrl() != null;
      repository.setNotFoundCacheActive(isProxyRepository);
    }
  }

  protected void undoForceNfcSetting(final Configuration configuration) {
    log.info(
        "Undoing NFC overrides: every repository present in system will have NFC activated (system property override present).");

    // just undo, set true on all repositories in system
    for (CRepository repository : configuration.getRepositories()) {
      repository.setNotFoundCacheActive(true);
    }
  }
}
