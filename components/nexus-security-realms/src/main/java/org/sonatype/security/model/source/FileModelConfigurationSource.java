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
package org.sonatype.security.model.source;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.security.model.Configuration;
import org.sonatype.security.model.io.xpp3.SecurityConfigurationXpp3Writer;
import org.sonatype.security.model.upgrade.SecurityConfigurationUpgrader;
import org.sonatype.sisu.goodies.common.io.FileReplacer;
import org.sonatype.sisu.goodies.common.io.FileReplacer.ContentWriter;

import org.apache.commons.io.FileUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The default configuration source powered by Modello. It will try to load configuration, upgrade if needed and
 * validate it. It also holds the one and only existing Configuration object.
 *
 * @author cstamas
 */
@Singleton
@Typed(SecurityModelConfigurationSource.class)
@Named("file")
public class FileModelConfigurationSource
    extends AbstractSecurityModelConfigurationSource
{
  /**
   * The configuration file.
   */
  private final File configurationFile;

  /**
   * The configuration upgrader.
   */
  private final SecurityConfigurationUpgrader configurationUpgrader;

  /**
   * The defaults configuration source.
   */
  private final SecurityModelConfigurationSource securityDefaults;

  /**
   * Flag to mark defaulted config
   */
  private boolean configurationDefaulted;

  @Inject
  public FileModelConfigurationSource(@Named("${security-xml-file}") File configurationFile,
                                      @Named("static") SecurityModelConfigurationSource securityDefaults,
                                      SecurityConfigurationUpgrader configurationUpgrader)
  {
    this.configurationFile = configurationFile;
    this.securityDefaults = securityDefaults;
    this.configurationUpgrader = configurationUpgrader;
  }

  /**
   * Gets the configuration file.
   *
   * @return the configuration file
   */
  public File getConfigurationFile() {
    return configurationFile;
  }

  public Configuration loadConfiguration()
      throws ConfigurationException, IOException
  {
    // propagate call and fill in defaults too
    securityDefaults.loadConfiguration();

    if (getConfigurationFile() == null || getConfigurationFile().getAbsolutePath().contains("${")) {
      throw new ConfigurationException("The configuration file is not set or resolved properly: "
          + (getConfigurationFile() == null ? "null" : getConfigurationFile().getAbsolutePath()));
    }

    if (!getConfigurationFile().exists()) {
      getLogger().warn("No configuration file in place, copying the default one and continuing with it.");

      // get the defaults and stick it to place
      setConfiguration(securityDefaults.getConfiguration());

      // check for a configuration before saving
      // if it is null use an empty configuraiton
      if (this.getConfiguration() == null) {
        Configuration configuration = new Configuration();
        configuration.setVersion(Configuration.MODEL_VERSION);
        this.setConfiguration(configuration);
      }

      saveConfiguration(getConfigurationFile());

      configurationDefaulted = true;
    }
    else {
      configurationDefaulted = false;
    }

    loadConfiguration(getConfigurationFile());

    // check for loaded model
    if (getConfiguration() == null) {
      upgradeConfiguration(getConfigurationFile());

      loadConfiguration(getConfigurationFile());
    }

    return getConfiguration();
  }

  public void storeConfiguration()
      throws IOException
  {
    saveConfiguration(getConfigurationFile());
  }

  public InputStream getConfigurationAsStream()
      throws IOException
  {
    return new BufferedInputStream(new FileInputStream(getConfigurationFile()));
  }

  @Override
  public SecurityModelConfigurationSource getDefaultsSource() {
    return securityDefaults;
  }

  protected void upgradeConfiguration(File file)
      throws IOException, ConfigurationException
  {
    getLogger().info("Trying to upgrade the configuration file " + file.getAbsolutePath());

    setConfiguration(configurationUpgrader.loadOldConfiguration(file));

    // after all we should have a configuration
    if (getConfiguration() == null) {
      throw new ConfigurationException("Could not upgrade Security configuration! Please replace the "
          + file.getAbsolutePath() + " file with a valid Security configuration file.");
    }

    getLogger().info("Creating backup from the old file and saving the upgraded configuration.");

    // backup the file
    backupConfiguration();

    // set the upgradeInstance to warn the application about this
    setConfigurationUpgraded(true);

    saveConfiguration(file);
  }

  /**
   * Load configuration.
   *
   * @param file the file
   * @return the configuration
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void loadConfiguration(File file)
      throws IOException
  {
    getLogger().info("Loading security configuration from: " + file.getAbsolutePath());

    InputStream input = null;
    try {
      input = new BufferedInputStream(new FileInputStream(file));

      loadConfiguration(input);
    }
    finally {
      if (input != null) {
        input.close();
      }
    }
  }

  /**
   * Creates a directory. Fails only if directory creation fails, otherwise cleanly returns. If cleanly returns,
   * it is guaranteed that passed in path is created (with all parents as needed) successfully. Unlike Java7
   * {@link Files#createDirectories(Path, FileAttribute[])} method, this method does support paths having last
   * path element a symlink too. In this case, it's verified that symlink points to a directory and is readable.
   */
  private static void mkdir(final Path dir) throws IOException {
    try {
      Files.createDirectories(dir);
    }
    catch (FileAlreadyExistsException e) {
      // this happens when last element of path exists, but is a symlink.
      // A simple test with Files.isDirectory should be able to  detect this
      // case as by default, it follows symlinks.
      if (!Files.isDirectory(dir)) {
        throw e;
      }
    }
  }

  /**
   * Save configuration.
   *
   * @param file the file
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void saveConfiguration(final File file)
      throws IOException
  {
    // Create the dir if doesn't exist, throw runtime exception on failure
    // bad bad bad
    try {
      mkdir(file.getParentFile().toPath());
    }
    catch (IOException e) {
      final String message =
          "\r\n******************************************************************************\r\n"
              + "* Could not create configuration file [ "
              + file.toString()
              + "]!!!! *\r\n"
              + "* Application cannot start properly until the process has read+write permissions to this folder *\r\n"
              + "******************************************************************************";

      getLogger().error(message);
      throw new IOException("Could not create configuration file " + file.getAbsolutePath(), e);
    }

    final Configuration configuration = getConfiguration();
    checkNotNull(configuration, "Missing security configuration");

    // perform the "safe save"
    getLogger().debug("Saving configuration: {}", file);
    final FileReplacer fileReplacer = new FileReplacer(file);
    fileReplacer.setDeleteBackupFile(true);

    fileReplacer.replace(new ContentWriter()
    {
      @Override
      public void write(final BufferedOutputStream output)
          throws IOException
      {
        new SecurityConfigurationXpp3Writer().write(output, configuration);
      }
    });
  }

  /**
   * Was the active configuration fetched from config file or from default source? True if it from default source.
   */
  public boolean isConfigurationDefaulted() {
    return configurationDefaulted;
  }

  @Override
  public void backupConfiguration()
      throws IOException
  {
    File file = getConfigurationFile();

    File backup = new File(file.getParentFile(), file.getName() + ".bak");

    FileUtils.copyFile(file, backup);
  }

}
