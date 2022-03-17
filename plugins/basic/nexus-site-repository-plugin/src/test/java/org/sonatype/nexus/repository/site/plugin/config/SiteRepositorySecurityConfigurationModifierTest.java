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
package org.sonatype.nexus.repository.site.plugin.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.inject.Inject;

import org.sonatype.security.model.Configuration;
import org.sonatype.security.model.io.xpp3.SecurityConfigurationXpp3Reader;
import org.sonatype.security.model.io.xpp3.SecurityConfigurationXpp3Writer;
import org.sonatype.sisu.goodies.testsupport.hamcrest.DiffMatchers;
import org.sonatype.sisu.goodies.testsupport.inject.InjectedTestSupport;

import org.junit.Test;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link SiteRepositorySecurityConfigurationModifier} UTs.
 *
 * @since site-repository 1.0
 */
public class SiteRepositorySecurityConfigurationModifierTest
    extends InjectedTestSupport
{

  @Inject
  private SiteRepositorySecurityConfigurationModifier modifier;

  @Test
  public void modifyConfiguration()
      throws Exception
  {
    final Configuration configuration = new SecurityConfigurationXpp3Reader().read(
        new FileInputStream(util.resolveFile("target/test-classes/security.xml"))
    );

    modifier.apply(configuration);

    final File modified = util.resolveFile("target/modified-security.xml");
    new SecurityConfigurationXpp3Writer().write(
        new FileOutputStream(modified),
        configuration
    );
    assertThat(
        normalizeLineEndings(readFileToString(modified)),
        DiffMatchers.equalToOnlyDiffs(
            normalizeLineEndings(readFileToString(util.resolveFile("target/test-classes/expected-security.xml"))))
    );
  }

  private String normalizeLineEndings(final String text) {
    return text.replace("\r\n", "\n");
  }

}
