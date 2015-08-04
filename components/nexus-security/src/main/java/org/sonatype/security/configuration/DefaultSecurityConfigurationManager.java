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
package org.sonatype.security.configuration;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.security.configuration.model.SecurityConfiguration;
import org.sonatype.security.configuration.source.SecurityConfigurationSource;
import org.sonatype.security.configuration.validator.SecurityConfigurationValidator;
import org.sonatype.security.configuration.validator.SecurityValidationContext;
import org.sonatype.sisu.goodies.common.ComponentSupport;

@Singleton
@Typed(SecurityConfigurationManager.class)
@Named("default")
public class DefaultSecurityConfigurationManager
    extends ComponentSupport
    implements SecurityConfigurationManager
{
  private final SecurityConfigurationSource configurationSource;

  private final SecurityConfigurationValidator validator;

  /**
   * This will hold the current configuration in memory, to reload, will need to set this to null
   */
  private SecurityConfiguration configuration = null;

  private ReentrantLock lock = new ReentrantLock();

  @Inject
  public DefaultSecurityConfigurationManager(@Named("file") SecurityConfigurationSource configurationSource,
                                             SecurityConfigurationValidator validator)
  {
    this.configurationSource = configurationSource;
    this.validator = validator;
  }

  public boolean isAnonymousAccessEnabled() {
    return this.getConfiguration().isAnonymousAccessEnabled();
  }

  public void setAnonymousAccessEnabled(boolean anonymousAccessEnabled) {
    this.getConfiguration().setAnonymousAccessEnabled(anonymousAccessEnabled);
  }

  public String getAnonymousPassword() {
    return this.getConfiguration().getAnonymousPassword();
  }

  public void setAnonymousPassword(String anonymousPassword)
      throws InvalidConfigurationException
  {
    ValidationResponse vr = validator.validateAnonymousPassword(this.initializeContext(), anonymousPassword);

    if (vr.isValid()) {
      this.getConfiguration().setAnonymousPassword(anonymousPassword);
    }
    else {
      throw new InvalidConfigurationException(vr);
    }
  }

  public String getAnonymousUsername() {
    return this.getConfiguration().getAnonymousUsername();
  }

  public void setAnonymousUsername(String anonymousUsername)
      throws InvalidConfigurationException
  {
    ValidationResponse vr = validator.validateAnonymousUsername(this.initializeContext(), anonymousUsername);

    if (vr.isValid()) {
      this.getConfiguration().setAnonymousUsername(anonymousUsername);
    }
    else {
      throw new InvalidConfigurationException(vr);
    }
  }

  public int getHashIterations() {
    return this.getConfiguration().getHashIterations();
  }

  public List<String> getRealms() {
    return Collections.unmodifiableList(this.getConfiguration().getRealms());
  }

  public void setRealms(List<String> realms)
      throws InvalidConfigurationException
  {
    ValidationResponse vr = validator.validateRealms(this.initializeContext(), realms);

    if (vr.isValid()) {
      this.getConfiguration().setRealms(realms);
    }
    else {
      throw new InvalidConfigurationException(vr);
    }
  }

  private SecurityConfiguration getConfiguration() {
    if (configuration != null) {
      return configuration;
    }

    lock.lock();

    try {
      this.configurationSource.loadConfiguration();

      configuration = this.configurationSource.getConfiguration();
    }
    catch (IOException e) {
      this.log.error("IOException while retrieving configuration file", e);
    }
    catch (ConfigurationException e) {
      this.log.error("Invalid Configuration", e);
    }
    finally {
      lock.unlock();
    }

    return configuration;
  }

  public void clearCache() {
    // Just to make sure we aren't fiddling w/ save/loading process
    lock.lock();
    configuration = null;
    lock.unlock();
  }

  public void save() {
    lock.lock();

    try {
      this.configurationSource.storeConfiguration();
    }
    catch (IOException e) {
      this.log.error("IOException while storing configuration file", e);
    }
    finally {
      lock.unlock();
    }
  }

  private SecurityValidationContext initializeContext() {
    SecurityValidationContext context = new SecurityValidationContext();
    context.setSecurityConfiguration(this.getConfiguration());

    return context;
  }
}
