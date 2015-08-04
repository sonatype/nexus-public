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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.micromailer.Address;
import org.sonatype.nexus.configuration.application.DefaultGlobalRemoteConnectionSettings;
import org.sonatype.nexus.configuration.application.DefaultGlobalRemoteProxySettings;
import org.sonatype.nexus.configuration.application.GlobalRemoteProxySettings;
import org.sonatype.nexus.configuration.model.CRemoteConnectionSettings;
import org.sonatype.nexus.configuration.model.CRemoteProxySettings;
import org.sonatype.nexus.configuration.model.CRestApiSettings;
import org.sonatype.nexus.configuration.model.CSmtpConfiguration;
import org.sonatype.nexus.configuration.source.ApplicationConfigurationSource;
import org.sonatype.nexus.notification.NotificationCheat;
import org.sonatype.nexus.notification.NotificationManager;
import org.sonatype.nexus.notification.NotificationTarget;
import org.sonatype.nexus.proxy.repository.UsernamePasswordRemoteAuthenticationSettings;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.GlobalConfigurationResourceResponse;
import org.sonatype.nexus.rest.model.HtmlUnescapeStringConverter;
import org.sonatype.nexus.rest.model.RemoteConnectionSettings;
import org.sonatype.nexus.rest.model.RemoteProxySettingsDTO;
import org.sonatype.nexus.rest.model.RestApiSettings;
import org.sonatype.nexus.rest.model.SmtpSettings;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.security.configuration.source.SecurityConfigurationSource;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang.StringUtils;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * The GlobalConfiguration resource. It simply gets and builds the requested config REST model (DTO) and passes
 * serializes it using underlying representation mechanism.
 *
 * @author cstamas
 * @author tstevens
 */
