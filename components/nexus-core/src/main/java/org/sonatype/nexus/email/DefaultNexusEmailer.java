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
package org.sonatype.nexus.email;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.micromailer.Address;
import org.sonatype.micromailer.EMailer;
import org.sonatype.micromailer.EmailerConfiguration;
import org.sonatype.micromailer.MailRequest;
import org.sonatype.micromailer.MailRequestStatus;
import org.sonatype.micromailer.MailType;
import org.sonatype.micromailer.imp.DefaultMailType;
import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.SystemStatus;
import org.sonatype.nexus.configuration.AbstractLastingConfigurable;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.nexus.configuration.model.CSmtpConfiguration;
import org.sonatype.nexus.configuration.model.CSmtpConfigurationCoreConfiguration;
import org.sonatype.nexus.proxy.events.NexusStoppedEvent;
import org.sonatype.sisu.goodies.common.SimpleFormat;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class DefaultNexusEmailer
    extends AbstractLastingConfigurable<CSmtpConfiguration>
    implements NexusEmailer
{
  private static final Logger LOG = LoggerFactory.getLogger(DefaultNexusEmailer.class);

  /**
   * The "name" of Nexus instance, as displayed on sent mails.
   */
  private static final String NEXUS_SENDER_NAME = "Nexus Repository Manager";

  /**
   * Custom header to deisgnate Nexus instance as sender
   */
  private static final String X_MESSAGE_SENDER_HEADER = "X-EMailer-Mail-Sender";

  private final GlobalRestApiSettings globalRestApiSettings;

  private final ApplicationStatusSource applicationStatusSource;

  private final EMailer eMailer;

  private final List<SmtpSessionParametersCustomizer> customizers;

  @Inject
  public DefaultNexusEmailer(final EventBus eventBus,
                             final ApplicationConfiguration applicationConfiguration,
                             final GlobalRestApiSettings globalRestApiSettings,
                             final ApplicationStatusSource applicationStatusSource,
                             final EMailer eMailer,
                             final List<SmtpSessionParametersCustomizer> customizers)
  {
    super("SMTP Client", eventBus, applicationConfiguration);
    this.globalRestApiSettings = checkNotNull(globalRestApiSettings);
    this.applicationStatusSource = checkNotNull(applicationStatusSource);
    this.eMailer = checkNotNull(eMailer);
    this.customizers = checkNotNull(customizers);
  }

  // ==
  
  @Subscribe
  public void on(final NexusStoppedEvent evt) {
    getEMailer().shutdown();
  }

  // ==

  @Override
  public EMailer getEMailer() {
    return eMailer;
  }

  @Override
  public String getDefaultMailTypeId() {
    return DefaultMailType.DEFAULT_TYPE_ID;
  }

  @Override
  public MailRequest getDefaultMailRequest(String subject, String body) {
    MailRequest request = new MailRequest(getMailId(), getDefaultMailTypeId());

    request.getCustomHeaders().put(X_MESSAGE_SENDER_HEADER, getSenderId());

    request.setFrom(getSMTPSystemEmailAddress());

    request.getBodyContext().put(DefaultMailType.SUBJECT_KEY, subject);

    request.getBodyContext().put(DefaultMailType.BODY_KEY, body);

    return request;
  }

  @Override
  public MailRequestStatus sendMail(MailRequest request) {
    if (request.getFrom() == null) {
      request.setFrom(getSMTPSystemEmailAddress());
    }

    prependNexusBaseUrl(request);

    if (emailSettingsConfigured()) {
      return getEMailer().sendMail(request);
    }

    final String message = SimpleFormat.format(
        "Mail requestId[%s] not sent, SMTP not configured", request.getRequestId()
    );

    LOG.debug(message);

    final MailRequestStatus status = new MailRequestStatus(request);
    status.setErrorCause(new EmailerException(message));
    return status;
  }

  /**
   * Prepend to message body a link to this Nexus instance (base server URL).
   */
  private void prependNexusBaseUrl(final MailRequest request) {
    final String baseNexusUrl = globalRestApiSettings.getBaseUrl();
    final MailType mailType = getEMailer().getMailTypeSource().getMailType(request.getMailTypeId());

    final StringBuilder messageBody = new StringBuilder().append("Message from: ");

    if (mailType != null && mailType.isBodyIsHtml()) {
      messageBody
          .append(
              StringUtils.isNotBlank(baseNexusUrl)
                  ? String.format("<a href=\"%s\">%s</a>", baseNexusUrl, baseNexusUrl)
                  : "<i>(Set the Base URL parameter in Nexus Server Administration to include in future emails)</i>"
          )
          .append("<br><br>");
    }
    else {
      messageBody
          .append(
              StringUtils.isNotBlank(baseNexusUrl)
                  ? baseNexusUrl
                  : "(Set the Base URL parameter in Nexus Server Administration to include in future emails)"
          )
          .append("\n\n");
    }

    messageBody.append(request.getBodyContext().get(DefaultMailType.BODY_KEY));

    request.getBodyContext().put(DefaultMailType.BODY_KEY, messageBody.toString());
  }

  private boolean emailSettingsConfigured() {
    return !("smtp-host".equals(getSMTPHostname())
        && 25 == getSMTPPort()
        && "smtp-username".equals(getSMTPUsername())
    );
  }

  // ==

  @Override
  protected void initializeConfiguration()
      throws ConfigurationException
  {
    if (getApplicationConfiguration().getConfigurationModel() != null) {
      configure(getApplicationConfiguration());
    }
  }

  @Override
  protected CoreConfiguration<CSmtpConfiguration> wrapConfiguration(Object configuration)
      throws ConfigurationException
  {
    if (configuration instanceof ApplicationConfiguration) {
      return new CSmtpConfigurationCoreConfiguration((ApplicationConfiguration) configuration);
    }
    else {
      throw new ConfigurationException("The passed configuration object is of class \""
          + configuration.getClass().getName() + "\" and not the required \""
          + ApplicationConfiguration.class.getName() + "\"!");
    }
  }

  @Override
  public void doConfigure()
      throws ConfigurationException
  {
    super.doConfigure();

    configureEmailer();
  }

  private synchronized void configureEmailer() {
    final EmailerConfiguration config = new NexusEmailerConfiguration(customizers);
    config.setDebug(isSMTPDebug());
    config.setMailHost(getSMTPHostname());
    config.setMailPort(getSMTPPort());
    config.setSsl(isSMTPSslEnabled());
    config.setTls(isSMTPTlsEnabled());
    config.setUsername(getSMTPUsername());
    config.setPassword(getSMTPPassword());

    getEMailer().configure(config);
  }

  // ==

  @Override
  public String getSMTPHostname() {
    return getCurrentConfiguration(false).getHostname();
  }

  @Override
  public void setSMTPHostname(String host) {
    getCurrentConfiguration(true).setHostname(host);
  }

  @Override
  public int getSMTPPort() {
    return getCurrentConfiguration(false).getPort();
  }

  @Override
  public void setSMTPPort(int port) {
    getCurrentConfiguration(true).setPort(port);
  }

  @Override
  public String getSMTPUsername() {
    return getCurrentConfiguration(false).getUsername();
  }

  @Override
  public void setSMTPUsername(String username) {
    getCurrentConfiguration(true).setUsername(username);
  }

  @Override
  public String getSMTPPassword() {
    return getCurrentConfiguration(false).getPassword();
  }

  @Override
  public void setSMTPPassword(String password) {
    getCurrentConfiguration(true).setPassword(password);
  }

  @Override
  public Address getSMTPSystemEmailAddress() {
    return new Address(getCurrentConfiguration(false).getSystemEmailAddress(), NEXUS_SENDER_NAME);
  }

  @Override
  public void setSMTPSystemEmailAddress(Address adr) {
    getCurrentConfiguration(true).setSystemEmailAddress(adr.getMailAddress());
  }

  @Override
  public boolean isSMTPDebug() {
    return getCurrentConfiguration(false).isDebugMode();
  }

  @Override
  public void setSMTPDebug(boolean val) {
    getCurrentConfiguration(true).setDebugMode(val);
  }

  @Override
  public boolean isSMTPSslEnabled() {
    return getCurrentConfiguration(false).isSslEnabled();
  }

  @Override
  public void setSMTPSslEnabled(boolean val) {
    getCurrentConfiguration(true).setSslEnabled(val);
  }

  @Override
  public boolean isSMTPTlsEnabled() {
    return getCurrentConfiguration(false).isTlsEnabled();
  }

  @Override
  public void setSMTPTlsEnabled(boolean val) {
    getCurrentConfiguration(true).setTlsEnabled(val);
  }

  // ==

  protected String getMailId() {
    StringBuilder sb = new StringBuilder("NX");

    sb.append(String.valueOf(System.currentTimeMillis()));

    return sb.toString();

  }

  // ==
  // TODO: this is a workaround, see NXCM-363

  /**
   * The edtion, that will tell us is there some change happened with installation.
   */
  private String platformEditionShort;

  /**
   * The lazily calculated invariant part of the UserAgentString.
   */
  private String userAgentPlatformInfo;

  protected String getSenderId() {
    SystemStatus status = applicationStatusSource.getSystemStatus();

    if (platformEditionShort == null || !platformEditionShort.equals(status.getEditionShort())
        || userAgentPlatformInfo == null) {
      // make it "remember" to be able to detect license changes later
      platformEditionShort = status.getEditionShort();

      userAgentPlatformInfo =
          new StringBuilder("Nexus/").append(status.getVersion()).append(" (")
              .append(status.getEditionShort()).append("; ").append(System.getProperty("os.name"))
              .append("; ").append(System.getProperty("os.version")).append("; ")
              .append(System.getProperty("os.arch")).append("; ")
              .append(System.getProperty("java.version")).append(") ").toString();
    }

    return userAgentPlatformInfo;
  }
}
