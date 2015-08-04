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
package org.sonatype.nexus.integrationtests;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.ws.rs.HttpMethod;

import org.sonatype.nexus.test.utils.ResponseMatchers;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.hamcrest.Matcher;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;

/**
 * HTTP Request helper (trying to hide any mention of the actual request implementation.)
 * <p/>
 * <b>NOTE</b>: The do${HTTP_METHOD}* methods without a {@link Matcher} parameter will automatically assert that the
 * received response is successful.
 * <p/>
 * <b>IMPORTANT</b>: Any {@link Response} instances returned from methods here should have their
 * {@link Response#release()} method called in a finally block when you are done with it.
 */
public class RequestFacade
{

  public static final String SERVICE_LOCAL = "service/local/";

  private static final NexusRestClient nexusRestClient;

  static {
    nexusRestClient = new NexusRestClient(TestContainer.getInstance().getTestContext());
  }

  /**
   * Null safe method to release a Response ( its streams and sockets )
   *
   * @param response release the response if not null
   */
  public static void releaseResponse(final Response response) {
    nexusRestClient.releaseResponse(response);
  }

  /**
   * Convert a serviceURIPart to a URL
   */
  public static URL toNexusURL(final String serviceURIPart)
      throws IOException
  {
    return nexusRestClient.toNexusURL(serviceURIPart);
  }

  /**
   * Sends a GET request to the specified uri and returns the text of the entity. This method asserts a successful
   * response by passing {@link ResponseMatchers#isSuccessful()} to {@link #doGetForText(String, Matcher)}.
   * <p/>
   * Using this method is RECOMMENDED if you simply want the text of a response and nothing more since this method
   * ensures proper cleanup of any sockets, streams, etc., by releasing the response.
   * <p/>
   * Of course the entire response text is buffered in memory so use this wisely.
   *
   * @param serviceURIpart the non-null part of the uri to fetch that is appended to the Nexus base URI.
   * @return the complete response body text
   * @throws NullPointerException if serviceURIpart is null
   */
  public static String doGetForText(final String serviceURIpart)
      throws IOException
  {
    return nexusRestClient.doGetForText(serviceURIpart);
  }

  public static String doGetForText(final String serviceURIpart,
                                    final org.hamcrest.Matcher<Response> responseMatcher)
      throws IOException
  {
    return nexusRestClient.doGetForText(serviceURIpart, responseMatcher);
  }

  /**
   * Gets the response text, asserting that the entity is not null, and also applying any specified assertions on the
   * response instance.
   */
  public static String doGetForText(final String serviceURIpart, final XStreamRepresentation representation,
                                    org.hamcrest.Matcher<Response> responseMatcher)
      throws IOException
  {
    return nexusRestClient.doGetForText(serviceURIpart, representation, responseMatcher);
  }

  /**
   * Make a request to the service uri specified and return the Status
   *
   * @return a non-null Status with no other assertions made on it.
   */
  public static Status doGetForStatus(final String serviceURIpart)
      throws IOException
  {
    return nexusRestClient.doGetForStatus(serviceURIpart);
  }

  /**
   * GET the status of a request at the specified uri part, asserting that the returned status is not null.
   * <p/>
   * If matcher is non-null, the matcher is applied to the status before returning and this may throw an {@link
   * AssertionError}.
   *
   * @param serviceURIpart url part to be appended to Nexus root url
   * @return a non-null Status with no other assertions made on it.
   * @throws AssertionError if the matcher is non-null and it fails
   */
  public static Status doGetForStatus(final String serviceURIpart, Matcher<Status> matcher)
      throws IOException
  {
    return nexusRestClient.doGetForStatus(serviceURIpart, matcher);
  }

  /**
   * Execute the GET request and assert a successful response.
   */
  public static void doGet(final String serviceURIpart)
      throws IOException
  {
    doGet(serviceURIpart, ResponseMatchers.isSuccessful());
  }

  public static void doGet(final String serviceURIpart, Matcher<Response> matcher)
      throws IOException
  {
    nexusRestClient.doGet(serviceURIpart, matcher);
  }

