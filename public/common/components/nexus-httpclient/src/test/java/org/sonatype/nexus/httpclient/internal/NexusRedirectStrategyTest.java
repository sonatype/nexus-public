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
package org.sonatype.nexus.httpclient.internal;

import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.net.HttpHeaders.LOCATION;
import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.sonatype.nexus.httpclient.internal.NexusRedirectStrategy.CONTENT_RETRIEVAL_MARKER_KEY;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link NexusRedirectStrategy}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class NexusRedirectStrategyTest
{
  @Mock
  private HttpResponse response;

  @Mock
  private StatusLine statusLine;

  private HttpGet request;

  @Test
  public void doNotFollowRedirectsToDirIndex() throws Exception {
    when(response.getStatusLine()).thenReturn(statusLine);

    final RedirectStrategy underTest = new NexusRedirectStrategy();
    HttpContext httpContext;

    // no location header
    request = new HttpGet("http://localhost/dir/fileA");
    httpContext = new BasicHttpContext();
    httpContext.setAttribute(CONTENT_RETRIEVAL_MARKER_KEY, Boolean.TRUE);
    when(statusLine.getStatusCode()).thenReturn(SC_OK);
    assertThat(underTest.isRedirected(request, response, httpContext), is(false));

    // redirect to file
    request = new HttpGet("http://localhost/dir/fileA");
    httpContext = new BasicHttpContext();
    httpContext.setAttribute(CONTENT_RETRIEVAL_MARKER_KEY, Boolean.TRUE);
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);
    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "http://localhost/dir/fileB"));
    assertThat(underTest.isRedirected(request, response, httpContext), is(true));

    // redirect to dir
    request = new HttpGet("http://localhost/dir");
    httpContext = new BasicHttpContext();
    httpContext.setAttribute(CONTENT_RETRIEVAL_MARKER_KEY, Boolean.TRUE);
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);
    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "http://localhost/dir/"));
    assertThat(underTest.isRedirected(request, response, httpContext), is(false));
  }

  @Test
  public void doFollowCrossSiteRedirects() throws Exception {
    when(response.getStatusLine()).thenReturn(statusLine);

    final RedirectStrategy underTest = new NexusRedirectStrategy();

    // simple cross redirect
    request = new HttpGet("http://hostA/dir");
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);
    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "http://hostB/dir"));
    assertThat(underTest.isRedirected(request, response, new BasicHttpContext()), is(true));

    // cross redirect to dir (failed coz NEXUS-5744)
    request = new HttpGet("http://hostA/dir/");
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);
    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "http://hostB/dir/"));
    assertThat(underTest.isRedirected(request, response, new BasicHttpContext()), is(true));
  }

  @Test
  public void preserveEncodedCharactersTrue_KeepsEncodedCharacters() throws Exception {
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);

    final NexusRedirectStrategy underTest = new NexusRedirectStrategy();

    // Test when preserveEncodedCharacters is true, keeps %2B as %2B
    request = new HttpGet("http://localhost/file%2Bname.txt");
    HttpContext httpContext = new BasicHttpContext();
    httpContext.setAttribute("preserveEncodedCharacters", Boolean.TRUE);

    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "http://localhost/redirect/file%2Bname.txt"));

    URI redirectUri = underTest.getLocationURI(request, response, httpContext);

    // Should preserve %2B without normalization
    assertThat(redirectUri.toString(), is("http://localhost/redirect/file%2Bname.txt"));
  }

  @Test
  public void preserveEncodedCharactersFalse_KeepsLiteralPlusCharacters() throws Exception {
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);

    final NexusRedirectStrategy underTest = new NexusRedirectStrategy();

    // Test when preserveEncodedCharacters is false, keeps + as + (default behavior for crates.io compatibility)
    request = new HttpGet("http://localhost/libgit2-sys-0.13.1+1.4.2.crate");
    HttpContext httpContext = new BasicHttpContext();
    httpContext.setAttribute("preserveEncodedCharacters", Boolean.FALSE);

    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "http://s3.amazonaws.com/crates/libgit2-sys-0.13.1+1.4.2.crate"));

    URI redirectUri = underTest.getLocationURI(request, response, httpContext);

    // Should preserve + as literal character (default HttpClient behavior)
    assertThat(redirectUri.toString(), is("http://s3.amazonaws.com/crates/libgit2-sys-0.13.1+1.4.2.crate"));
  }

  @Test
  public void preserveEncodedCharactersFalse_UsesDefaultBehavior() throws Exception {
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);

    final NexusRedirectStrategy underTest = new NexusRedirectStrategy();

    // Test when preserveEncodedCharacters is false, uses default behavior (normalization)
    request = new HttpGet("http://localhost/file+name.txt");
    HttpContext httpContext = new BasicHttpContext();
    httpContext.setAttribute("preserveEncodedCharacters", Boolean.FALSE);

    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "http://localhost/redirect/file+name.txt"));

    URI redirectUri = underTest.getLocationURI(request, response, httpContext);

    // Should normalize (default behavior)
    assertThat(redirectUri.getPath(), is("/redirect/file+name.txt"));
  }

  @Test
  public void preserveEncodedCharactersNotSet_UsesDefaultBehavior() throws Exception {
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);

    final NexusRedirectStrategy underTest = new NexusRedirectStrategy();

    // Test when preserveEncodedCharacters is not set in context (legacy behavior)
    request = new HttpGet("http://localhost/file+name.txt");
    HttpContext httpContext = new BasicHttpContext();
    // No preserveEncodedCharacters attribute set

    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "http://localhost/redirect/file+name.txt"));

    URI redirectUri = underTest.getLocationURI(request, response, httpContext);

    // Should use default behavior (normalization)
    assertThat(redirectUri.getPath(), is("/redirect/file+name.txt"));
  }

  @Test
  public void preserveEncodedCharactersTrue_RealWorldAwsS3Redirect() throws Exception {
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);

    final NexusRedirectStrategy underTest = new NexusRedirectStrategy();

    // Real-world AWS S3 scenario: C++ package with %2B encoding
    // When preserveEncodedCharacters is true, keeps %2B as %2B
    request = new HttpGet("http://localhost/packages/ncurses-c%2B%2B-libs-6.2-4.rpm");
    HttpContext httpContext = new BasicHttpContext();
    httpContext.setAttribute("preserveEncodedCharacters", Boolean.TRUE);

    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "https://s3.amazonaws.com/bucket/ncurses-c%2B%2B-libs-6.2-4.rpm"));

    URI redirectUri = underTest.getLocationURI(request, response, httpContext);

    // Should keep %2B as %2B for AWS S3 compatibility
    assertThat(redirectUri.toString(), is("https://s3.amazonaws.com/bucket/ncurses-c%2B%2B-libs-6.2-4.rpm"));
  }

  @Test
  public void preserveEncodedCharactersFalse_RealWorldCratesIoRedirect() throws Exception {
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);

    final NexusRedirectStrategy underTest = new NexusRedirectStrategy();

    // Real-world crates.io scenario: version with literal +
    // When preserveEncodedCharacters is false (default), preserves + for backward compatibility
    request = new HttpGet("http://localhost/crates/libgit2-sys/libgit2-sys-0.13.1+1.4.2.crate");
    HttpContext httpContext = new BasicHttpContext();
    httpContext.setAttribute("preserveEncodedCharacters", Boolean.FALSE);

    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(
            new BasicHeader(LOCATION, "https://static.crates.io/crates/libgit2-sys/libgit2-sys-0.13.1+1.4.2.crate"));

    URI redirectUri = underTest.getLocationURI(request, response, httpContext);

    // Should keep + as + for crates.io compatibility (default HttpClient behavior)
    assertThat(redirectUri.toString(),
        is("https://static.crates.io/crates/libgit2-sys/libgit2-sys-0.13.1+1.4.2.crate"));
  }

  @Test
  public void preserveEncodedCharactersTrue_NormalizesPathTraversal() throws Exception {
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);

    final NexusRedirectStrategy underTest = new NexusRedirectStrategy();

    // Test that path traversal sequences are normalized (security fix)
    request = new HttpGet("http://localhost/packages/file.rpm");
    HttpContext httpContext = new BasicHttpContext();
    httpContext.setAttribute("preserveEncodedCharacters", Boolean.TRUE);

    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "https://s3.amazonaws.com/bucket/../../../etc/passwd"));

    URI redirectUri = underTest.getLocationURI(request, response, httpContext);

    // Path traversal should be normalized: /bucket/../../../etc/passwd -> /etc/passwd
    assertThat(redirectUri.toString(), is("https://s3.amazonaws.com/etc/passwd"));
  }

  @Test
  public void preserveEncodedCharactersTrue_NormalizesEncodedPathTraversal() throws Exception {
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);

    final NexusRedirectStrategy underTest = new NexusRedirectStrategy();

    // Test that encoded path traversal sequences are normalized
    request = new HttpGet("http://localhost/packages/file.rpm");
    HttpContext httpContext = new BasicHttpContext();
    httpContext.setAttribute("preserveEncodedCharacters", Boolean.TRUE);

    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "https://s3.amazonaws.com/bucket/%2e%2e/%2e%2e/etc/passwd"));

    URI redirectUri = underTest.getLocationURI(request, response, httpContext);

    // Encoded path traversal should be normalized
    assertThat(redirectUri.getPath(), is("/etc/passwd"));
  }

  @Test
  public void preserveEncodedCharactersTrue_PreservesEncodingWithoutTraversal() throws Exception {
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);

    final NexusRedirectStrategy underTest = new NexusRedirectStrategy();

    // Test that percent-encoding is preserved when there's no path traversal
    request = new HttpGet("http://localhost/packages/file.rpm");
    HttpContext httpContext = new BasicHttpContext();
    httpContext.setAttribute("preserveEncodedCharacters", Boolean.TRUE);

    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "https://s3.amazonaws.com/bucket/file%2Bname%23test.rpm"));

    URI redirectUri = underTest.getLocationURI(request, response, httpContext);

    // Percent-encoding should be preserved (%2B and %23 remain encoded)
    assertThat(redirectUri.toString(), is("https://s3.amazonaws.com/bucket/file%2Bname%23test.rpm"));
  }

  @Test
  public void preserveEncodedCharactersTrue_NormalizesCurrentDirectoryReferences() throws Exception {
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(SC_MOVED_TEMPORARILY);

    final NexusRedirectStrategy underTest = new NexusRedirectStrategy();

    // Test that current directory references (.) are normalized
    request = new HttpGet("http://localhost/packages/file.rpm");
    HttpContext httpContext = new BasicHttpContext();
    httpContext.setAttribute("preserveEncodedCharacters", Boolean.TRUE);

    when(response.getFirstHeader(argThat(equalToIgnoringCase(LOCATION))))
        .thenReturn(new BasicHeader(LOCATION, "https://s3.amazonaws.com/bucket/./subdir/./file.rpm"));

    URI redirectUri = underTest.getLocationURI(request, response, httpContext);

    // Current directory references should be removed: /bucket/./subdir/./file.rpm -> /bucket/subdir/file.rpm
    assertThat(redirectUri.toString(), is("https://s3.amazonaws.com/bucket/subdir/file.rpm"));
  }
}
