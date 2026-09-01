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

import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.Response;

import org.sonatype.nexus.common.node.DeploymentAccess;
import org.sonatype.nexus.coreui.internal.ui.UISwitchFeedbackResource.FeedbackRequest;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.HttpClientPlan;
import org.sonatype.nexus.httpclient.HttpClientPlan.Customizer;
import org.sonatype.nexus.kv.KeyValueStore;
import org.sonatype.nexus.validation.ssrf.AntiSsrfService;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UISwitchFeedbackResourceTest
{
  @Mock
  HttpClientManager httpClientManager;

  @Mock
  CloseableHttpClient httpClient;

  @Mock
  CloseableHttpResponse httpResponse;

  @Mock
  StatusLine statusLine;

  @Mock
  DeploymentAccess deploymentAccess;

  @Mock
  KeyValueStore keyValueStore;

  @Mock
  AntiSsrfService antiSsrfService;

  @Mock
  SecurityManager securityManager;

  @Mock
  Subject subject;

  ObjectMapper objectMapper = new ObjectMapper();

  UISwitchFeedbackResource underTest;

  @BeforeEach
  void setUp() throws Exception {
    lenient().when(httpClientManager.create(any(Customizer.class))).thenReturn(httpClient);
    lenient().when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
    lenient().when(httpResponse.getStatusLine()).thenReturn(statusLine);
    lenient().when(statusLine.getStatusCode()).thenReturn(200);
    lenient().when(deploymentAccess.getId()).thenReturn("deployment-123");
    lenient().when(keyValueStore.getBoolean(PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY))
        .thenReturn(java.util.Optional.empty());
    lenient().when(subject.isAuthenticated()).thenReturn(true);
    ThreadContext.bind(securityManager);
    ThreadContext.bind(subject);

    underTest = newResource(false);
  }

  @AfterEach
  void tearDown() {
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
  }

  @Test
  void shouldConfigureConditionalOnPropertyToRegisterBeanByDefault() {
    ConditionalOnProperty annotation =
        UISwitchFeedbackResource.class.getAnnotation(ConditionalOnProperty.class);

    assertThat(annotation, notNullValue());
    assertThat(annotation.name(), arrayContaining("nexus.previewui.enabled"));
    assertThat(annotation.havingValue(), is("true"));
    assertThat(annotation.matchIfMissing(), is(true));
  }

  @Test
  void returnsNoContentWhenRequestIsNull() throws Exception {
    Response response = underTest.submitFeedback(null);

    assertThat(response.getStatus(), is(204));
    verify(httpClient, never()).execute(any(HttpPost.class));
  }

  @Test
  void submitsSuccessfully() throws Exception {
    Response response = underTest.submitFeedback(request("PRO", "3.78.0", "hello"));

    assertThat(response.getStatus(), is(204));
    verify(httpClient).execute(any(HttpPost.class));
  }

  @Test
  void returnsNoContentWhenSsrfBlocksFeedbackUrl() throws Exception {
    doThrow(new ValidationException("blocked"))
        .when(antiSsrfService)
        .validateHost("evil.internal");

    UISwitchFeedbackResource resource = new UISwitchFeedbackResource(
        httpClientManager, objectMapper, keyValueStore, deploymentAccess, antiSsrfService, false,
        "https://evil.internal/collect");

    Response response = resource.submitFeedback(request("PRO", "3.78.0", "hi"));

    assertThat(response.getStatus(), is(204));
    verify(httpClient, never()).execute(any(HttpPost.class));
  }

  @Test
  void returnsNoContentWhenFeedbackEndpointReturns4xx() throws Exception {
    when(statusLine.getStatusCode()).thenReturn(400);

    Response response = underTest.submitFeedback(request("PRO", "3.78.0", "hello"));

    assertThat(response.getStatus(), is(204));
  }

  @Test
  void returnsNoContentWhenFeedbackEndpointUrlIsBlank() throws Exception {
    UISwitchFeedbackResource resource = newResource("");

    Response response = resource.submitFeedback(request("PRO", "3.78.0", "hello"));

    assertThat(response.getStatus(), is(204));
    verify(httpClient, never()).execute(any(HttpPost.class));
  }

  @Test
  void returnsNoContentWhenSwitchFeedbackIsDisabled() throws Exception {
    when(keyValueStore.getBoolean(PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY)).thenReturn(java.util.Optional.of(true));

    Response response = underTest.submitFeedback(request("PRO", "3.78.0", "hello"));

    assertThat(response.getStatus(), is(204));
    verify(httpClient, never()).execute(any(HttpPost.class));
  }

  @Test
  void returnsNoContentWhenHttpClientThrows() throws Exception {
    when(httpClient.execute(any(HttpPost.class))).thenThrow(new java.io.IOException("timeout"));

    Response response = underTest.submitFeedback(request("PRO", "3.78.0", "hello"));

    assertThat(response.getStatus(), is(204));
  }

  @Test
  void returnsNoContentWhenFeedbackIsEmpty() throws Exception {
    Response response = underTest.submitFeedback(request("PRO", "3.78.0", ""));

    assertThat(response.getStatus(), is(204));
    verify(httpClient, never()).execute(any(HttpPost.class));
  }

  @Test
  void returnsNoContentWhenFeedbackIsOnlyWhitespace() throws Exception {
    Response response = underTest.submitFeedback(request("PRO", "3.78.0", "   "));

    assertThat(response.getStatus(), is(204));
    verify(httpClient, never()).execute(any(HttpPost.class));
  }

  @Test
  void truncatesFeedbackAt500Chars() throws Exception {
    String longFeedback = "x".repeat(600);
    ArgumentCaptor<HttpPost> postCaptor = ArgumentCaptor.forClass(HttpPost.class);

    underTest.submitFeedback(request("PRO", "3.78.0", longFeedback));

    verify(httpClient).execute(postCaptor.capture());
    String body = new String(postCaptor.getValue().getEntity().getContent().readAllBytes());
    assertThat(body, not(containsString("x".repeat(501))));
  }

  @Test
  void includesDeploymentIdEditionVersionInMessage() throws Exception {
    ArgumentCaptor<HttpPost> postCaptor = ArgumentCaptor.forClass(HttpPost.class);

    underTest.submitFeedback(request("PRO", "3.78.0", "great UI"));

    verify(httpClient).execute(postCaptor.capture());
    String body = new String(postCaptor.getValue().getEntity().getContent().readAllBytes());
    assertThat(body, containsString("deployment-123"));
    assertThat(body, containsString("PRO"));
    assertThat(body, containsString("3.78.0"));
    assertThat(body, containsString("great UI"));
  }

  @Test
  void escapesJsonSpecialCharsInFeedback() throws Exception {
    ArgumentCaptor<HttpPost> postCaptor = ArgumentCaptor.forClass(HttpPost.class);

    underTest.submitFeedback(request("PRO", "3.78.0", "say \"hello\" & <world>"));

    verify(httpClient).execute(postCaptor.capture());
    String body = new String(postCaptor.getValue().getEntity().getContent().readAllBytes());
    // ObjectMapper produces valid JSON — the body should parse without error
    objectMapper.readTree(body);
  }

  @Test
  void setsTimeoutAndMaxRedirectsOnOutboundRequest() throws Exception {
    ArgumentCaptor<HttpPost> postCaptor = ArgumentCaptor.forClass(HttpPost.class);

    underTest.submitFeedback(request("PRO", "3.78.0", "hi"));

    verify(httpClient).execute(postCaptor.capture());
    RequestConfig config = postCaptor.getValue().getConfig();
    assertThat(config, notNullValue());
    assertThat(config.getConnectTimeout(), is(UISwitchFeedbackResource.REQUEST_TIMEOUT_MS));
    assertThat(config.getSocketTimeout(), is(UISwitchFeedbackResource.REQUEST_TIMEOUT_MS));
    assertThat(config.getMaxRedirects(), is(UISwitchFeedbackResource.MAX_REDIRECTS));
  }

  @Test
  void configuresRedirectStrategyWhenClientIsCreated() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Customizer> customizerCaptor = ArgumentCaptor.forClass(Customizer.class);
    HttpClientBuilder builder = mock(HttpClientBuilder.class);
    HttpClientPlan plan = mock(HttpClientPlan.class);
    doReturn(builder).when(plan).getClient();

    underTest.submitFeedback(request("PRO", "3.78.0", "hi"));

    verify(httpClientManager).create(customizerCaptor.capture());
    customizerCaptor.getValue().customize(plan);
    verify(builder).setRedirectStrategy(underTest.getFeedbackRedirectStrategy());
  }

  @Test
  void preservesPostMethodAcross302Redirect() throws Exception {
    HttpUriRequest redirected = invokeRedirectStrategy(302, "https://example.com/forwarded");

    assertThat(redirected.getMethod(), is("POST"));
    assertThat(redirected.getURI().toString(), is("https://example.com/forwarded"));
    assertThat(redirected, instanceOf(HttpEntityEnclosingRequest.class));
    String redirectedBody =
        new String(((HttpEntityEnclosingRequest) redirected).getEntity().getContent().readAllBytes());
    assertThat(redirectedBody, containsString("redirect body"));
  }

  @Test
  void preservesPostMethodAcross301Redirect() throws Exception {
    HttpUriRequest redirected = invokeRedirectStrategy(301, "https://example.com/forwarded");

    assertThat(redirected.getMethod(), is("POST"));
    assertThat(redirected.getURI().toString(), is("https://example.com/forwarded"));
    assertThat(redirected, instanceOf(HttpEntityEnclosingRequest.class));
    String redirectedBody =
        new String(((HttpEntityEnclosingRequest) redirected).getEntity().getContent().readAllBytes());
    assertThat(redirectedBody, containsString("redirect body"));
  }

  @Test
  void preservesPostMethodAcross307Redirect() throws Exception {
    HttpUriRequest redirected = invokeRedirectStrategy(307, "https://example.com/forwarded");

    assertThat(redirected.getMethod(), is("POST"));
    assertThat(redirected.getURI().toString(), is("https://example.com/forwarded"));
  }

  @Test
  void rejectsNonHttpsRedirect() {
    ProtocolException thrown = assertThrows(ProtocolException.class,
        () -> invokeRedirectStrategy(302, "http://example.com/forwarded"));

    assertThat(thrown.getMessage(), containsString("non-HTTPS"));
  }

  @Test
  void rejectsRedirectToSsrfBlockedHost() {
    doThrow(new ValidationException("restricted address"))
        .when(antiSsrfService)
        .validateHost("169.254.169.254");

    ProtocolException thrown = assertThrows(ProtocolException.class,
        () -> invokeRedirectStrategy(302, "https://169.254.169.254/meta"));

    assertThat(thrown.getMessage(), containsString("disallowed host"));
    assertThat(thrown.getMessage(), containsString("169.254.169.254"));
  }

  private UISwitchFeedbackResource newResource(final boolean defaultDisabled) {
    return new UISwitchFeedbackResource(
        httpClientManager, objectMapper, keyValueStore, deploymentAccess, antiSsrfService, defaultDisabled,
        UISwitchFeedbackResource.DEFAULT_FEEDBACK_ENDPOINT_URL);
  }

  private UISwitchFeedbackResource newResource(final String feedbackEndpointUrl) {
    return new UISwitchFeedbackResource(
        httpClientManager, objectMapper, keyValueStore, deploymentAccess, antiSsrfService, false,
        feedbackEndpointUrl);
  }

  private HttpUriRequest invokeRedirectStrategy(final int statusCode, final String locationValue) throws Exception {
    RedirectStrategy strategy = underTest.getFeedbackRedirectStrategy();
    HttpResponse response = mock(HttpResponse.class);
    StatusLine redirectStatus = mock(StatusLine.class);
    Header location = mock(Header.class);
    HttpContext context = new BasicHttpContext();

    HttpPost originalRequest = new HttpPost("https://links.sonatype.com/products/nxrm3/newui");
    originalRequest.setEntity(new StringEntity("{\"text\":\"redirect body\"}", ContentType.APPLICATION_JSON));

    lenient().when(response.getStatusLine()).thenReturn(redirectStatus);
    lenient().when(redirectStatus.getStatusCode()).thenReturn(statusCode);
    when(response.getFirstHeader("location")).thenReturn(location);
    when(location.getValue()).thenReturn(locationValue);
    context.setAttribute(HttpClientContext.REQUEST_CONFIG, RequestConfig.DEFAULT);
    context.setAttribute(HttpClientContext.HTTP_TARGET_HOST,
        new HttpHost("links.sonatype.com", 443, "https"));

    return strategy.getRedirect(originalRequest, response, context);
  }

  private static FeedbackRequest request(String edition, String version, String feedback) {
    FeedbackRequest req = new FeedbackRequest();
    req.setEdition(edition);
    req.setVersion(version);
    req.setFeedback(feedback);
    return req;
  }
}
