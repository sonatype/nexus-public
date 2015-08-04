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
package org.sonatype.nexus.security.ldap.realms.internal

import org.sonatype.security.ldap.realms.persist.model.io.xpp3.LdapConfigurationXpp3Reader
import org.sonatype.security.ldap.realms.persist.model.io.xpp3.LdapConfigurationXpp3Writer
import org.sonatype.nexus.atlas.GeneratedContentSourceSupport
import org.sonatype.nexus.atlas.SupportBundle
import org.sonatype.nexus.atlas.SupportBundleCustomizer
import org.sonatype.nexus.configuration.application.ApplicationConfiguration
import org.sonatype.sisu.goodies.common.ComponentSupport

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import static com.google.common.base.Preconditions.checkNotNull
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Priority.HIGH
import static org.sonatype.nexus.atlas.SupportBundle.ContentSource.Type.SECURITY

/**
 * LDAP (for OSS) {@link SupportBundleCustomizer}.
 *
 * @since 2.11
 */
@Named
@Singleton
public class SupportBundleCustomizerImpl
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  private final ApplicationConfiguration applicationConfiguration

  @Inject
  public SupportBundleCustomizerImpl(final ApplicationConfiguration applicationConfiguration) {
    this.applicationConfiguration = checkNotNull(applicationConfiguration)
  }

  /**
   * Customize the given bundle, adding one or more content sources.
   */
  @Override
  public void customize(final SupportBundle supportBundle) {
    supportBundle << new LdapXmlContentSource()
  }

  /**
   * Source for obfuscated ldap.xml
   */
  private class LdapXmlContentSource
      extends GeneratedContentSourceSupport
  {
    LdapXmlContentSource() {
      super(SECURITY, 'work/conf/ldap.xml')
      this.priority = HIGH
    }

    @Override
    protected void generate(final File file) {
      File source = new File(applicationConfiguration.configurationDirectory, 'ldap.xml')
      if (!source.exists()) {
        log.debug 'Skipping non-existent file: {}', source
        return
      }

      log.debug 'Reading: {}', source
      source.withInputStream { input ->
        def model = new LdapConfigurationXpp3Reader().read(input)

        // obfuscate sensitive content
        model?.connectionInfo?.systemPassword = PASSWORD_TOKEN

        file.withOutputStream { output ->
          new LdapConfigurationXpp3Writer().write(output, model)
        }
      }
    }
  }
}
