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
package org.sonatype.nexus.repository.apt.internal.gpg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.apt.internal.AptMimeTypes;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

/**
 * @since 3.next
 */
@Named
@Facet.Exposed
public class AptSigningFacet
    extends FacetSupport
{
  @VisibleForTesting
  static final String CONFIG_KEY = "aptSigning";

  @VisibleForTesting
  static class Config
  {
    @NotNull(groups = {HostedType.ValidationGroup.class, GroupType.ValidationGroup.class})
    public String keypair;

    public String passphrase = "";
  }

  private Config config;

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    facet(ConfigurationFacet.class).validateSection(
        configuration,
        CONFIG_KEY,
        Config.class,
        Default.class,
        getRepository().getType().getValidationGroup());
  }

  @Override
  protected void doConfigure(final Configuration configuration) throws Exception {
    config = facet(ConfigurationFacet.class).readSection(configuration, CONFIG_KEY, Config.class);
  }

  @Override
  protected void doDestroy() throws Exception {
    config = null;
  }

  public Content getPublicKey() throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    PGPSecretKey signKey = readSecretKey();
    PGPPublicKey publicKey = signKey.getPublicKey();
    try (BCPGOutputStream os = new BCPGOutputStream(new ArmoredOutputStream(buffer))) {
      publicKey.encode(os);
    }

    return new Content(new BytesPayload(buffer.toByteArray(), AptMimeTypes.PUBLICKEY));
  }

  public byte[] signInline(String input) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try {
      PGPSecretKey signKey = readSecretKey();
      PGPPrivateKey privKey = signKey.extractPrivateKey(
          new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(config.passphrase.toCharArray()));
      PGPSignatureGenerator sigGenerator = new PGPSignatureGenerator(
          new JcaPGPContentSignerBuilder(signKey.getPublicKey().getAlgorithm(), PGPUtil.SHA256).setProvider("BC"));
      sigGenerator.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, privKey);

      @SuppressWarnings("unchecked")
      Iterator<String> userIds = signKey.getUserIDs();
      if (userIds.hasNext()) {
        PGPSignatureSubpacketGenerator sigSubpacketGenerator = new PGPSignatureSubpacketGenerator();
        sigSubpacketGenerator.setSignerUserID(false, userIds.next());
        sigGenerator.setHashedSubpackets(sigSubpacketGenerator.generate());
      }

      String[] lines = input.split("\r?\n");
      try (ArmoredOutputStream aOut = new ArmoredOutputStream(buffer)) {
        aOut.beginClearText(PGPUtil.SHA256);

        boolean firstLine = true;
        for (String line : lines) {
          String sigLine = (firstLine ? "" : "\r\n") + line.replaceAll("\\s*$", "");
          sigGenerator.update(sigLine.getBytes(Charsets.UTF_8));
          aOut.write((line + "\n").getBytes(Charsets.UTF_8));
          firstLine = false;
        }
        aOut.endClearText();

        BCPGOutputStream bOut = new BCPGOutputStream(aOut);
        sigGenerator.generate().encode(bOut);
      }
    }
    catch (PGPException ex) {
      throw new RuntimeException(ex);
    }
    return buffer.toByteArray();
  }

  public byte[] signExternal(String input) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try {
      PGPSecretKey signKey = readSecretKey();
      PGPPrivateKey privKey = signKey.extractPrivateKey(
          new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(config.passphrase.toCharArray()));
      PGPSignatureGenerator sigGenerator = new PGPSignatureGenerator(
          new JcaPGPContentSignerBuilder(signKey.getPublicKey().getAlgorithm(), PGPUtil.SHA256).setProvider("BC"));
      sigGenerator.init(PGPSignature.BINARY_DOCUMENT, privKey);

      try (ArmoredOutputStream aOut = new ArmoredOutputStream(buffer)) {
        BCPGOutputStream bOut = new BCPGOutputStream(aOut);
        sigGenerator.update(input.getBytes(Charsets.UTF_8));
        sigGenerator.generate().encode(bOut);
      }
    }
    catch (PGPException ex) {
      throw new RuntimeException(ex);
    }

    return buffer.toByteArray();
  }

  private PGPSecretKey readSecretKey() throws IOException {
    try {
      PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(
          PGPUtil.getDecoderStream(new ByteArrayInputStream(config.keypair.getBytes(Charsets.UTF_8))),
          new JcaKeyFingerprintCalculator());

      Iterator<PGPSecretKeyRing> keyRings = pgpSec.getKeyRings();
      while (keyRings.hasNext()) {
        PGPSecretKeyRing keyRing = (PGPSecretKeyRing) keyRings.next();

        Iterator<PGPSecretKey> keys = keyRing.getSecretKeys();
        while (keys.hasNext()) {
          PGPSecretKey key = (PGPSecretKey) keys.next();

          if (key.isSigningKey()) {
            return key;
          }
        }
      }
    }
    catch (PGPException ex) {
      throw new RuntimeException(ex);
    }

    throw new IllegalStateException("Can't find signing key in key ring.");
  }
}
