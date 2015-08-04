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
package org.sonatype.nexus.rest.global;

import java.net.UnknownHostException;
import java.security.cert.CertPathBuilderException;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import org.sonatype.nexus.configuration.model.CSmtpConfiguration;
import org.sonatype.nexus.email.EmailerException;
import org.sonatype.nexus.email.SmtpSettingsValidator;
import org.sonatype.nexus.rest.model.HtmlUnescapeStringConverter;
import org.sonatype.nexus.rest.model.SmtpSettingsResource;
import org.sonatype.nexus.rest.model.SmtpSettingsResourceRequest;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResourceException;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang.StringUtils;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * The Smtp settings validation resource.
 *
 * @author velo
 */
@Named
@Singleton
@Path(SmtpSettingsValidationPlexusResource.RESOURCE_URI)
@Consumes({"application/xml", "application/json"})
public class SmtpSettingsValidationPlexusResource
    extends AbstractGlobalConfigurationPlexusResource
{
  public static final String RESOURCE_URI = "/check_smtp_settings";

  private static final Pattern EMAIL_PATTERN = Pattern.compile(".+@.+\\.[a-zA-Z]+");

  private final SmtpSettingsValidator emailer;

  @Inject
  public SmtpSettingsValidationPlexusResource(final SmtpSettingsValidator emailer) {
    this.emailer = emailer;
    this.setModifiable(true);
  }

  @Override
  public Object getPayloadInstance() {
    return new SmtpSettingsResourceRequest();
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/check_smtp_settings", "authcBasic,perms[nexus:settings]");
  }

  /**
   * Validate smtp settings, send a test email using the configuration.
   */
  @Override
  @PUT
  @ResourceMethodSignature(input = SmtpSettingsResourceRequest.class)
  public Object put(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    SmtpSettingsResourceRequest configRequest = (SmtpSettingsResourceRequest) payload;

    SmtpSettingsResource settings = configRequest.getData();

    String email = settings.getTestEmail();

    validateEmail(email);

    CSmtpConfiguration config = new CSmtpConfiguration();

    config.setHostname(settings.getHost());

    String oldPassword = getNexusEmailer().getSMTPPassword();

    config.setPassword(this.getActualPassword(settings.getPassword(), oldPassword));
    config.setPort(settings.getPort());
    config.setSslEnabled(settings.isSslEnabled());
    config.setTlsEnabled(settings.isTlsEnabled());
    config.setUsername(settings.getUsername());
    config.setSystemEmailAddress(settings.getSystemEmailAddress().trim());

    boolean status;
    try {
      status = emailer.sendSmtpConfigurationTest(config, email);
    }
    catch (EmailerException e) {
      throw new PlexusResourceException(
          Status.CLIENT_ERROR_BAD_REQUEST, e,
          getNexusErrorResponse("*", "Failed to send validation e-mail: " + parseReason(e))
      );
    }

    if (status) {
      response.setStatus(Status.SUCCESS_OK, "Email was sent. Check your inbox!");
    }
    else {
      response.setStatus(Status.SUCCESS_OK, "Unable to determine if e-mail was sent or not.  Check your inbox!");
    }

    return null;
  }

  private String parseReason(final EmailerException e) {
    // first let's go to the top in exception chain
    Throwable top = e;
    while (top.getCause() != null) {
      top = top.getCause();
    }
    if (top instanceof SSLPeerUnverifiedException) {
      return "Untrusted Remote";
    }
    if (top instanceof CertPathBuilderException) {
      return "Untrusted Remote (" + top.getMessage() + ")";
    }
    if (top instanceof UnknownHostException) {
      return "Unknown host '" + top.getMessage() + "'";
    }
    return top.getMessage();
  }

  static void validateEmail(final String email)
      throws ResourceException
  {
    if (StringUtils.isEmpty(email)) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "E-mail address cannot be empty");
    }
    if (!EMAIL_PATTERN.matcher(email).matches()) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid e-mail address: " + email);
    }
  }

  @Override
  public void configureXStream(final XStream xstream) {
    xstream.registerLocalConverter(SmtpSettingsResource.class, "username", new HtmlUnescapeStringConverter(true));
    xstream.registerLocalConverter(SmtpSettingsResource.class, "password", new HtmlUnescapeStringConverter(true));
  }
}