  /**
   * FIXME this is used everywhere Send a message to a resource as a GET request and return the response.
   * <p/>
   * Ensure you explicity clean up the response entity returned by this method by calling {@link Response#release()}
   *
   * @param serviceURIpart the part of the uri to fetch that is appended to the Nexus base URI.
   * @return the response of the request
   * @throws IOException if there is a problem communicating the response
   */
  public static Response doGetRequest(final String serviceURIpart)
      throws IOException
  {
    return nexusRestClient.doGetRequest(serviceURIpart);
  }

  public static void doPut(final String serviceURIpart, final XStreamRepresentation representation,
                           Matcher<Response> matcher)
      throws IOException
  {
    nexusRestClient.doPut(serviceURIpart, representation, matcher);
  }

  /**
   * PUT a representation to the specified URI
   */
  public static Status doPutForStatus(final String serviceURIpart, final XStreamRepresentation representation,
                                      Matcher<Response> matcher)
      throws IOException
  {
    return nexusRestClient.doPutForStatus(serviceURIpart, representation, matcher);
  }

  public static String doPutForText(final String serviceURIpart, final Representation representation)
      throws IOException
  {
    return nexusRestClient.doPutForText(serviceURIpart, representation);
  }

  public static String doPutForText(final String serviceURIpart, final Representation representation,
                                    Matcher<Response> responseMatcher)
      throws IOException
  {
    return nexusRestClient.doPutForText(serviceURIpart, representation, responseMatcher);
  }

  public static void doPost(final String serviceURIpart, final Representation representation,
                            Matcher<Response> responseMatcher)
      throws IOException
  {
    nexusRestClient.doPost(serviceURIpart, representation, responseMatcher);
  }

  public static String doPostForText(final String serviceURIpart, final Representation representation)
      throws IOException
  {
    return nexusRestClient.doPostForText(serviceURIpart, representation);
  }

  public static String doPostForText(final String serviceURIpart, final Representation representation,
                                     Matcher<Response> responseMatcher)
      throws IOException
  {
    return nexusRestClient.doPostForText(serviceURIpart, representation, responseMatcher);
  }

  public static Status doPostForStatus(final String serviceURIpart, final Representation representation)
      throws IOException
  {
    return nexusRestClient.doPostForStatus(serviceURIpart, representation);
  }

  public static Status doPostForStatus(final String serviceURIpart, final Representation representation,
                                       Matcher<Response> responseMatcher)
      throws IOException
  {
    return nexusRestClient.doPostForStatus(serviceURIpart, representation, responseMatcher);
  }

  public static void doDelete(final String serviceURIpart)
      throws IOException
  {
    nexusRestClient.doDelete(serviceURIpart);
  }

  public static void doDelete(final String serviceURIpart, Matcher<Response> responseMatcher)
      throws IOException
  {
    nexusRestClient.doDelete(serviceURIpart, responseMatcher);
  }

  public static Status doDeleteForStatus(final String serviceURIpart, Matcher<Response> responseMatcher)
      throws IOException
  {
    return nexusRestClient.doDeleteForStatus(serviceURIpart, responseMatcher);
  }

  /**
   * Send a message to a resource and return the response.
   * <p/>
   * Ensure you explicity clean up the response entity returned by this method by calling {@link Response#release()}
   *
   * @param serviceURIpart the part of the uri to fetch that is appended to the Nexus base URI.
   * @param method         the method type of the request
   * @return the response of the request
   * @throws IOException if there is a problem communicating the response
   */
  public static Response sendMessage(final String serviceURIpart, final Method method)
      throws IOException
  {
    return nexusRestClient.sendMessage(serviceURIpart, method);
  }

  /**
   * Send a message to a resource and return the response.
   * <p/>
   * Ensure you explicity clean up the response entity returned by this method by calling {@link Response#release()}
   *
   * @param serviceURIpart the part of the uri to fetch that is appended to the Nexus base URI.
   * @param method         the method type of the request
   * @param representation the representation to map the response to, may be null
   * @return the response of the request
   * @throws IOException if there is a problem communicating the response
   */
  public static Response sendMessage(final String serviceURIpart, final Method method,
                                     final Representation representation)
      throws IOException
  {
    return nexusRestClient.sendMessage(serviceURIpart, method, representation);
  }

