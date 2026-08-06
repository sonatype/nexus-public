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
package org.sonatype.nexus.api.rest.selfhosted.email;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.sonatype.nexus.api.rest.selfhosted.email.model.ApiEmailConfiguration;
import org.sonatype.nexus.api.rest.selfhosted.email.model.ApiEmailValidation;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.email.EmailManager;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.validation.Validate;
import org.sonatype.nexus.validation.ssrf.AntiSsrfService;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.mail2.core.EmailException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

@Produces(APPLICATION_JSON)
public class EmailConfigurationApiResource
    implements Resource, EmailConfigurationApiResourceDoc
{
  private static final Logger log = LoggerFactory.getLogger(EmailConfigurationApiResource.class);

  private final EmailManager emailManager;

  private final AntiSsrfService antiSsrfService;

  @Autowired
  public EmailConfigurationApiResource(EmailManager emailManager, AntiSsrfService antiSsrfService) {
    this.emailManager = emailManager;
    this.antiSsrfService = antiSsrfService;
  }

  @GET
  @RequiresPermissions("nexus:settings:read")
  public ApiEmailConfiguration getEmailConfiguration() {
    return convert(emailManager.getConfiguration());
  }

  @PUT
  @RequiresAuthentication
  @Validate
  @RequiresPermissions("nexus:settings:update")
  public void setEmailConfiguration(@NotNull @Valid final ApiEmailConfiguration apiEmailConfiguration) {
    antiSsrfService.validateHostWithoutCache(apiEmailConfiguration.getHost());
    emailManager.setConfiguration(convert(apiEmailConfiguration), apiEmailConfiguration.getPassword());
  }

  @POST
  @Path("/verify")
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:update")
  public Response testEmailConfiguration(@NotNull String verificationAddress) {
    EmailConfiguration emailConfiguration = emailManager.getConfiguration();

    if (emailConfiguration == null) {
      return Response.status(BAD_REQUEST)
          .entity(new ApiEmailValidation(false, "Email Settings are not yet configured"))
          .type(APPLICATION_JSON)
          .build();
    }

    try {
      emailManager.sendVerification(emailConfiguration, verificationAddress);
      return Response.ok(new ApiEmailValidation(true), APPLICATION_JSON)
          .build();
    }
    catch (EmailException e) {
      log.warn("Unable to send verification", e);
      Throwable rootCause = ExceptionUtils.getRootCause(e);
      if (rootCause == null) {
        rootCause = e;
      }
      String rawMessage = rootCause.getMessage();
      String exceptionMessage = rawMessage != null
          ? rawMessage.replace(rootCause.getClass().getName() + ": ", "")
          : rootCause.getClass().getSimpleName();

      return Response.status(BAD_REQUEST)
          .entity(new ApiEmailValidation(false, exceptionMessage))
          .type(APPLICATION_JSON)
          .build();
    }
  }

  @DELETE
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:update")
  public void deleteEmailConfiguration() {
    emailManager.setConfiguration(emailManager.newConfiguration(), Strings2.EMPTY);
  }

  private EmailConfiguration convert(ApiEmailConfiguration apiEmailConfiguration) {

    EmailConfiguration emailConfiguration = emailManager.newConfiguration();
    emailConfiguration.setEnabled(apiEmailConfiguration.isEnabled());
    emailConfiguration.setHost(apiEmailConfiguration.getHost());
    emailConfiguration.setPort(apiEmailConfiguration.getPort() == null ? 0 : apiEmailConfiguration.getPort());
    emailConfiguration.setNexusTrustStoreEnabled(apiEmailConfiguration.isNexusTrustStoreEnabled());

    if (StringUtils.isNotEmpty(apiEmailConfiguration.getUsername())) {
      emailConfiguration.setUsername(apiEmailConfiguration.getUsername());
    }
    else {
      emailConfiguration.setUsername("");
    }

    emailConfiguration.setFromAddress(apiEmailConfiguration.getFromAddress());
    emailConfiguration.setSubjectPrefix(apiEmailConfiguration.getSubjectPrefix());
    emailConfiguration.setStartTlsEnabled(apiEmailConfiguration.isStartTlsEnabled());
    emailConfiguration.setStartTlsRequired(apiEmailConfiguration.isStartTlsRequired());
    emailConfiguration.setSslOnConnectEnabled(apiEmailConfiguration.isSslOnConnectEnabled());
    emailConfiguration.setSslCheckServerIdentityEnabled(apiEmailConfiguration.isSslServerIdentityCheckEnabled());
    return emailConfiguration;
  }

  private ApiEmailConfiguration convert(EmailConfiguration emailConfiguration) {
    if (emailConfiguration == null) {
      return new ApiEmailConfiguration();
    }

    ApiEmailConfiguration apiEmailConfiguration = new ApiEmailConfiguration();
    apiEmailConfiguration.setEnabled(emailConfiguration.isEnabled());
    apiEmailConfiguration.setHost(emailConfiguration.getHost());
    apiEmailConfiguration.setPort(emailConfiguration.getPort());
    apiEmailConfiguration.setNexusTrustStoreEnabled(emailConfiguration.isNexusTrustStoreEnabled());
    apiEmailConfiguration.setUsername(emailConfiguration.getUsername());
    apiEmailConfiguration.setPassword(null);
    apiEmailConfiguration.setFromAddress(emailConfiguration.getFromAddress());
    apiEmailConfiguration.setSubjectPrefix(emailConfiguration.getSubjectPrefix());
    apiEmailConfiguration.setStartTlsEnabled(emailConfiguration.isStartTlsEnabled());
    apiEmailConfiguration.setStartTlsRequired(emailConfiguration.isStartTlsRequired());
    apiEmailConfiguration.setSslOnConnectEnabled(emailConfiguration.isSslOnConnectEnabled());
    apiEmailConfiguration.setSslServerIdentityCheckEnabled(emailConfiguration.isSslCheckServerIdentityEnabled());
    return apiEmailConfiguration;
  }
}
