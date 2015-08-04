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
package org.sonatype.nexus.testsuite.p2.nexus5741;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;
import org.sonatype.sisu.litmus.testsupport.hamcrest.LogFileMatcher;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * ITs related to validation of proxied items content for P2 repositories.
 *
 * @since 2.6
 */
public class Nexus5741P2RepositoryContentValidationIT
    extends AbstractNexusProxyP2IT
{

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public Nexus5741P2RepositoryContentValidationIT() {
    super("nexus5741");
  }

  /**
   * Verify that a jar file, which has an invalid content (html in this case) will fail validation and such it cannot
   * be downloaded.
   */
  @Test
  public void jarWithXmlContentShouldBeInvalid()
      throws IOException
  {
    try {
      downloadFile(
          new URL(getNexusTestRepoUrl() + "org.eclipse.mylyn.commons.core_3.8.4.v20130429-0100.jar"),
          new File("target/downloads/nexus5741/org.eclipse.mylyn.commons.core_3.8.4.v20130429-0100.jar")
              .getCanonicalPath()
      );

      assertThat("Expected to fail with " + FileNotFoundException.class.getSimpleName(), false);
    }
    catch (final FileNotFoundException e) {
      assertThat(
          e.getMessage(),
          containsString(
              "content/repositories/nexus5741/org.eclipse.mylyn.commons.core_3.8.4.v20130429-0100.jar"
          )
      );
    }

    assertThat(
        getNexusLogFile(),
        LogFileMatcher.hasText(
            "Proxied item nexus5741:/org.eclipse.mylyn.commons.core_3.8.4.v20130429-0100.jar evaluated as INVALID during content validation (validator=filetypevalidator"
        )
    );
  }

}
