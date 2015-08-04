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
package org.sonatype.security.realms.kenai.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.security.realms.kenai.config.model.Configuration;
import com.sonatype.security.realms.kenai.config.model.io.xpp3.KenaiRealmConfigurationXpp3Reader;
import com.sonatype.security.realms.kenai.config.model.io.xpp3.KenaiRealmConfigurationXpp3Writer;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.ModelUtils.CorruptModelException;
import org.sonatype.nexus.configuration.ModelUtils.MissingModelVersionException;
import org.sonatype.nexus.configuration.ModelUtils.Versioned;
import org.sonatype.nexus.configuration.ModelloUtils;
import org.sonatype.nexus.configuration.ModelloUtils.ModelloModelReader;
import org.sonatype.nexus.configuration.ModelloUtils.ModelloModelUpgrader;
import org.sonatype.nexus.configuration.ModelloUtils.VersionedInFieldXmlModelloModelHelper;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.security.SecuritySystem;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.base.Throwables;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Kenai realm configuration, a special one, as it's configuration is "one way", is read only. Also, the model
 * never got updated/changed (structurally) nor in version. Last change is aligning it's version with core
 * model version, but aside of version, model is basically the same as from the first day.
 */
@Singleton
@Named
@Typed(KenaiRealmConfiguration.class)
public class DefaultKenaiRealmConfiguration
    extends ComponentSupport
    implements KenaiRealmConfiguration
{
  private static class KenaiRealmModelReader
      extends ModelloModelReader<Configuration>
      implements Versioned
  {
    private final VersionedInFieldXmlModelloModelHelper versionedHelper = new VersionedInFieldXmlModelloModelHelper(
        "version");

    private final KenaiRealmConfigurationXpp3Reader modelloReader = new KenaiRealmConfigurationXpp3Reader();

    @Override
    public Configuration doRead(final Reader reader) throws IOException, XmlPullParserException {
      return modelloReader.read(reader);
    }

    @Override
    public String readVersion(final InputStream input) throws IOException, CorruptModelException {
      try {
        return versionedHelper.readVersion(input);
      }
      catch (MissingModelVersionException e) {
        // kenai models were not consistent about versioning. still, they
        // are basically 1.0.0
        return "1.0.0";
      }
    }
  }

  private final SecuritySystem securitySystem; // used for validation

  private final File configurationFile;

  private final KenaiRealmModelReader kenaiRealmModelReader;

  private Configuration configuration;

  @Inject
  public DefaultKenaiRealmConfiguration(final ApplicationConfiguration applicationConfiguration,
                                        final SecuritySystem securitySystem) throws IOException
  {
    this.configurationFile = new File(applicationConfiguration.getConfigurationDirectory(), "kenai-realm.xml");
    this.securitySystem = securitySystem;
    this.kenaiRealmModelReader = new KenaiRealmModelReader();
  }

  @Override
  public synchronized Configuration getConfiguration() {
    if (configuration == null) {
      try {
        configuration = load();
      }
      catch (IOException e) {
        Throwables.propagate(e);
      }
    }
    return configuration;
  }

  protected Configuration load() throws IOException {
    if (!configurationFile.exists()) {
      return new Configuration();
    }
    final Configuration result = ModelloUtils.load(Configuration.MODEL_VERSION, this.configurationFile,
        kenaiRealmModelReader, new ModelloModelUpgrader("1.0.0", Configuration.MODEL_VERSION)
    {
      @Override
      public void doUpgrade(final Reader reader, final Writer writer) throws IOException, XmlPullParserException {
        // no model structure change, merely the version
        final Configuration conf = new KenaiRealmConfigurationXpp3Reader().read(reader);
        conf.setVersion(Configuration.MODEL_VERSION);
        new KenaiRealmConfigurationXpp3Writer().write(writer, conf);
      }
    });
    final ValidationResponse vr = validateConfig(result);
    if (vr.isValid()) {
      return result;
    }
    else {
      log.warn("Invalid Kenai Realm configuration, not using it ", new InvalidConfigurationException(vr));
      return new Configuration();
    }
  }

  private ValidationResponse validateConfig(Configuration config) {
    ValidationResponse response = new ValidationResponse();

    if (StringUtils.isEmpty(config.getBaseUrl())) {
      ValidationMessage msg = new ValidationMessage("baseUrl", "Base Url cannot be empty.");
      response.addValidationError(msg);
    }
    else {
      try {
        new URL(config.getBaseUrl());
      }
      catch (MalformedURLException e) {
        ValidationMessage msg = new ValidationMessage("baseUrl", "Base Url is not valid: " + e.getMessage());
        response.addValidationError(msg);
      }
    }

    if (StringUtils.isEmpty(config.getEmailDomain())) {
      ValidationMessage msg = new ValidationMessage("emailDomain", "Email domain cannot be empty.");
      response.addValidationError(msg);
    }

    if (StringUtils.isEmpty(config.getDefaultRole())) {
      ValidationMessage msg = new ValidationMessage("defaultRole", "Default role cannot be empty.");
      response.addValidationError(msg);
    }
    else {
      // check that this is a valid role
      try {
        this.securitySystem.getAuthorizationManager("default").getRole(config.getDefaultRole());
      }
      catch (Exception e) {
        log.debug("Failed to find role {} during validation.", config.getDefaultRole(), e);
        ValidationMessage msg = new ValidationMessage("defaultRole", "Failed to find role.");
        response.addValidationError(msg);
      }
    }

    return response;
  }
}
