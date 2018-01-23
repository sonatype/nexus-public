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
package org.sonatype.security.configuration.source;

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
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.security.configuration.model.SecurityConfiguration;
import org.sonatype.security.configuration.model.io.xpp3.SecurityConfigurationXpp3Writer;
import org.sonatype.security.configuration.upgrade.SecurityConfigurationUpgrader;
import org.sonatype.sisu.goodies.common.io.FileReplacer;
import org.sonatype.sisu.goodies.common.io.FileReplacer.ContentWriter;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The default configuration source powered by Modello. It will try to load configuration, upgrade if needed and
 * validate it. It also holds the one and only existing Configuration object.
 *
 * @author tstevens
 */
@Singleton
@Typed(SecurityConfigurationSource.class)
@Named("file")
public class FileSecurityConfigurationSource
    extends AbstractSecurityConfigurationSource
{
  /**
   * The configuration file.
   */
  private File configurationFile;

  /**
   * The configuration upgrader.
   */
  private SecurityConfigurationUpgrader configurationUpgrader;

  /**
   * The defaults configuration source.
   */
  private final SecurityConfigurationSource securityDefaults;

  private final PasswordHelper passwordHelper;

  /**
   * Flag to mark defaulted config
   */
  private boolean configurationDefaulted;

  @Inject
  public FileSecurityConfigurationSource(@Named("static") SecurityConfigurationSource securityDefaults,
                                         @Named("${application-conf}/security-configuration.xml")
                                         File configurationFile,
                                         PasswordHelper passwordHelper,
                                         SecurityConfigurationUpgrader configurationUpgrader)
  {
    this.securityDefaults = securityDefaults;
    this.configurationFile = configurationFile;
    this.passwordHelper = passwordHelper;
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

  public SecurityConfiguration loadConfiguration()
      throws ConfigurationException, IOException
  {
    // propagate call and fill in defaults too
    securityDefaults.loadConfiguration();

    if (getConfigurationFile() == null || getConfigurationFile().getAbsolutePath().contains("${")) {
      throw new ConfigurationException("The configuration file is not set or resolved properly: "
          + (getConfigurationFile() == null ? "null" : getConfigurationFile().getAbsolutePath()));
    }

    if (!getConfigurationFile().exists()) {
      this.getLogger().warn("No configuration file in place, copying the default one and continuing with it.");

      // get the defaults and stick it to place
      setConfiguration(securityDefaults.getConfiguration());

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
    else {
      // was able to load configuration w/o upgrading it

      if (passwordHelper.foundLegacyEncoding()) {
        getLogger().info("Re-encoding entries using new master phrase");
        saveConfiguration(getConfigurationFile());
      }
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
    return new FileInputStream(getConfigurationFile());
  }

  public SecurityConfigurationSource getDefaultsSource() {
    return securityDefaults;
  }

  protected void upgradeConfiguration(File file)
      throws IOException,
             ConfigurationException
  {
    this.getLogger().info("Trying to upgrade the security configuration file {}", file.getAbsolutePath());

    setConfiguration(configurationUpgrader.loadOldConfiguration(file));

    // after all we should have a configuration
    if (getConfiguration() == null) {
      throw new ConfigurationException("Could not upgrade Security configuration! Please replace the "
          + file.getAbsolutePath() + " file with a valid Security configuration file.");
    }

    // Need to decrypt the anonymous user's password
    SecurityConfiguration configuration = this.getConfiguration();
    if (StringUtils.isNotEmpty(configuration.getAnonymousPassword())) {
      String encryptedPassword = configuration.getAnonymousPassword();
      try {
        configuration.setAnonymousPassword(this.passwordHelper.decrypt(encryptedPassword));
      }
      catch (PlexusCipherException e) {
        this.getLogger().error(
            "Failed to decrypt anonymous user's password in security-configuration.xml, password might be encrypted in memory.",
            e);
      }
    }

    this.getLogger().info("Creating backup from the old file and saving the upgraded security configuration.");

    // backup the file
    File backup = new File(file.getParentFile(), file.getName() + ".bak");

    FileUtils.copyFile(file, backup);

    // set the upgradeInstance to warn the application about this
    setConfigurationUpgraded(true);

    saveConfiguration(file);
  }

  /**
   * Load configuration.
   */
  private void loadConfiguration(File file)
      throws IOException
  {
    this.getLogger().info("Loading Security configuration from {}", file.getAbsolutePath());

    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);

      loadConfiguration(fis);

      // decrypte the anon users password
      SecurityConfiguration configuration = this.getConfiguration();
      if (configuration != null && StringUtils.isNotEmpty(configuration.getAnonymousPassword())) {
        String encryptedPassword = configuration.getAnonymousPassword();
        try {
          configuration.setAnonymousPassword(this.passwordHelper.decrypt(encryptedPassword));
        }
        catch (PlexusCipherException e) {
          this.getLogger().error(
              "Failed to decrype anonymous user's password in security-configuration.xml, password might be encrypted in memory.",
              e);
        }
      }
    }
    finally {
      if (fis != null) {
        fis.close();
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

    final SecurityConfiguration configuration = getConfiguration();
    checkNotNull(configuration, "Missing security configuration");
    // store clear text password, as we have to encrypt the persisted password
    final String clearPassword = configuration.getAnonymousPassword();
    try {
      configuration.setAnonymousPassword(passwordHelper.encrypt(clearPassword));
    }
    catch (PlexusCipherException e) {
      getLogger().warn(
          "Filed to encrypte the anonymous users password, storing configuration with cleartext password!", e);
    }

    try {
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
    finally {

      // set back to clear text
      configuration.setAnonymousPassword(clearPassword);
    }
  }

  /**
   * Was the active configuration fetched from config file or from default source? True if it from default source.
   */
  public boolean isConfigurationDefaulted() {
    return configurationDefaulted;
  }
}
