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
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.sonatype.nexus.api.rest.selfhosted.email.model.ApiEmailConfiguration;
import org.sonatype.nexus.api.rest.selfhosted.email.model.ApiEmailValidation;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.email.EmailManager;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.validation.Validate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.mail.EmailException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Produces(APPLICATION_JSON)
public class EmailConfigurationApiResource
    implements Resource, EmailConfigurationApiResourceDoc
{
  private static final Logger log = LoggerFactory.getLogger(EmailConfigurationApiResource.class);

  private final EmailManager emailManager;

  @Autowired
  public EmailConfigurationApiResource(EmailManager emailManager) {
    this.emailManager = emailManager;
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
      String exceptionMessage = rootCause.getMessage().replace(rootCause.getClass().getName() + ": ", "");

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
