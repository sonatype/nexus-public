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
package org.sonatype.repository.conan.internal.security;

import java.io.IOException;

import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.mime.internal.DefaultMimeSupport;
import org.sonatype.nexus.repository.storage.DefaultContentValidator;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ConanContentValidatorTest
{
  private ConanContentValidator underTest;

  @Before
  public void setUp() throws Exception {
    DefaultContentValidator defaultContentValidator = new DefaultContentValidator(new DefaultMimeSupport());

    underTest = new ConanContentValidator(defaultContentValidator);
  }

  @Test
  public void shouldBePythonFileWhenUsingSheBang() throws IOException {
    String contentType = underTest.determineContentType(true,
        () -> getClass().getResourceAsStream("conanfile.py"),
        MimeRulesSource.NOOP,
        "something/conanfile.py",
        "text/x-python");

    assertThat(contentType, is("text/x-python"));
  }

  @Test
  public void shouldBePythonFileWhenNotUsingSheBang() throws IOException {
    String contentType = underTest.determineContentType(true,
        () -> getClass().getResourceAsStream("../metadata/conanfile.py"),
        MimeRulesSource.NOOP,
        "something/conanfile.py",
        "text/x-python");

    assertThat(contentType, is("text/x-python"));
  }
}
