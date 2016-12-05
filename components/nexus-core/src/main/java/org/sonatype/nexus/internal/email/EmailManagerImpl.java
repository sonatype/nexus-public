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
package org.sonatype.nexus.internal.email;

import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.Session;
import javax.net.ssl.SSLContext;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.Mutex;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.email.EmailConfigurationChangedEvent;
import org.sonatype.nexus.email.EmailManager;
import org.sonatype.nexus.ssl.TrustStore;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link EmailManager}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class EmailManagerImpl
    extends ComponentSupport
    implements EmailManager, EventAware
{
  private final EventManager eventManager;

  private final EmailConfigurationStore store;

  private final TrustStore trustStore;

  private final Provider<EmailConfiguration> defaults;

  private final Mutex lock = new Mutex();

  private EmailConfiguration configuration;

  @Inject
  public EmailManagerImpl(final EventManager eventManager,
                          final EmailConfigurationStore store,
                          final TrustStore trustStore,
                          @Named("initial") final Provider<EmailConfiguration> defaults)
  {
    this.eventManager = checkNotNull(eventManager);
    this.store = checkNotNull(store);
    this.trustStore = checkNotNull(trustStore);
    this.defaults = checkNotNull(defaults);
  }

  //
  // Configuration
  //

  /**
   * Load configuration from store, or use defaults.
   */
  private EmailConfiguration loadConfiguration() {
    EmailConfiguration model = store.load();

    // use defaults if no configuration was loaded from the store
    if (model == null) {
      model = defaults.get();

      // default config must not be null
      checkNotNull(model);

      log.info("Using default configuration: {}", model);
    }
    else {
      log.info("Loaded configuration: {}", model);
    }

    return model;
  }

  /**
   * Return configuration, loading if needed.
   *
   * The result model should be considered _immutable_ unless copied.
   */
  private EmailConfiguration getConfigurationInternal() {
    synchronized (lock) {
      if (configuration == null) {
        configuration = loadConfiguration();
      }
      return configuration;
    }
  }

  @Override
  public EmailConfiguration getConfiguration() {
    return getConfigurationInternal().copy();
  }

  @Override
  public void setConfiguration(final EmailConfiguration configuration) {
    checkNotNull(configuration);

    EmailConfiguration model = configuration.copy();
    // TODO: Validate configuration before saving?  Or leave to ext.direct?

    log.info("Saving configuration: {}", model);
    synchronized (lock) {
      store.save(model);
      this.configuration = model;
    }

    eventManager.post(new EmailConfigurationChangedEvent(model));
  }

  //
  // Mail sending
  //

  /**
   * Apply server configuration to email.
   */
  @VisibleForTesting
  Email apply(final EmailConfiguration configuration, final Email mail) throws EmailException {
    mail.setHostName(configuration.getHost());
    mail.setSmtpPort(configuration.getPort());
    if (!Strings.isNullOrEmpty(configuration.getUsername()) || !Strings.isNullOrEmpty(configuration.getPassword())) {
      mail.setAuthentication(configuration.getUsername(), configuration.getPassword());
    }

    mail.setStartTLSEnabled(configuration.isStartTlsEnabled());
    mail.setStartTLSRequired(configuration.isStartTlsRequired());
    mail.setSSLOnConnect(configuration.isSslOnConnectEnabled());
    mail.setSSLCheckServerIdentity(configuration.isSslCheckServerIdentityEnabled());
    mail.setSslSmtpPort(Integer.toString(configuration.getPort()));

    // default from address
    if (mail.getFromAddress() == null) {
      mail.setFrom(configuration.getFromAddress());
    }

    // apply subject prefix if configured
    String subjectPrefix = configuration.getSubjectPrefix();
    if (subjectPrefix != null) {
      String subject = mail.getSubject();
      mail.setSubject(String.format("%s %s", subjectPrefix, subject));
    }

    // do this last (mail properties are set up from the email fields when you get the mail session)
    if (configuration.isNexusTrustStoreEnabled()) {
      SSLContext context = trustStore.getSSLContext();
      Session session = mail.getMailSession();
      Properties properties = session.getProperties();
      properties.remove(EmailConstants.MAIL_SMTP_SOCKET_FACTORY_CLASS);
      properties.put(EmailConstants.MAIL_SMTP_SSL_ENABLE, true);
      properties.put("mail.smtp.ssl.socketFactory", context.getSocketFactory());
    }

    return mail;
  }

  @Override
  public void send(final Email mail) throws EmailException {
    checkNotNull(mail);

    EmailConfiguration model = getConfigurationInternal();
    if (model.isEnabled()) {
      Email prepared = apply(model, mail);
      prepared.send();
    }
    else {
      log.warn("No email enabled but asked to send anyway.");
    }
  }

  @Override
  public void sendVerification(final EmailConfiguration configuration, final String address) throws EmailException {
    checkNotNull(configuration);
    checkNotNull(address);

    Email mail = new SimpleEmail();
    mail.setSubject("Email configuration verification");
    mail.addTo(address);
    mail.setMsg("Verification successful");
    mail = apply(configuration, mail);
    mail.send();
  }

  @Subscribe
  public void onStoreChanged(final EmailConfigurationEvent event) {
    if (!event.isLocal()) {
      log.debug("Reloading configuration after change by node {}", event.getRemoteNodeId());
      EmailConfiguration model;
      synchronized (lock) {
        configuration = model = loadConfiguration();
      }
      eventManager.post(new EmailConfigurationChangedEvent(model));
    }
  }

}
