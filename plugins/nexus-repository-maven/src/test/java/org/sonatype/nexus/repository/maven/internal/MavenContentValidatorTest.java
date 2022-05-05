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
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nullable;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.io.InputStreamSupplier;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.repository.mime.DefaultContentValidator;

import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.verification.VerificationMode;

import static java.util.Optional.ofNullable;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class MavenContentValidatorTest
    extends TestSupport
{
  private static final InputStreamSupplier DEFAULT_SUPPLIER = () -> new ByteArrayInputStream("0xDEADBEEF".getBytes());

  // "caff" as a header would normally be detected as 'audio/x-caf'
  private static final InputStreamSupplier AUDIO_CAF_SUPPLIER = () -> new ByteArrayInputStream("caff123456789".getBytes());

  private static final InputStreamSupplier MD5_SUPPLIER = () -> new ByteArrayInputStream("0161dba22520b2c13b50493fd98ed4ce".getBytes());

  private static final InputStreamSupplier SHA1_SUPPLIER = () -> new ByteArrayInputStream("2abe58492ad5e25e18cfca3fad4f0322d47cb893".getBytes());

  private static final InputStreamSupplier SHA256_SUPPLIER = () -> new ByteArrayInputStream("34cbb64305cb610b642162b052e66b4683ae68fd20d34591c21ab68bec106ccb".getBytes());

  private static final InputStreamSupplier SHA512_SUPPLIER = () -> new ByteArrayInputStream("bfe3bcd9fc7180c2439d7c0b3b3036f71a6da1fed2983e3ab23185bf3a6877f6a32dbd6b949d7ef3ab1935699a113f47987082fbaffb2ce9f65f5ad058475c0e".getBytes());

  @Parameters(name = "{index}: contentName:{0} strictContentValidation:{1}, declaredContentType:{2}, contentSupplier:{3}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {null, true, TEXT_PLAIN, DEFAULT_SUPPLIER, times(1), TEXT_PLAIN, null},
        {"file.jar", true, TEXT_PLAIN, DEFAULT_SUPPLIER, times(1), TEXT_PLAIN, null},
        {"file.pom", true, TEXT_PLAIN, DEFAULT_SUPPLIER, times(1), TEXT_PLAIN, null},

        {"file.md5", false, TEXT_PLAIN, MD5_SUPPLIER, never(), TEXT_PLAIN, null},
        {"file.md5", true, TEXT_PLAIN, MD5_SUPPLIER, never(), TEXT_PLAIN, null},
        {"file.md5", true, TEXT_PLAIN, DEFAULT_SUPPLIER, never(), TEXT_PLAIN, "Not a Maven2 digest: file.md5"},
        {"file.md5", false, null, MD5_SUPPLIER, times(1), TEXT_PLAIN, null},
        {"file.md5", false, TEXT_PLAIN, AUDIO_CAF_SUPPLIER, never(), TEXT_PLAIN, null},

        {"file.sha1", false, TEXT_PLAIN, SHA1_SUPPLIER, never(), TEXT_PLAIN, null},
        {"file.sha1", true, TEXT_PLAIN, SHA1_SUPPLIER, never(), TEXT_PLAIN, null},
        {"file.sha1", true, TEXT_PLAIN, DEFAULT_SUPPLIER, never(), TEXT_PLAIN, "Not a Maven2 digest: file.sha1"},
        {"file.sha1", false, null, SHA1_SUPPLIER, times(1), TEXT_PLAIN, null},
        {"file.sha1", false, TEXT_PLAIN, AUDIO_CAF_SUPPLIER, never(), TEXT_PLAIN, null},

        {"file.sha256", false, TEXT_PLAIN, SHA256_SUPPLIER, never(), TEXT_PLAIN, null},
        {"file.sha256", true, TEXT_PLAIN, SHA256_SUPPLIER, never(), TEXT_PLAIN, null},
        {"file.sha256", true, TEXT_PLAIN, DEFAULT_SUPPLIER, never(), TEXT_PLAIN, "Not a Maven2 digest: file.sha256"},
        {"file.sha256", false, null, SHA256_SUPPLIER, times(1), TEXT_PLAIN, null},
        {"file.sha256", false, TEXT_PLAIN, AUDIO_CAF_SUPPLIER, never(), TEXT_PLAIN, null},

        {"file.sha512", false, TEXT_PLAIN, SHA512_SUPPLIER, never(), TEXT_PLAIN, null},
        {"file.sha512", true, TEXT_PLAIN, SHA512_SUPPLIER, never(), TEXT_PLAIN, null},
        {"file.sha512", true, TEXT_PLAIN, DEFAULT_SUPPLIER, never(), TEXT_PLAIN, "Not a Maven2 digest: file.sha512"},
        {"file.sha512", false, null, SHA512_SUPPLIER, times(1), TEXT_PLAIN, null},
        {"file.sha512", false, TEXT_PLAIN, AUDIO_CAF_SUPPLIER, never(), TEXT_PLAIN, null},
    });
  }

  @Mock
  private MimeRulesSource mimeRulesSource;

  @Mock
  private DefaultContentValidator defaultContentValidator;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Nullable
  @Parameter(0)
  public String contentName;

  @Parameter(1)
  public boolean isStrictContentValidation;

  @Nullable
  @Parameter(2)
  public ContentType declaredContentType;

  @Parameter(3)
  public InputStreamSupplier contentSupplier;

  @Parameter(4)
  public VerificationMode defaultContentValidatorInvocation;

  @Parameter(5)
  public ContentType expectedContentType;

  @Nullable
  @Parameter(6)
  public String expectedExceptionMessage;

  private MavenContentValidator underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new MavenContentValidator(defaultContentValidator);

    String declaredMimeType = ofNullable(declaredContentType).map(ContentType::getMimeType).orElse(null);

    when(defaultContentValidator.determineContentType(eq(isStrictContentValidation),
        eq(contentSupplier),
        eq(mimeRulesSource),
        any(),
        eq(declaredMimeType)
        )).thenReturn(TEXT_PLAIN.getMimeType());
  }

  @Test
  public void determineContentType() throws Exception {
    String declaredMimeType = ofNullable(declaredContentType).map(ContentType::getMimeType).orElse(null);

    if (expectedExceptionMessage != null) {
      thrown.expectMessage(expectedExceptionMessage);
    }

    assertThat(underTest.determineContentType(
        isStrictContentValidation,
        contentSupplier,
        mimeRulesSource,
        contentName,
        declaredMimeType),
    is(expectedContentType.getMimeType()));

    String contentNameForDefaultValidator = ("file.pom".equalsIgnoreCase(contentName)) ? "file.pom.xml" : contentName;

    verify(defaultContentValidator, defaultContentValidatorInvocation)
        .determineContentType(
            isStrictContentValidation,
            contentSupplier,
            mimeRulesSource,
            contentNameForDefaultValidator,
            declaredMimeType);
  }
}
