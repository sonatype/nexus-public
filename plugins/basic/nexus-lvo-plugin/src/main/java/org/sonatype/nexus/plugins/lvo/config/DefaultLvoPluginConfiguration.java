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
package org.sonatype.nexus.plugins.lvo.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.ModelUtils.CorruptModelException;
import org.sonatype.nexus.configuration.ModelUtils.Versioned;
import org.sonatype.nexus.configuration.ModelloUtils;
import org.sonatype.nexus.configuration.ModelloUtils.ModelloModelReader;
import org.sonatype.nexus.configuration.ModelloUtils.ModelloModelUpgrader;
import org.sonatype.nexus.configuration.ModelloUtils.ModelloModelWriter;
import org.sonatype.nexus.configuration.ModelloUtils.VersionedInFieldXmlModelloModelHelper;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.plugins.lvo.NoSuchKeyException;
import org.sonatype.nexus.plugins.lvo.config.model.CLvoKey;
import org.sonatype.nexus.plugins.lvo.config.model.Configuration;
import org.sonatype.nexus.plugins.lvo.config.model.io.xpp3.NexusLvoPluginConfigurationXpp3Reader;
import org.sonatype.nexus.plugins.lvo.config.model.io.xpp3.NexusLvoPluginConfigurationXpp3Writer;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration for LVO plugin. There is no "real" upgrade happening, as model version changed in Nexus 1.x "era", and
 * it's version aligned with Nexus core, but the model itself is unchanged from the model used in Nexus 2.0 (was
 * 1.0.1).
 */
@Named
@Singleton
public class DefaultLvoPluginConfiguration
    extends ComponentSupport
    implements LvoPluginConfiguration
{
  private static class LvoModelReader
      extends ModelloModelReader<Configuration>
      implements Versioned
  {
    private final VersionedInFieldXmlModelloModelHelper versionedHelper = new VersionedInFieldXmlModelloModelHelper(
        "version");

    private final NexusLvoPluginConfigurationXpp3Reader modelloReader = new NexusLvoPluginConfigurationXpp3Reader();

    @Override
    public Configuration doRead(final Reader reader) throws IOException, XmlPullParserException {
      return modelloReader.read(reader);
    }

    @Override
    public String readVersion(final InputStream input) throws IOException, CorruptModelException {
      // NEXUS-6099: Treat "1.0.0" XML as "1.0.1" as those are backward compatible
      // But it seems we still have instances with mismanaged LVO XML versions out there
      final String version = versionedHelper.readVersion(input);
      if ("1.0.0".equals(version)) {
        return "1.0.1";
      }
      else {
        return version;
      }
    }
  }

  private static class LvoModelWriter
      extends ModelloModelWriter<Configuration>
  {
    private final NexusLvoPluginConfigurationXpp3Writer modelloWriter = new NexusLvoPluginConfigurationXpp3Writer();

    @Override
    public void write(final Writer writer, final Configuration model) throws IOException {
      model.setVersion(Configuration.MODEL_VERSION);
      modelloWriter.write(writer, model);
    }
  }

  private final File configurationFile;

  private final LvoModelReader lvoModelReader;

  private final LvoModelWriter lvoModelWriter;

  @Inject
  public DefaultLvoPluginConfiguration(final ApplicationConfiguration applicationConfiguration) throws IOException {
    checkNotNull(applicationConfiguration);
    this.configurationFile = new File(applicationConfiguration.getConfigurationDirectory(), "lvo-plugin.xml");
    this.lvoModelReader = new LvoModelReader();
    this.lvoModelWriter = new LvoModelWriter();
    this.configuration = load();
  }

  private Configuration configuration;

  public synchronized CLvoKey getLvoKey(String key)
      throws NoSuchKeyException
  {
    if (StringUtils.isEmpty(key)) {
      throw new NoSuchKeyException(key);
    }

    try {
      Configuration c = getConfiguration();

      for (CLvoKey lvoKey : c.getLvoKeys()) {
        if (key.equals(lvoKey.getKey())) {
          return lvoKey;
        }
      }

      throw new NoSuchKeyException(key);
    }
    catch (Exception e) {
      throw new NoSuchKeyException(key);
    }
  }

  public synchronized boolean isEnabled() {
    try {
      return getConfiguration().isEnabled();
    }
    catch (IOException e) {
      log.error("Unable to read configuration", e);
    }

    return false;
  }

  public synchronized void enable() throws IOException {
    getConfiguration().setEnabled(true);
    save();
  }

  public synchronized void disable() throws IOException {
    getConfiguration().setEnabled(false);
    save();
  }

  protected Configuration getConfiguration() throws IOException {
    return configuration;
  }

  protected synchronized Configuration load() throws IOException {
    if (!configurationFile.exists()) {
      // This is ok, may not exist first time around, default it
      FileUtils.copyURLToFile(
          getClass().getResource("/META-INF/nexus-lvo-plugin/lvo-plugin.xml"),
          configurationFile);
    }
    return ModelloUtils.load(Configuration.MODEL_VERSION, this.configurationFile,
        lvoModelReader, new ModelloModelUpgrader("1.0.1", Configuration.MODEL_VERSION)
    {
      @Override
      public void doUpgrade(final Reader reader, final Writer writer) throws IOException, XmlPullParserException {
        // no model structure change, merely the version
        final Configuration conf = new NexusLvoPluginConfigurationXpp3Reader().read(reader);
        conf.setVersion(Configuration.MODEL_VERSION);
        new NexusLvoPluginConfigurationXpp3Writer().write(writer, conf);
      }
    });
  }

  protected synchronized void save() throws IOException {
    ModelloUtils.save(configuration, configurationFile, lvoModelWriter);
  }

}
