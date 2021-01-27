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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.apt.internal.AptMimeTypes;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.security.GpgUtils;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.openpgp.PGPPublicKey;

/**
 * Signs an Apt metadata by using PGP signing key pair.
 *
 * @since 3.17
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
    PGPPublicKey publicKey = GpgUtils.getPublicKey(config.keypair);
    try (BCPGOutputStream os = new BCPGOutputStream(new ArmoredOutputStream(buffer))) {
      publicKey.encode(os);
    }

    return new Content(new BytesPayload(buffer.toByteArray(), AptMimeTypes.PUBLICKEY));
  }

  public byte[] signInline(final String input) throws IOException {
    return GpgUtils.signInline(input, config.keypair, config.passphrase);
  }

  public byte[] signExternal(final String input) throws IOException {
    try (InputStream is = IOUtils.toInputStream(input, StandardCharsets.UTF_8)) {
      return GpgUtils.signExternal(is, config.keypair, config.passphrase);
    }
  }
}
