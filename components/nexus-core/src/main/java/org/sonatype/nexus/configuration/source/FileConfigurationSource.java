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
package org.sonatype.nexus.configuration.source;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationRequest;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.configuration.application.upgrade.ApplicationConfigurationUpgrader;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.configuration.model.ConfigurationHelper;
import org.sonatype.nexus.configuration.model.io.xpp3.NexusConfigurationXpp3Writer;
import org.sonatype.nexus.configuration.validator.ApplicationConfigurationValidator;
import org.sonatype.nexus.configuration.validator.ConfigurationValidator;
import org.sonatype.nexus.util.ApplicationInterpolatorProvider;
import org.sonatype.nexus.util.file.DirSupport;
import org.sonatype.security.events.SecurityConfigurationChanged;
import org.sonatype.sisu.goodies.common.io.FileReplacer;
import org.sonatype.sisu.goodies.common.io.FileReplacer.ContentWriter;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The default configuration source powered by Modello. It will try to load configuration, upgrade if needed and
 * validate it. It also holds the one and only existing Configuration object.
 *
 * @author cstamas
 */
@Singleton
@Named("file")
public class FileConfigurationSource
    extends AbstractApplicationConfigurationSource
{

  private final EventBus eventBus;

  private final ApplicationStatusSource applicationStatusSource;

  /**
   * The configuration file.
   */
  private final File configurationFile;

  /**
   * The configuration validator.
   */
  private final ApplicationConfigurationValidator configurationValidator;

  /**
   * The configuration upgrader.
   */
  private final ApplicationConfigurationUpgrader configurationUpgrader;

  /**
   * The nexus defaults configuration source.
   */
  private final ApplicationConfigurationSource nexusDefaults;

  private final ConfigurationHelper configHelper;

  /**
   * Flag to mark defaulted config
   */
  private boolean configurationDefaulted;

  @Inject
  public FileConfigurationSource(final ApplicationInterpolatorProvider interpolatorProvider,
                                 final EventBus eventBus,
                                 final ApplicationStatusSource applicationStatusSource,
                                 final @Named("${nexus-work}/conf/nexus.xml") File configurationFile,
                                 final ApplicationConfigurationValidator configurationValidator,
                                 final ApplicationConfigurationUpgrader configurationUpgrader,
                                 final @Named("static") ApplicationConfigurationSource nexusDefaults,
                                 final ConfigurationHelper configHelper)
  {
    super(interpolatorProvider);
    this.eventBus = checkNotNull(eventBus);
    this.applicationStatusSource = checkNotNull(applicationStatusSource);
    this.configurationFile = checkNotNull(configurationFile);
    this.configurationValidator = checkNotNull(configurationValidator);
    this.configurationUpgrader = checkNotNull(configurationUpgrader);
    this.nexusDefaults = checkNotNull(nexusDefaults);
    this.configHelper = checkNotNull(configHelper);
  }

  /**
   * Gets the configuration validator.
   *
   * @return the configuration validator
   */
  public ConfigurationValidator getConfigurationValidator() {
    return configurationValidator;
  }

  /**
   * Gets the configuration file.
   *
   * @return the configuration file
   */
  public File getConfigurationFile() {
    return configurationFile;
  }

  @Override
  public Configuration loadConfiguration()
      throws ConfigurationException, IOException
  {
    // propagate call and fill in defaults too
    nexusDefaults.loadConfiguration();

    if (getConfigurationFile() == null || getConfigurationFile().getAbsolutePath().contains("${")) {
      throw new ConfigurationException("The configuration file is not set or resolved properly: "
          + getConfigurationFile().getAbsolutePath());
    }

    if (!getConfigurationFile().exists()) {
      log.warn("No configuration file in place, copying the default one and continuing with it.");

      // get the defaults and stick it to place
      setConfiguration(nexusDefaults.getConfiguration());

      saveConfiguration(getConfigurationFile());

      configurationDefaulted = true;
    }
    else {
      configurationDefaulted = false;
    }

    try {
      loadConfiguration(getConfigurationFile());

      // was able to load configuration w/o upgrading it
      setConfigurationUpgraded(false);
    }
    catch (ConfigurationException e) {
      log.info("Configuration file is outdated, begin upgrade");

      upgradeConfiguration(getConfigurationFile());

      // had to upgrade configuration before I was able to load it
      setConfigurationUpgraded(true);

      loadConfiguration(getConfigurationFile());

      // if the configuration is upgraded we need to reload the security.
      // it would be great if this was put somewhere else, but I am out of ideas.
      // the problem is the default security was already loaded with the security-system component was loaded
      // so it has the defaults, the upgrade from 1.0.8 -> 1.4 moves security out of the nexus.xml
      // and we cannot use the 'correct' way of updating the info, because that would cause an infinit loop
      // loading the nexus.xml
      this.eventBus.post(new SecurityConfigurationChanged());
    }

    upgradeNexusVersion();

    ValidationResponse vResponse =
        getConfigurationValidator().validateModel(new ValidationRequest(getConfiguration()));

    dumpValidationErrors(vResponse);

    setValidationResponse(vResponse);

    if (vResponse.isValid()) {
      if (vResponse.isModified()) {
        log.info("Validation has modified the configuration, storing the changes.");

        storeConfiguration();
      }

      return getConfiguration();
    }
    else {
      throw new InvalidConfigurationException(vResponse);
    }
  }

  protected void dumpValidationErrors(final ValidationResponse response) {
    // summary
    if (response.getValidationErrors().size() > 0 || response.getValidationWarnings().size() > 0) {
      log.error("* * * * * * * * * * * * * * * * * * * * * * * * * *");

      log.error("Nexus configuration has validation errors/warnings");

      log.error("* * * * * * * * * * * * * * * * * * * * * * * * * *");

      if (response.getValidationErrors().size() > 0) {
        log.error("The ERRORS:");

        for (ValidationMessage msg : response.getValidationErrors()) {
          log.error(msg.toString());
        }
      }

      if (response.getValidationWarnings().size() > 0) {
        log.error("The WARNINGS:");

        for (ValidationMessage msg : response.getValidationWarnings()) {
          log.error(msg.toString());
        }
      }

      log.error("* * * * * * * * * * * * * * * * * * * * *");
    }
    else {
      log.info("Nexus configuration validated successfully.");
    }
  }

  protected void upgradeNexusVersion()
      throws IOException
  {
    final String currentVersion = checkNotNull(applicationStatusSource.getSystemStatus().getVersion());
    final String previousVersion = getConfiguration().getNexusVersion();
    if (currentVersion.equals(previousVersion)) {
      setInstanceUpgraded(false);
    }
    else {
      setInstanceUpgraded(true);
      getConfiguration().setNexusVersion(currentVersion);
      storeConfiguration();
    }

  }

  @Override
  public void storeConfiguration()
      throws IOException
  {
    saveConfiguration(getConfigurationFile());
  }

  @Override
  public InputStream getConfigurationAsStream()
      throws IOException
  {
    return new FileInputStream(getConfigurationFile());
  }

  @Override
  public ApplicationConfigurationSource getDefaultsSource() {
    return nexusDefaults;
  }

  protected void upgradeConfiguration(File file)
      throws IOException, ConfigurationException
  {
    log.info("Trying to upgrade the configuration file " + file.getAbsolutePath());

    setConfiguration(configurationUpgrader.loadOldConfiguration(file));

    // after all we should have a configuration
    if (getConfiguration() == null) {
      throw new ConfigurationException("Could not upgrade Nexus configuration! Please replace the "
          + file.getAbsolutePath() + " file with a valid Nexus configuration file.");
    }

    log.info("Creating backup from the old file and saving the upgraded configuration.");

    backupConfiguration();

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
      throws IOException, ConfigurationException
  {
    log.debug("Loading Nexus configuration from " + file.getAbsolutePath());

    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);

      loadConfiguration(fis);

      // seems a bit dirty, but the config might need to be upgraded.
      if (this.getConfiguration() != null) {
        // decrypt the passwords
        setConfiguration(configHelper.encryptDecryptPasswords(getConfiguration(), false));
      }
    }
    finally {
      if (fis != null) {
        fis.close();
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
      DirSupport.mkdir(file.getParentFile().toPath());
    }
    catch (IOException e) {
      String message =
          "\r\n******************************************************************************\r\n"
              + "* Could not create configuration file [ " + file.toString() + "]!!!! *\r\n"
              + "* Nexus cannot start properly until the process has read+write permissions to this folder *\r\n"
              + "******************************************************************************";

      log.error(message, e);
      throw new IOException("Could not create configuration file " + file.getAbsolutePath(), e);
    }

    // Clone the conf so we can encrypt the passwords
    final Configuration configuration = configHelper.encryptDecryptPasswords(getConfiguration(), true);
    log.debug("Saving configuration: {}", file);
    final FileReplacer fileReplacer = new FileReplacer(file);
    // we save this file many times, don't litter backups
    fileReplacer.setDeleteBackupFile(true);
    fileReplacer.replace(new ContentWriter()
    {
      @Override
      public void write(final BufferedOutputStream output)
          throws IOException
      {
        new NexusConfigurationXpp3Writer().write(output, configuration);
      }
    });
  }

  /**
   * Was the active configuration fetched from config file or from default source? True if it from default source.
   */
  @Override
  public boolean isConfigurationDefaulted() {
    return configurationDefaulted;
  }

  @Override
  public void backupConfiguration()
      throws IOException
  {
    File file = getConfigurationFile();

    // backup the file
    File backup = new File(file.getParentFile(), file.getName() + ".bak");
    Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
  }
}