@Named
@Singleton
@Path(GlobalConfigurationPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
public class GlobalConfigurationPlexusResource
    extends AbstractGlobalConfigurationPlexusResource
{
  /**
   * The config key used in URI and request attributes
   */
  public static final String CONFIG_NAME_KEY = "configName";

  public static final String RESOURCE_URI = "/global_settings/{" + CONFIG_NAME_KEY + "}";

  /**
   * Name denoting current Nexus configuration
   */
  public static final String CURRENT_CONFIG_NAME = "current";

  /**
   * Name denoting default Nexus configuration
   */
  public static final String DEFAULT_CONFIG_NAME = "default";

  private final NotificationManager notificationManager;

  private final SecurityConfigurationSource defaultSecurityConfigurationSource;

  private final ApplicationConfigurationSource configurationSource;

  @Inject
  public GlobalConfigurationPlexusResource(final NotificationManager notificationManager,
                                           final @Named("static") SecurityConfigurationSource defaultSecurityConfigurationSource,
                                           final @Named("static") ApplicationConfigurationSource configurationSource)
  {
    this.notificationManager = notificationManager;
    this.defaultSecurityConfigurationSource = defaultSecurityConfigurationSource;
    this.configurationSource = configurationSource;

    this.setModifiable(true);
  }

  // ----------------------------------------------------------------------------
  // Default Configuration
  // ----------------------------------------------------------------------------

  public boolean isDefaultAnonymousAccessEnabled() {
    return this.defaultSecurityConfigurationSource.getConfiguration().isAnonymousAccessEnabled();
  }

  public String getDefaultAnonymousUsername() {
    return this.defaultSecurityConfigurationSource.getConfiguration().getAnonymousUsername();
  }

  public String getDefaultAnonymousPassword() {
    return this.defaultSecurityConfigurationSource.getConfiguration().getAnonymousPassword();
  }

  public List<String> getDefaultRealms() {
    return this.defaultSecurityConfigurationSource.getConfiguration().getRealms();
  }

  public CRemoteConnectionSettings readDefaultGlobalRemoteConnectionSettings() {
    return configurationSource.getConfiguration().getGlobalConnectionSettings();
  }

  public CRemoteProxySettings readDefaultRemoteProxySettings() {
    return configurationSource.getConfiguration().getRemoteProxySettings();
  }

  public CRestApiSettings readDefaultRestApiSettings() {
    return configurationSource.getConfiguration().getRestApi();
  }

  public CSmtpConfiguration readDefaultSmtpConfiguration() {
    return configurationSource.getConfiguration().getSmtpConfiguration();
  }

  // ==

  @Override
  public Object getPayloadInstance() {
    return new GlobalConfigurationResourceResponse();
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/global_settings/*", "authcBasic,perms[nexus:settings]");
  }

  /**
   * Get the specified global configuration (i.e. current or default)
   *
   * @param configName The name of the config (as returned by the global configuration list resource) to get.
   */
  @Override
  @GET
  @ResourceMethodSignature(pathParams = {@PathParam(GlobalConfigurationPlexusResource.CONFIG_NAME_KEY)},
      output = GlobalConfigurationResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    String configurationName = request.getAttributes().get(CONFIG_NAME_KEY).toString();

    if (!DEFAULT_CONFIG_NAME.equals(configurationName) && !CURRENT_CONFIG_NAME.equals(configurationName)) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
    }
    else {
      GlobalConfigurationResource resource = new GlobalConfigurationResource();

      if (DEFAULT_CONFIG_NAME.equals(configurationName)) {
        fillDefaultConfiguration(request, resource);
      }
      else {
        fillCurrentConfiguration(request, resource);
      }

      GlobalConfigurationResourceResponse result = new GlobalConfigurationResourceResponse();

      result.setData(resource);

      return result;
    }
  }

  /**
   * Update the global configuration.
   *
   * @param configName The name of the config (as returned by the global configuration list resource) to update.
   */
  @Override
  @PUT
  @ResourceMethodSignature(pathParams = {@PathParam(GlobalConfigurationPlexusResource.CONFIG_NAME_KEY)},
      input = GlobalConfigurationResourceResponse.class)
  public Object put(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    String configurationName = request.getAttributes().get(CONFIG_NAME_KEY).toString();

    if (!DEFAULT_CONFIG_NAME.equals(configurationName) && !CURRENT_CONFIG_NAME.equals(configurationName)) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
    }
    else if (!CURRENT_CONFIG_NAME.equals(configurationName)) {
      throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }
    else {
      GlobalConfigurationResourceResponse configRequest = (GlobalConfigurationResourceResponse) payload;

      if (configRequest != null) {
        GlobalConfigurationResource resource = configRequest.getData();

        try {
          if (resource.getSmtpSettings() != null) {
            SmtpSettings settings = resource.getSmtpSettings();

            getNexusEmailer().setSMTPHostname(settings.getHost());

            // lookup old password
            String oldPassword = getNexusEmailer().getSMTPPassword();

            if (settings.getPassword() == null) {
              settings.setPassword("");
            }
            getNexusEmailer().setSMTPPassword(this.getActualPassword(settings.getPassword(), oldPassword));

            getNexusEmailer().setSMTPPort(settings.getPort());

            getNexusEmailer().setSMTPSslEnabled(settings.isSslEnabled());

            getNexusEmailer().setSMTPTlsEnabled(settings.isTlsEnabled());

            if (settings.getUsername() == null) {
              settings.setUsername("");
            }
            getNexusEmailer().setSMTPUsername(settings.getUsername());

            getNexusEmailer().setSMTPSystemEmailAddress(
                new Address(settings.getSystemEmailAddress().trim()));
          }

          if (resource.getGlobalConnectionSettings() != null) {
            RemoteConnectionSettings s = resource.getGlobalConnectionSettings();

            getGlobalRemoteConnectionSettings().setConnectionTimeout(s.getConnectionTimeout() * 1000);

            getGlobalRemoteConnectionSettings().setRetrievalRetryCount(s.getRetrievalRetryCount());

            getGlobalRemoteConnectionSettings().setQueryString(s.getQueryString());

            getGlobalRemoteConnectionSettings().setUserAgentCustomizationString(s.getUserAgentString());
          }

          setGlobalProxySettings(resource.getRemoteProxySettings(), getGlobalRemoteProxySettings());

          getNexusConfiguration().setRealms(resource.getSecurityRealms());

          final String anonymousUsername = resource.getSecurityAnonymousUsername();
          final String anonymousPassword =
              getActualPassword(resource.getSecurityAnonymousPassword(),
                  getNexusConfiguration().getAnonymousPassword());

          if (resource.isSecurityAnonymousAccessEnabled() && !StringUtils.isEmpty(anonymousUsername)
              && !StringUtils.isEmpty(anonymousPassword)) {
            getNexusConfiguration().setAnonymousAccess(true, anonymousUsername, anonymousPassword);
          }
          else if (resource.isSecurityAnonymousAccessEnabled()) {
            // the supplied anon auth info is wrong/empty
            getLogger().warn(
                "Nexus refused to apply configuration, the supplied anonymous username/pwd information is empty.");

            throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, getNexusErrorResponse(
                "securityAnonymousUsername", "Cannot be empty when Anonynous access is enabled"));
          }
          else {
            getNexusConfiguration().setAnonymousAccess(false, null, null);
          }

          if (resource.getGlobalRestApiSettings() != null) {
            RestApiSettings restApiSettings = resource.getGlobalRestApiSettings();

            getGlobalRestApiSettings().setForceBaseUrl(restApiSettings.isForceBaseUrl());

            if (StringUtils.isEmpty(resource.getGlobalRestApiSettings().getBaseUrl())) {
              getGlobalRestApiSettings().setBaseUrl(null);
            }
            else {
              getGlobalRestApiSettings().setBaseUrl(
                  new Reference(restApiSettings.getBaseUrl()).getTargetRef().toString());
            }

            getGlobalRestApiSettings().setUITimeout(restApiSettings.getUiTimeout() * 1000);
          }
          else {
            getGlobalRestApiSettings().disable();
          }

          if (resource.getSystemNotificationSettings() != null) {
            notificationManager.setEnabled(resource.getSystemNotificationSettings().isEnabled());

            NotificationTarget target =
                notificationManager.readNotificationTarget(NotificationCheat.AUTO_BLOCK_NOTIFICATION_GROUP_ID);

            if (target == null) {
              target = new NotificationTarget();
              target.setTargetId(NotificationCheat.AUTO_BLOCK_NOTIFICATION_GROUP_ID);
            }

            target.getTargetRoles().clear();
            target.getTargetRoles().addAll(resource.getSystemNotificationSettings().getRoles());

            target.getExternalTargets().clear();

            if (StringUtils.isNotEmpty(resource.getSystemNotificationSettings().getEmailAddresses())) {
              target.getExternalTargets().addAll(
                  Arrays.asList(resource.getSystemNotificationSettings().getEmailAddresses().split(",")));
            }

            target.getTargetUsers().clear();

            notificationManager.updateNotificationTarget(target);
          }

          // NEXUS-3064: to "inform" global remote storage context (and hence, all affected proxy
          // repositories) about the change, but only if config is saved okay
          // TODO: this is wrong, the config framework should "tell" this changed, but we have some
          // design flaw here: the globalRemoteStorageContext is NOT a component, while the settings are
          boolean remoteConnectionSettingsIsDirty = ((DefaultGlobalRemoteConnectionSettings)getGlobalRemoteConnectionSettings()).isDirty();

          boolean remoteProxySettingsIsDirty = ((DefaultGlobalRemoteProxySettings)getGlobalRemoteProxySettings()).isDirty();

          getNexusConfiguration().saveConfiguration();

          // NEXUS-3064: to "inform" global remote storage context (and hence, all affected proxy
          // repositories) about the change, but only if config is saved okay
          // TODO: this is wrong, the config framework should "tell" this changed, but we have some
          // design flaw here: the globalRemoteStorageContext is NOT a component, while the settings are
          if (remoteConnectionSettingsIsDirty) {
            getNexusConfiguration().getGlobalRemoteStorageContext().setRemoteConnectionSettings(
                getGlobalRemoteConnectionSettings());
          }

          if (remoteProxySettingsIsDirty) {
            getNexusConfiguration().getGlobalRemoteStorageContext().setRemoteProxySettings(
                getGlobalRemoteProxySettings()
            );
          }
        }
        catch (IOException e) {
          getLogger().warn("Got IO Exception during update of Nexus configuration.", e);

          throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
        }
        catch (InvalidConfigurationException e) {
          // TODO: this should be removed from the Global config, as it is NO longer part of the nexus.xml
          getLogger().debug("Configuraiton Exception while setting security values", e);
          this.handleInvalidConfigurationException(e);
        }
        catch (ConfigurationException e) {
          getLogger().warn("Nexus refused to apply configuration.", e);

          throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(),
              getNexusErrorResponse("*", e.getMessage()));
        }
      }
    }
    // TODO: this method needs some serious cleaning up...
    response.setStatus(Status.SUCCESS_NO_CONTENT);
    return null;
  }

  private void setGlobalProxySettings(final RemoteProxySettingsDTO remoteProxySettings,
                                      final GlobalRemoteProxySettings remoteProxySettingsConfiguration)
      throws ConfigurationException
  {
    if (remoteProxySettings != null
        && remoteProxySettings.getHttpProxySettings() != null
        && !StringUtils.isEmpty(remoteProxySettings.getHttpProxySettings().getProxyHostname())) {
      String oldHttpProxyPassword = null;
      if (remoteProxySettingsConfiguration.getHttpProxySettings() != null
          && remoteProxySettingsConfiguration.getHttpProxySettings().getProxyAuthentication() != null) {
        oldHttpProxyPassword =
            ((UsernamePasswordRemoteAuthenticationSettings) remoteProxySettingsConfiguration.getHttpProxySettings()
                .getProxyAuthentication()).getPassword();
      }

      String oldHttpsProxyPassword = null;
      if (remoteProxySettingsConfiguration.getHttpsProxySettings() != null
          && remoteProxySettingsConfiguration.getHttpsProxySettings().getProxyAuthentication() != null) {
        oldHttpsProxyPassword =
            ((UsernamePasswordRemoteAuthenticationSettings) remoteProxySettingsConfiguration.getHttpsProxySettings()
                .getProxyAuthentication()).getPassword();
      }

      remoteProxySettingsConfiguration.setHttpProxySettings(
          convert(remoteProxySettings.getHttpProxySettings(), oldHttpProxyPassword)
      );

      remoteProxySettingsConfiguration.setHttpsProxySettings(
          convert(remoteProxySettings.getHttpsProxySettings(), oldHttpsProxyPassword)
      );

      List<String> nonProxyHosts = remoteProxySettings.getNonProxyHosts();
      if (nonProxyHosts != null && !nonProxyHosts.isEmpty()) {
        // removing nulls and empty strings
        HashSet<String> cleanNonProxyHosts = new HashSet<String>();
        for (String host : nonProxyHosts) {
          if (StringUtils.isNotEmpty(host)) {
            cleanNonProxyHosts.add(host);
          }
        }
        remoteProxySettingsConfiguration.setNonProxyHosts(cleanNonProxyHosts);
      }
      else {
        // clear it out
        remoteProxySettingsConfiguration.setNonProxyHosts(null);
      }
    }
    else {
      remoteProxySettingsConfiguration.setHttpProxySettings(null);
      remoteProxySettingsConfiguration.setHttpsProxySettings(null);
      remoteProxySettingsConfiguration.setNonProxyHosts(null);
    }
  }

  /**
   * Externalized Nexus object to DTO's conversion, using default Nexus configuration.
   */
  protected void fillDefaultConfiguration(Request request, GlobalConfigurationResource resource) {

    resource.setSecurityAnonymousAccessEnabled(isDefaultAnonymousAccessEnabled());

    resource.setSecurityRealms(getDefaultRealms());

    resource.setSecurityAnonymousUsername(getDefaultAnonymousUsername());

    resource.setSecurityAnonymousPassword(PASSWORD_PLACE_HOLDER);

    resource.setGlobalConnectionSettings(convert(readDefaultGlobalRemoteConnectionSettings()));

    resource.setRemoteProxySettings(convert(readDefaultRemoteProxySettings()));

    RestApiSettings restApiSettings = convert(readDefaultRestApiSettings());
    if (restApiSettings != null) {
      restApiSettings.setBaseUrl(getContextRoot(request).getTargetRef().toString());
    }
    resource.setGlobalRestApiSettings(restApiSettings);

    resource.setSmtpSettings(convert(readDefaultSmtpConfiguration()));
  }

  /**
   * Externalized Nexus object to DTO's conversion, using current Nexus configuration.
   */
  protected void fillCurrentConfiguration(Request request, GlobalConfigurationResource resource) {

    resource.setSecurityAnonymousAccessEnabled(getNexusConfiguration().isAnonymousAccessEnabled());

    resource.setSecurityRealms(getNexusConfiguration().getRealms());

    resource.setSecurityAnonymousUsername(getNexusConfiguration().getAnonymousUsername());

    resource.setSecurityAnonymousPassword(PASSWORD_PLACE_HOLDER);

    resource.setGlobalConnectionSettings(convert(getGlobalRemoteConnectionSettings()));

    resource.setRemoteProxySettings(convert(getGlobalRemoteProxySettings()));

    RestApiSettings restApiSettings = convert(getGlobalRestApiSettings());
    if (restApiSettings != null && StringUtils.isEmpty(restApiSettings.getBaseUrl())) {
      restApiSettings.setBaseUrl(getContextRoot(request).getTargetRef().toString());
    }
    resource.setGlobalRestApiSettings(restApiSettings);

    resource.setSmtpSettings(convert(getNexusEmailer()));

    resource.setSystemNotificationSettings(convert(notificationManager));
  }

  protected String getSecurityConfiguration(boolean enabled, String authSourceType) {
    if (!enabled) {
      return SECURITY_OFF;
    }
    else {
      if (SECURITY_SIMPLE.equals(authSourceType)) {
        return SECURITY_SIMPLE;
      }
      else {
        return SECURITY_CUSTOM;
      }
    }
  }

  @Override
  public void configureXStream(final XStream xstream) {
    xstream.registerLocalConverter(SmtpSettings.class, "username", new HtmlUnescapeStringConverter(true));
    xstream.registerLocalConverter(SmtpSettings.class, "password", new HtmlUnescapeStringConverter(true));
  }
}