  public static Response sendMessage(final URL url, final Method method, final Representation representation)
      throws IOException
  {
    return nexusRestClient.sendMessage(url, method, representation);
  }

  public static Response sendMessage(final String uriPart, final Method method, final Representation representation,
                                     Matcher<Response> matcher)
      throws IOException
  {
    return nexusRestClient.sendMessage(uriPart, method, representation, matcher);
  }

  /**
   * Send a message to a resource and return the response.
   * <p/>
   * Ensure you explicity clean up the response entity returned by this method by calling {@link Response#release()}
   *
   * @param url            the absolute url of the resource to request
   * @param method         the method type of the request
   * @param representation the representation to map the response to, may be null
   * @return the response of the request
   * @throws IOException if there is a problem communicating the response
   */
  public static Response sendMessage(final URL url, final Method method, final Representation representation,
                                     org.hamcrest.Matcher<Response> matchers)
      throws IOException
  {
    return nexusRestClient.sendMessage(url, method, representation, matchers);
  }

  public static Response sendMessage(final Request request, final org.hamcrest.Matcher<Response> matchers)
      throws IOException
  {
    return nexusRestClient.sendMessage(request, matchers);
  }

  /**
   * Download a file at a url and save it to the target file location specified by targetFile.
   *
   * @param url        the url to fetch the file from
   * @param targetFile the location where to save the download
   * @return a File instance for the saved file
   * @throws IOException if there is a problem saving the file
   */
  public static File downloadFile(URL url, String targetFile)
      throws IOException
  {
    return nexusRestClient.downloadFile(url, targetFile);
  }

  /**
   * Execute a HTTPClient method in the context of a test. ie it will use {@link TestContainer#getTestContext()} to
   * make decisions how to execute.
   * <p/>
   * NOTE: Before being returned, {@link HttpMethod#releaseConnection()} is called on the {@link HttpMethod}
   * instance,
   * therefore subsequent calls to get response body as string may return nulls.
   */
  public static HttpResponse executeHTTPClientMethod(HttpUriRequest method)
      throws IOException
  {
    return nexusRestClient.executeHTTPClientMethod(method);
  }

  public static AuthenticationInfo getWagonAuthenticationInfo() {
    AuthenticationInfo authInfo = null;
    // check the text context to see if this is a secure test
    TestContext context = TestContainer.getInstance().getTestContext();
    if (context.isSecureTest()) {
      authInfo = new AuthenticationInfo();
      authInfo.setUserName(context.getUsername());
      authInfo.setPassword(context.getPassword());
    }
    return authInfo;
  }

  /**
   * Execute a HTTPClient method, optionally in the context of a test. ie {@link TestContainer#getTestContext()}
   * <p/>
   * NOTE: Before being returned, {@link HttpMethod#releaseConnection()} is called on the {@link HttpMethod}
   * instance,
   * therefore subsequent calls to get response body as string may return nulls.
   *
   * @param method         the method to execute
   * @param useTestContext if true, execute this request in the context of a Test, false means ignore the testContext
   *                       settings
   * @return the HttpMethod instance passed into this method
   */
  public static HttpResponse executeHTTPClientMethod(final HttpUriRequest method, final boolean useTestContext)
      throws IOException
  {
    return nexusRestClient.executeHTTPClientMethod(method, useTestContext);
  }

  /**
   * Clocks how much time it takes to download a give url
   *
   * @param url address to download
   * @return time in milliseconds
   */
  public static long clockUrlDownload(URL url)
      throws IOException
  {
    return nexusRestClient.clockUrlDownload(url);
  }

  /**
   * Clocks how much time it takes to download a give url
   *
   * @param url        address to download
   * @param speedLimit max speed while downloading in Kbps, -1 to no speed limit
   * @return time in milliseconds
   */
  public static long clockUrlDownload(URL url, int speedLimit)
      throws IOException
  {
    return nexusRestClient.clockUrlDownload(url, speedLimit);
  }

  public static NexusRestClient getNexusRestClient() {
    return nexusRestClient;
  }

}
