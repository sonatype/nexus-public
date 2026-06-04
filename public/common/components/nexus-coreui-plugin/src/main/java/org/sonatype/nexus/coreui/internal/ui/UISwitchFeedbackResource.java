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
package org.sonatype.nexus.coreui.internal.ui;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

import javax.validation.ValidationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.common.node.DeploymentAccess;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.kv.KeyValueStore;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.validation.ssrf.AntiSsrfService;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.protocol.HttpContext;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SETTINGS_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_NAMED_VALUE;

/**
 * Proxy endpoint that forwards Classic UI switch feedback to the Sonatype feedback endpoint.
 * Returns 204 in all cases so that navigation is never blocked.
 */
@Component
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ConditionalOnProperty(name = PREVIEW_UI_SETTINGS_ENABLED, havingValue = "true")
@Path("internal/ui/switch-feedback")
public class UISwitchFeedbackResource
    implements Resource
{

  private static final Logger log = LoggerFactory.getLogger(UISwitchFeedbackResource.class);

  static final int MAX_FEEDBACK_LENGTH = 500;

  static final int REQUEST_TIMEOUT_MS = 2_000;

  static final int MAX_REDIRECTS = 1;

  static final String DEFAULT_FEEDBACK_ENDPOINT_URL = "https://links.sonatype.com/products/nxrm3/newui";

  private final ObjectMapper objectMapper;

  private final String feedbackEndpointUrl;

  private final KeyValueStore keyValueStore;

  private final DeploymentAccess deploymentAccess;

  private final AntiSsrfService antiSsrfService;

  private final HttpClientManager httpClientManager;

  private final boolean defaultSwitchFeedbackDisabled;

  private final RedirectStrategy feedbackRedirectStrategy;

  private volatile CloseableHttpClient httpClient;

  @Autowired
  public UISwitchFeedbackResource(
      final HttpClientManager httpClientManager,
      final ObjectMapper objectMapper,
      final KeyValueStore keyValueStore,
      final DeploymentAccess deploymentAccess,
      final AntiSsrfService antiSsrfService,
      @Value(PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_NAMED_VALUE) final boolean defaultSwitchFeedbackDisabled,
      @Value("${nexus.preview.ui.switch.feedback.url:" + DEFAULT_FEEDBACK_ENDPOINT_URL
          + "}") final String feedbackEndpointUrl)
  {
    this.objectMapper = checkNotNull(objectMapper);
    this.keyValueStore = checkNotNull(keyValueStore);
    this.deploymentAccess = checkNotNull(deploymentAccess);
    this.antiSsrfService = checkNotNull(antiSsrfService);
    this.httpClientManager = checkNotNull(httpClientManager);
    this.defaultSwitchFeedbackDisabled = defaultSwitchFeedbackDisabled;
    this.feedbackEndpointUrl = feedbackEndpointUrl;
    this.feedbackRedirectStrategy = new PostPreservingRedirectStrategy(antiSsrfService);
  }

  @PreDestroy
  public void shutdown() {
    CloseableHttpClient client = httpClient;
    if (client == null) {
      return;
    }

    try {
      client.close();
    }
    catch (IOException e) {
      log.debug("Error closing feedback HTTP client", e);
    }
  }

  @POST
  @RequiresAuthentication
  public Response submitFeedback(final FeedbackRequest request) {
    if (request == null) {
      return Response.noContent().build();
    }

    if (isSwitchFeedbackDisabled()) {
      return Response.noContent().build();
    }

    if (isNullOrEmpty(feedbackEndpointUrl)) {
      log.warn("Switch feedback endpoint URL not configured; skipping proxy");
      return Response.noContent().build();
    }

    String raw = request.getFeedback();
    if (isNullOrEmpty(raw) || raw.trim().isEmpty()) {
      return Response.noContent().build();
    }
    String feedback = raw.trim();
    if (feedback.length() > MAX_FEEDBACK_LENGTH) {
      feedback = feedback.substring(0, MAX_FEEDBACK_LENGTH);
    }

    String text = String.format("DeploymentId: %s | Edition: %s | Version: %s | Feedback: %s | Submitted: %s",
        getDeploymentId(),
        nullToUnknown(request.getEdition()),
        nullToUnknown(request.getVersion()),
        feedback,
        Instant.now().toString());

    try {
      URI feedbackUri = URI.create(feedbackEndpointUrl);
      antiSsrfService.validateHost(feedbackUri.getHost());

      String json = objectMapper.writeValueAsString(Map.of("text", text));

      HttpPost post = new HttpPost(feedbackUri);
      post.setConfig(RequestConfig.custom()
          .setConnectTimeout(REQUEST_TIMEOUT_MS)
          .setSocketTimeout(REQUEST_TIMEOUT_MS)
          .setMaxRedirects(MAX_REDIRECTS)
          .build());
      post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

      try (CloseableHttpResponse response = getHttpClient().execute(post)) {
        int code = response.getStatusLine().getStatusCode();
        if (code >= 300) {
          log.warn("Feedback endpoint returned non-2xx status: {}", code);
        }
      }
    }
    catch (Exception e) {
      log.warn("Failed to send feedback to feedback endpoint", e);
    }

    return Response.noContent().build();
  }

  RedirectStrategy getFeedbackRedirectStrategy() {
    return feedbackRedirectStrategy;
  }

  private static String nullToUnknown(String value) {
    return isNullOrEmpty(value) ? "Unknown" : value;
  }

  private String getDeploymentId() {
    try {
      return nullToUnknown(deploymentAccess.getId());
    }
    catch (Exception e) {
      log.debug("Deployment id is unavailable for feedback payload", e);
      return "Unknown";
    }
  }

  private boolean isSwitchFeedbackDisabled() {
    return keyValueStore.getBoolean(PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY).orElse(defaultSwitchFeedbackDisabled);
  }

  private CloseableHttpClient getHttpClient() {
    CloseableHttpClient client = httpClient;
    if (client == null) {
      synchronized (this) {
        client = httpClient;
        if (client == null) {
          client = httpClientManager.create(
              plan -> plan.getClient().setRedirectStrategy(feedbackRedirectStrategy));
          httpClient = client;
        }
      }
    }
    return client;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class FeedbackRequest
  {
    @JsonProperty
    private String edition;

    @JsonProperty
    private String version;

    @JsonProperty
    private String feedback;

    public String getEdition() {
      return edition;
    }

    public void setEdition(final String edition) {
      this.edition = edition;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(final String version) {
      this.version = version;
    }

    public String getFeedback() {
      return feedback;
    }

    public void setFeedback(final String feedback) {
      this.feedback = feedback;
    }
  }

  /**
   * Preserves POST method and body across 301/302/307/308 redirects. Required because
   * the default feedback endpoint URL is a Sonatype link-redirector that 302s to the actual
   * collector, which expects a POST body. Standard {@link LaxRedirectStrategy} rewrites
   * 301/302 POSTs to GETs, which would drop our payload.
   *
   * Validates the redirect target against {@link AntiSsrfService} and rejects any non-HTTPS
   * scheme, preventing the upstream redirector from pivoting the outbound request at
   * private or otherwise-restricted hosts.
   */
  static final class PostPreservingRedirectStrategy
      extends LaxRedirectStrategy
  {
    private final AntiSsrfService antiSsrfService;

    PostPreservingRedirectStrategy(final AntiSsrfService antiSsrfService) {
      this.antiSsrfService = antiSsrfService;
    }

    @Override
    public HttpUriRequest getRedirect(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context) throws ProtocolException
    {
      URI location = getLocationURI(request, response, context);
      validateLocation(location);

      String method = request.getRequestLine().getMethod();
      int statusCode = response.getStatusLine().getStatusCode();

      if ("POST".equalsIgnoreCase(method)
          && (statusCode == 301 || statusCode == 302 || statusCode == 307 || statusCode == 308)) {
        return RequestBuilder.copy(request).setUri(location).build();
      }

      return super.getRedirect(request, response, context);
    }

    private void validateLocation(final URI location) throws ProtocolException {
      String scheme = location.getScheme();
      if (!"https".equalsIgnoreCase(scheme)) {
        throw new ProtocolException("Refusing to follow non-HTTPS redirect: " + location);
      }

      String host = location.getHost();
      if (host == null) {
        throw new ProtocolException("Redirect location has no host: " + location);
      }

      try {
        antiSsrfService.validateHost(host);
      }
      catch (ValidationException e) {
        throw new ProtocolException(
            "Refusing to follow redirect to disallowed host " + host + ": " + e.getMessage());
      }
    }
  }
}
