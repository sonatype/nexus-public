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
package org.sonatype.security.ldap.realms.persist;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.locks.ReentrantLock;

import org.sonatype.nexus.configuration.ModelUtils.CorruptModelException;
import org.sonatype.nexus.configuration.ModelUtils.MissingModelVersionException;
import org.sonatype.nexus.configuration.ModelUtils.Versioned;
import org.sonatype.nexus.configuration.ModelloUtils;
import org.sonatype.nexus.configuration.ModelloUtils.ModelloModelReader;
import org.sonatype.nexus.configuration.ModelloUtils.ModelloModelUpgrader;
import org.sonatype.nexus.configuration.ModelloUtils.ModelloModelWriter;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.security.ldap.dao.LdapAuthConfiguration;
import org.sonatype.security.ldap.realms.persist.model.CConnectionInfo;
import org.sonatype.security.ldap.realms.persist.model.CUserAndGroupAuthConfiguration;
import org.sonatype.security.ldap.realms.persist.model.Configuration;
import org.sonatype.security.ldap.realms.persist.model.io.xpp3.LdapConfigurationXpp3Reader;
import org.sonatype.security.ldap.realms.persist.model.io.xpp3.LdapConfigurationXpp3Writer;
import org.sonatype.security.ldap.upgrade.cipher.PlexusCipherException;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.base.Strings;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractLdapConfiguration
    extends ComponentSupport
    implements LdapConfiguration
{
  /**
   * Model reader that carefully handles XML version field, as it does not exists always..
   */
  private static class LdapModelReader
      extends ModelloModelReader<Configuration>
      implements Versioned
  {
    private final LdapConfigurationXpp3Reader modelloReader = new LdapConfigurationXpp3Reader();

    @Override
    public Configuration doRead(final Reader reader) throws IOException, XmlPullParserException {
      return modelloReader.read(reader);
    }

    @Override
    public String readVersion(final InputStream input) throws IOException, CorruptModelException {
      // special handling for versions needed, as we might hit unversioned file
      // older ones never got version written out
      try (final Reader r = new InputStreamReader(input, charset)) {
        try {
          final Xpp3Dom dom = Xpp3DomBuilder.build(r);
          final Xpp3Dom versionNode = dom.getChild("version");
          if (versionNode != null) {
            final String originalFileVersion = versionNode.getValue();
            if (Strings.isNullOrEmpty(originalFileVersion)) {
              throw new MissingModelVersionException("Passed in XML model have empty 'version' node");
            }
            return originalFileVersion;
          }
          else {
            // unversioned, expected in OSS LDAP XML, "lie" 1.0.1
            return "1.0.1";
          }
        }
        catch (XmlPullParserException e) {
          throw new CorruptModelException("Passed in XML model cannot be parsed", e);
        }
      }
    }
  }

  private static class LdapModelWriter
      extends ModelloModelWriter<Configuration>
  {
    private final LdapConfigurationXpp3Writer modelloWriter = new LdapConfigurationXpp3Writer();

    @Override
    public void write(final Writer writer, final Configuration model) throws IOException {
      model.setVersion(Configuration.MODEL_VERSION);
      modelloWriter.write(writer, model);
    }
  }

  private final ConfigurationValidator validator;

  private final PasswordHelper passwordHelper;

  private final File configurationFile;

  private final LdapModelReader ldapModelReader;

  private final LdapModelWriter ldapModelWriter;

  private final ReentrantLock lock = new ReentrantLock();

  private Configuration configuration;

  public AbstractLdapConfiguration(ApplicationConfiguration applicationConfiguration, ConfigurationValidator validator,
                                   PasswordHelper passwordHelper) throws IOException
  {
    checkNotNull(applicationConfiguration);
    this.validator = checkNotNull(validator);
    this.passwordHelper = checkNotNull(passwordHelper);
    this.configurationFile = new File(applicationConfiguration.getConfigurationDirectory(), "ldap.xml");
    this.ldapModelReader = new LdapModelReader();
    this.ldapModelWriter = new LdapModelWriter();
    this.configuration = load();
  }

  @Override
  public CConnectionInfo readConnectionInfo() {
    return getConfiguration().getConnectionInfo();
  }

  @Override
  public CUserAndGroupAuthConfiguration readUserAndGroupConfiguration() {
    return getConfiguration().getUserAndGroupConfig();
  }

  @Override
  public void updateUserAndGroupConfiguration(CUserAndGroupAuthConfiguration userAndGroupConfig)
      throws InvalidConfigurationException
  {
    lock.lock();
    try {
      final ValidationResponse vr = validator.validateUserAndGroupAuthConfiguration(null, userAndGroupConfig);
      if (vr.getValidationErrors().size() > 0) {
        throw new InvalidConfigurationException(vr);
      }
      getConfiguration().setUserAndGroupConfig(userAndGroupConfig);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public void updateConnectionInfo(CConnectionInfo connectionInfo)
      throws InvalidConfigurationException
  {
    lock.lock();
    try {
      final ValidationResponse vr = validator.validateConnectionInfo(null, connectionInfo);
      if (vr.getValidationErrors().size() > 0) {
        throw new InvalidConfigurationException(vr);
      }
      getConfiguration().setConnectionInfo(connectionInfo);
    }
    finally {
      lock.unlock();
    }
  }

  protected Configuration getConfiguration() {
    return configuration;
  }

  protected Configuration load() throws IOException {
    lock.lock();
    try {
      Configuration configuration = ModelloUtils.load(Configuration.MODEL_VERSION, configurationFile, ldapModelReader,
          new ModelloModelUpgrader("1.0.1", Configuration.MODEL_VERSION)
          {
            @Override
            public void doUpgrade(final Reader reader, final Writer writer) throws IOException, XmlPullParserException {
              // no model structure change, merely the version
              final Configuration conf = new LdapConfigurationXpp3Reader().read(reader);
              conf.setVersion(Configuration.MODEL_VERSION);
              new LdapConfigurationXpp3Writer().write(writer, conf);
            }
          });
      final ValidationResponse vr = validator.validateModel(new ValidationRequest(configuration));
      if (vr.getValidationErrors().size() > 0) {
        log.warn("Invalid LDAP configuration, defaulting configuration", new InvalidConfigurationException(vr));
        configuration = getDefaultConfiguration();
      }
      if (configuration.getConnectionInfo() != null
          && StringUtils.isNotEmpty(configuration.getConnectionInfo().getSystemPassword())) {
        try {
          configuration.getConnectionInfo().setSystemPassword(
              passwordHelper.decrypt(configuration.getConnectionInfo().getSystemPassword()));
        }
        catch (PlexusCipherException e) {
          this.log.error(
              "Failed to decrypt password, assuming the password in file: '" + configurationFile.getAbsolutePath()
                  + "' is clear text.", e);
        }
      }
      return configuration;
    }
    catch (FileNotFoundException e) {
      // This is ok, may not exist first time around
      return getDefaultConfiguration();
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public void save() throws IOException {
    lock.lock();
    try {
      final Configuration savedConfiguration = this.configuration.clone();
      // change the password to be encrypted
      if (savedConfiguration.getConnectionInfo() != null
          && StringUtils.isNotEmpty(savedConfiguration.getConnectionInfo().getSystemPassword())) {
        try {
          savedConfiguration.getConnectionInfo().setSystemPassword(
              passwordHelper.encrypt(savedConfiguration.getConnectionInfo().getSystemPassword()));
        }
        catch (PlexusCipherException e) {
          log.error("Failed to encrypt password while storing configuration file", e);
        }
      }
      // perform the "safe save"
      log.debug("Saving configuration: {}", configurationFile);
      ModelloUtils.save(savedConfiguration, configurationFile, ldapModelWriter);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public void clearCache() {
    configuration = null;
  }

  private Configuration getDefaultConfiguration() {

    Configuration defaultConfig = null;

    Reader fr = null;
    InputStream is = null;
    try {
      is = getClass().getResourceAsStream("/META-INF/realms/ldap.xml");
      LdapConfigurationXpp3Reader reader = new LdapConfigurationXpp3Reader();
      fr = new InputStreamReader(is);
      defaultConfig = reader.read(fr);
    }
    catch (IOException e) {
      this.log.error(
          "Failed to read default LDAP Realm configuration.  This may be corrected while the application is running.",
          e);
      defaultConfig = new Configuration();
    }
    catch (XmlPullParserException e) {
      this.log.error(
          "Failed to read default LDAP Realm configuration.  This may be corrected while the application is running.",
          e);
      defaultConfig = new Configuration();
    }
    finally {
      if (fr != null) {
        try {
          fr.close();
        }
        catch (IOException e) {
          // just closing if open
        }
      }

      if (is != null) {
        try {
          is.close();
        }
        catch (IOException e) {
          // just closing if open
        }
      }
    }
    return defaultConfig;
  }

  @Override
  public LdapAuthConfiguration getLdapAuthConfiguration() {
    CUserAndGroupAuthConfiguration userAndGroupsConf = readUserAndGroupConfiguration();
    LdapAuthConfiguration authConfig = new LdapAuthConfiguration();

    authConfig.setEmailAddressAttribute(userAndGroupsConf.getEmailAddressAttribute());
    // authConfig.setPasswordEncoding( userAndGroupsConf.getPreferredPasswordEncoding() );
    authConfig.setUserBaseDn(StringUtils.defaultString(userAndGroupsConf.getUserBaseDn(), ""));
    authConfig.setUserIdAttribute(userAndGroupsConf.getUserIdAttribute());
    authConfig.setUserObjectClass(userAndGroupsConf.getUserObjectClass());
    authConfig.setPasswordAttribute(userAndGroupsConf.getUserPasswordAttribute());
    authConfig.setUserRealNameAttribute(userAndGroupsConf.getUserRealNameAttribute());

    authConfig.setGroupBaseDn(StringUtils.defaultString(userAndGroupsConf.getGroupBaseDn(), ""));
    authConfig.setGroupIdAttribute(userAndGroupsConf.getGroupIdAttribute());
    // authConfig.setGroupMappings( groupMappings )
    authConfig.setGroupMemberAttribute(userAndGroupsConf.getGroupMemberAttribute());
    authConfig.setGroupMemberFormat(userAndGroupsConf.getGroupMemberFormat());
    authConfig.setGroupObjectClass(userAndGroupsConf.getGroupObjectClass());
    authConfig.setUserSubtree(userAndGroupsConf.isUserSubtree());
    authConfig.setGroupSubtree(userAndGroupsConf.isGroupSubtree());
    authConfig.setUserMemberOfAttribute(userAndGroupsConf.getUserMemberOfAttribute());
    authConfig.setLdapGroupsAsRoles(userAndGroupsConf.isLdapGroupsAsRoles());
    authConfig.setLdapFilter(userAndGroupsConf.getLdapFilter());
    return authConfig;
  }
}
