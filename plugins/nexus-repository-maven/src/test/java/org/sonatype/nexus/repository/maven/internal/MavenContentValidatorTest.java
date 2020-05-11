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
package org.sonatype.nexus.repository.maven.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.mime.internal.DefaultMimeSupport;
import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.mime.DefaultContentValidator;

import com.google.common.base.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MavenContentValidatorTest
    extends TestSupport
{
  @Mock
  private Supplier<InputStream> contentSupplier;

  @Mock
  private MimeRulesSource mimeRulesSource;

  @Mock
  private DefaultContentValidator defaultContentValidator;

  private MavenContentValidator underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new MavenContentValidator(defaultContentValidator);
  }

  @Test
  public void whenNoContentNameShouldDetermineByDefaultValidator() throws Exception {
    underTest.determineContentType(true, contentSupplier, mimeRulesSource, null, TEXT_PLAIN.getMimeType());

    verify(defaultContentValidator)
        .determineContentType(true, contentSupplier, mimeRulesSource, null, TEXT_PLAIN.getMimeType());
  }

  @Test
  public void whenContentNameIsJarShouldDetermineByDefaultValidator() throws Exception {
    underTest.determineContentType(true, contentSupplier, mimeRulesSource, "file.jar", TEXT_PLAIN.getMimeType());

    verify(defaultContentValidator)
        .determineContentType(true, contentSupplier, mimeRulesSource, "file.jar", TEXT_PLAIN.getMimeType());
  }

  @Test
  public void whenContentNameIsPomShouldNotDetermineByDefaultValidatorWithXml() throws Exception {
    underTest.determineContentType(true, contentSupplier, mimeRulesSource, "file.pom", TEXT_PLAIN.getMimeType());

    verify(defaultContentValidator)
        .determineContentType(true, contentSupplier, mimeRulesSource, "file.pom.xml", TEXT_PLAIN.getMimeType());
  }

  @Test
  public void whenContentNameIsSha1ShouldNotDetermineByDefaultValidator() throws Exception {
    underTest.determineContentType(false, contentSupplier, mimeRulesSource, "file.sha1", TEXT_PLAIN.getMimeType());

    verify(defaultContentValidator, times(0))
        .determineContentType(false, contentSupplier, mimeRulesSource, "file.sha1", TEXT_PLAIN.getMimeType());
  }

  @Test(expected = InvalidContentException.class)
  public void whenContentNameIsSha1AndStrictValidationShouldNotDetermineByDefaultValidator() throws Exception {
    Supplier<InputStream> supplier = () -> new ByteArrayInputStream("hello".getBytes());

    underTest.determineContentType(true, supplier, mimeRulesSource, "file.sha1", TEXT_PLAIN.getMimeType());
  }

  @Test
  public void whenContentNameIsSha1AndNoMimeTypeShouldDetermineByDefaultValidator() throws Exception {
    underTest.determineContentType(false, contentSupplier, mimeRulesSource, "file.sha1", null);

    verify(defaultContentValidator)
        .determineContentType(false, contentSupplier, mimeRulesSource, "file.sha1", null);
  }

  @Test
  public void whenContentNameIsSha1MimTypeDetectionNotPerformed() throws Exception {
    // "caff" as a header would normally be detected as 'audio/x-caf'
    Supplier<InputStream> supplier = () -> new ByteArrayInputStream("caff123456789".getBytes());

    MavenContentValidator mavenContentValidator = new MavenContentValidator(
        new DefaultContentValidator(new DefaultMimeSupport()));

    assertThat(
        mavenContentValidator
            .determineContentType(false, supplier, mimeRulesSource, "file.sha1", TEXT_PLAIN.getMimeType()),
        is(TEXT_PLAIN.getMimeType()));
  }
}
