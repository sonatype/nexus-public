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
package org.sonatype.nexus.testsuite.testsupport.apt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;
import org.sonatype.nexus.testsuite.testsupport.fixtures.RepositoryRule;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.junit.experimental.categories.Category;

/**
 * Support for Apt ITs.
 */
@Category(AptTestGroup.class)
public class AptITSupport
    extends RepositoryITSupport
{
  protected static final String CONTENT_TYPE = "application/x-debian-package";

  protected static final String CATEGORY = StringUtils.EMPTY;

  public static final String DEB = "nano_2.2.6-1_amd64.deb";

  public static final String DEB_SZT =
      "linux-image-unsigned-5.14.1-051401-generic_5.14.1-051401.202109030936_amd64.deb";

  public static final String DEB_ARCH = "amd64";

  public static final String DEB_NAME = "nano";

  public static final String DEB_VERSION = "2.2.6-1";

  public static final String DEB_PATH = "pool/n/nano/" + DEB;

  public static final String DEB_V2_5 = "nano_2.5.3-2_amd64.deb";

  public static final String DEB_V2_5_PATH = "pool/n/nano/" + DEB_V2_5;

  public static final String DEB_V2_5_VERSION = "2.5.3-2";

  public static final String CPU_LIMIT_DEB = "cpulimit_2.5-1_amd64.deb";

  public static final String CPU_LIMIT_NAME = "cpulimit";

  public static final String DISTRIBUTION = "bionic";

  public static final String HOSTED_REPO_NAME = "apt-hosted";

  public static final String HOSTED_REPO_WITH_COMPONENTS_NAME = "apt-hosted-with-components";

  public static final String GPG_KEY_NAME = "gpgKey";

  protected static final String GPG_PUBLIC_KEY_NAME = "gpgPublicKey";

  protected static final String METEDATA_PATH = "dists/bionic/";

  protected static final String METADATA_INRELEASE = "InRelease";

  protected static final String METADATA_INRELEASE_PATH = METEDATA_PATH + METADATA_INRELEASE;

  protected static final String METADATA_RELEASE = "Release";

  protected static final String METADATA_RELEASE_PATH = METEDATA_PATH + METADATA_RELEASE;

  protected static final String METADATA_RELEASE_GPG = "Release.gpg";

  protected static final String METADATA_RELEASE_GPG_PATH = METEDATA_PATH + METADATA_RELEASE_GPG;

  public static final String PACKAGES_PATH = METEDATA_PATH + "main/binary-amd64/Packages";

  public static final String PACKAGES_BZ2_PATH = METEDATA_PATH + "main/binary-amd64/Packages.bz2";

  public static final String PACKAGES_GZ_PATH = METEDATA_PATH + "main/binary-amd64/Packages.gz";

  protected static final String PROXY_REPO_NAME = "apt-proxy";

  protected static final String PROXIED_PROXY_REPO_NAME = "proxied-apt-proxy";

  protected static final String PROXIED_HOSTED_REPO_NAME = "proxied-apt-hosted";

  public AptITSupport() {
    testData.addDirectory(resolveBaseFile("target/it-resources/apt"));
  }

  public Repository createAptHostedRepository(final String name, final String distribution, final String gpgKeyName)
      throws IOException
  {
    return createAptHostedRepository(repos, name, distribution, testData.resolveFile(gpgKeyName).toPath());
  }

  public Repository createAptHostedRepository(final RepositoryRule repository,
                                              final String name,
                                              final String distribution,
                                              final Path gpgFilePath)
      throws IOException
  {
    String gpgKey = new String(Files.readAllBytes(gpgFilePath), StandardCharsets.UTF_8);
    return repository.createAptHosted(name, distribution, gpgKey);
  }

  protected Repository createAptProxyRepository(final String name, final String remoteUrl, final String distribution) {
    return repos.createAptProxy(name, remoteUrl, distribution);
  }

  public boolean verifyReleaseFilePgpSignature(final InputStream signedData,
                                               final InputStream signature,
                                               final InputStream publicKey)
      throws Exception
  {
    PGPObjectFactory pgpFact =
        new PGPObjectFactory(PGPUtil.getDecoderStream(signature), new JcaKeyFingerprintCalculator());
    PGPSignature sig = ((PGPSignatureList) pgpFact.nextObject()).get(0);

    PGPPublicKeyRingCollection pgpPubRingCollection =
        new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(publicKey),
            new JcaKeyFingerprintCalculator());

    PGPPublicKey key = pgpPubRingCollection.getPublicKey(sig.getKeyID());
    sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), key);
    byte[] buff = new byte[1024];
    int read = 0;
    while ((read = signedData.read(buff)) != -1) {
      sig.update(buff, 0, read);
    }
    signedData.close();
    return sig.verify();
  }

  public boolean verifyInReleaseFilePgpSignature(final InputStream fileContent, final InputStream publicKeyString)
      throws Exception
  {

    PGPPublicKeyRingCollection pgpRings =
        new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(publicKeyString),
            new JcaKeyFingerprintCalculator());
    ArmoredInputStream aIn = new ArmoredInputStream(fileContent);
    ByteArrayOutputStream releaseContent = new ByteArrayOutputStream();
    ByteArrayOutputStream lineOut = new ByteArrayOutputStream();

    int fromPositon = -1;
    if (aIn.isClearText()) {
      do {
        fromPositon = readStreamLine(lineOut, fromPositon, aIn);
        releaseContent.write(lineOut.toByteArray());
      }
      while (fromPositon != -1 && aIn.isClearText());
    }

    PGPObjectFactory pgpFact = new PGPObjectFactory(aIn, new JcaKeyFingerprintCalculator());
    PGPSignatureList p3 = (PGPSignatureList) pgpFact.nextObject();
    PGPSignature sig = p3.get(0);

    PGPPublicKey publicKey = pgpRings.getPublicKey(sig.getKeyID());
    sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), publicKey);
    InputStream sigIn = new ByteArrayInputStream(releaseContent.toByteArray());

    fromPositon = -1;
    do {
      int length;
      if (fromPositon != -1) {
        sig.update((byte) '\r');
        sig.update((byte) '\n');
      }
      fromPositon = readStreamLine(lineOut, fromPositon, sigIn);
      length = lineOut.toString(StandardCharsets.UTF_8.name()).replaceAll("\\s*$", "").length();
      if (length > 0) {
        sig.update(lineOut.toByteArray(), 0, length);
      }
    }
    while (fromPositon != -1);

    return sig.verify();
  }

  private static int readStreamLine(final ByteArrayOutputStream lineBuffer,
                                    final int fromPosition,
                                    final InputStream in)
      throws IOException
  {
    lineBuffer.reset();
    int symbol;
    if (fromPosition != -1) {
      lineBuffer.write(fromPosition);
    }
    while ((symbol = in.read()) >= 0) {
      lineBuffer.write(symbol);
      if (symbol == '\n') {
        break;
      }
    }
    return symbol < 0 ? -1 : in.read();
  }
}
