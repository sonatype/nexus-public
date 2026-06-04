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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.sonatype.nexus.mime.internal.DefaultMimeSupport;
import org.sonatype.nexus.repository.mime.DefaultContentValidator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AptContentValidatorTest
{
  private static AptContentValidator underTest;

  @BeforeAll
  static void setUp() {
    // Use real DefaultContentValidator with real DefaultMimeSupport
    underTest = new AptContentValidator(new DefaultContentValidator(new DefaultMimeSupport()));
  }

  @Test
  public void AptGpgSignatureContentTypeIsDeterminedAsExpected() throws IOException {
    String content = """
        -----BEGIN PGP SIGNATURE-----
        iQIzBAABCAAdFiEETLUBkCB7R1ij9zp5btDnuCZD4TEFAmi8AkgACgkQbtDnuCZD
        4TFqlhAAlDKAWkf/O8MdhTIYozuDPXpRxL32nMBAOL+amYDj47HT2dM/TqFddroI
        H26YvU8PMGrTX4wDJSd28VaffV4xawb3AqD0lBOHI2+Up8gH83Ekj6QrRqsFT3GP
        uWbjwaKXlF1uIXEULLEyubLICpgmW3rOXefsk9AKo7awpbCgp89eM0Effu2Ay6Cq
        Gffll9Df1UVzup9TYoqk5MVALW3FvvOgVrPiN4A4DqJweOVz8Bezy0s1WaaU+xky
        rNBsc83dkkFj7eRSHEwvJ6/UyDHv2ktxZ5ZggNW0k83U2K0g0gAECXvfInKPqPmH
        7EfxvNvmsJKAZChU8TDvWwINrsCq5/3h4exYxUH81MPnlPwHTM4y9RyruWHOjS3b
        X6jr6vtm872GlkJW+4DhSqDAQiggDbVyqB8313LLXmomaey3guhT1a5B4Oq/MO8w
        1qrE06GXbrqkwVxR1Vd3DjLrVYSuD43e63LBCzY8O2bRmBXkqsoFoK8ASpNDq7p0
        zVosicLW1D1aP6BmkvPnbVjzvyvfh8a2yM6xvUf68XtdJ4jsOr4Yr7WLL5Y6KI8F
        i4O+Axd68nyq21uiFrG/MgWuzxzWGQgYFEoehw2OPdyNsqbj2u/ZXWLu4IDZ8OmO
        d+qnvIWJpa5IqZwaKaRgGqgM/e/ZtLdNj52nhUoXlR4946DPTmmJAjMEAAEIAB0W
        IQS45fExdtKnp1IgAoB426O8R+8iZQUCaLwCSAAKCRB426O8R+8iZTibD/wN+n3d
        Snjp3Kb3bXtgojMOyJ93pGzTdBP3ez6p9RPTcxWa7HGmIuys15LBlnlBdd2OGz46
        sDwtDnYro0/zjV+Xi/Zz9//Y/qwfqQR2H+DG0dJPHA79VI75KCfloZLyYmZIzXn7
        HZbIOqcbpnajvt5mqQj2CTOPyxMx89ElDvzl7K2dywC1XZt4Dol3vla/74E4oLMM
        KFCVzMHuRIIzBSPdwJcFPYqFEZG7LzuyteQkf01zoET+1JDA0GIu1rbSLuJCd8QE
        FfnL/1njLBTr/pCJV3x9rmKPWY3jIdcYD6oSlqNv6CIiYpZ5ryM811UbZBjlVrVB
        4l2rVksZpWDT2f1393/yK4SjaoHfmPa2i3tjQFv1LGjGHitAwah9+DkMtzUE130X
        Tc/Tr+ClpRdMbj0fwQxptdjPz43IdD9eoLqyun3l6K6WX5nREP219BgSygjpPwo8
        q0JqTY2zurOWMbumcdbS/QTr/MHa3jZ62Mav9q08VvNFgPI2Job1WROTL/sF/WxC
        RDFL/aSfpnsNnxAVy1J55FsByxjNlZn2bMY/DCTuGcZ+mkDBQouQyOE9k/cBi4CC
        MeVyWqHAMVvnjtdWXFaHz9F263FbWFA/A+fIkFAD8dULpp4pup1fxEu6FRrC4kK9
        HdyU6SouAmYiKmR6mTvWfywyJBb/QbiDH4W+5IiWBAAWCAA+FiEEQVh/fbjHdLzP
        ExQWdi9noLLDneQFAmi8A54gHGRlYmlhbi1yZWxlYXNlQGxpc3RzLmRlYmlhbi5v
        cmcACgkQdi9noLLDneRD8AD/Rvdg5OYdGVetGK4ascn7sEk+3UqRx3M0/MjP/erG
        FVsBAMZy7zUz1GyTLXslmOeGenAjJWlgYhnGmaMzsbm/W/0F
        =cv9j
        -----END PGP SIGNATURE-----
        """;

    String type = underTest.determineContentType(
        true,
        () -> new ByteArrayInputStream(content.getBytes()),
        new AptMimeRulesSource(), // use Apt specific mime rules so content is assumed correctly
        "/dists/noble/Release.gpg",
        "application/pgp");
    assertThat(type, equalTo("application/pgp-signature"));
  }

  @Test
  public void releaseGpgWithUpstreamTextPlainDeclared_doesNotThrowAndReturnsSignatureType() {
    // Upstream servers sometimes return Release.gpg with Content-Type: text/plain.
    // The validator must not corrupt the content name by appending .txt (which causes
    // InvalidContentException when strict validation compares "text/plain" vs "pgp-signature").
    String pgpContent = """
        -----BEGIN PGP SIGNATURE-----
        iQIzBAABCAAdFiEETLUBkCB7R1ij9zp5btDnuCZD4TEFAmi8AkgACgkQbtDnuCZD
        4TFqlhAAlDKAWkf/O8MdhTIYozuDPXpRxL32nMBAOL+amYDj47HT2dM/TqFddroI
        =cv9j
        -----END PGP SIGNATURE-----
        """;

    String[] result = new String[1];
    assertDoesNotThrow(() -> result[0] = underTest.determineContentType(
        true,
        () -> new ByteArrayInputStream(pgpContent.getBytes()),
        new AptMimeRulesSource(),
        "/dists/jammy/Release.gpg",
        "text/plain")); // upstream declared text/plain for a .gpg file

    assertThat(result[0], equalTo("application/pgp-signature"));
  }

  @Test
  public void extensionlessFileGetsTxtAppendedBeforeDelegation() throws IOException {
    // Packages, Release, InRelease (no dot in the basename) declared text/plain should
    // have .txt appended so the default validator can confirm they are plain text.
    DefaultContentValidator mockValidator = mock(DefaultContentValidator.class);
    AptContentValidator validator = new AptContentValidator(mockValidator);
    when(mockValidator.determineContentType(anyBoolean(), any(), any(), anyString(), anyString()))
        .thenReturn("text/plain");

    ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
    validator.determineContentType(
        true,
        () -> new ByteArrayInputStream("Package: nano\n".getBytes()),
        new AptMimeRulesSource(),
        "/dists/focal/main/binary-amd64/Packages",
        "text/plain");

    verify(mockValidator).determineContentType(anyBoolean(), any(), any(), nameCaptor.capture(), anyString());
    assertThat(nameCaptor.getValue(), equalTo("/dists/focal/main/binary-amd64/Packages.txt"));
  }

  @Test
  public void fileWithExtensionIsNotRenamedBeforeDelegation() throws IOException {
    // Files that already have an extension (e.g. Packages.gz, Release.gpg) must NOT get
    // .txt appended — that would break content-type validation for the actual extension.
    DefaultContentValidator mockValidator = mock(DefaultContentValidator.class);
    AptContentValidator validator = new AptContentValidator(mockValidator);
    when(mockValidator.determineContentType(anyBoolean(), any(), any(), anyString(), anyString()))
        .thenReturn("application/x-gzip");

    ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
    validator.determineContentType(
        true,
        () -> new ByteArrayInputStream(new byte[0]),
        new AptMimeRulesSource(),
        "/dists/focal/main/binary-amd64/Packages.gz",
        "text/plain");

    verify(mockValidator).determineContentType(anyBoolean(), any(), any(), nameCaptor.capture(), anyString());
    assertThat(nameCaptor.getValue(), equalTo("/dists/focal/main/binary-amd64/Packages.gz"));
  }

  @Test
  public void nullContentNameIsPassedThroughUnmodified() throws IOException {
    // A null contentName must not throw NullPointerException.
    DefaultContentValidator mockValidator = mock(DefaultContentValidator.class);
    AptContentValidator validator = new AptContentValidator(mockValidator);
    when(mockValidator.determineContentType(anyBoolean(), any(), any(), isNull(), anyString()))
        .thenReturn("application/octet-stream");

    assertDoesNotThrow(() -> validator.determineContentType(
        false,
        () -> new ByteArrayInputStream(new byte[0]),
        new AptMimeRulesSource(),
        null,
        "text/plain"));
  }
}
