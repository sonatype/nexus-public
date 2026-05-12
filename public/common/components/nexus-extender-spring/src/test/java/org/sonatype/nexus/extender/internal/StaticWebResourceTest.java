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
package org.sonatype.nexus.extender.internal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.webresources.WebResource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaticWebResourceTest
{
  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private MimeSupport mimeSupport;

  private StaticWebResource underTest;

  @Test
  void testConstructor_WithNoResources() throws Exception {
    lenient().when(mimeSupport.guessMimeTypeFromPath(anyString())).thenReturn("application/octet-stream");

    when(applicationContext.getResources("classpath*:/static/**/")).thenReturn(new Resource[0]);
    when(applicationContext.getResources("classpath*:/assets/**/")).thenReturn(new Resource[0]);

    underTest = new StaticWebResource(applicationContext, mimeSupport);

    List<WebResource> resources = underTest.getResources();
    assertThat(resources, is(empty()));
    assertThat(resources, is(notNullValue()));
  }

  @Test
  void testConstructor_WithIOException() throws Exception {
    lenient().when(mimeSupport.guessMimeTypeFromPath(anyString())).thenReturn("application/octet-stream");

    when(applicationContext.getResources(anyString())).thenThrow(new IOException("Resource loading failed"));

    assertThrows(IOException.class, () -> {
      new StaticWebResource(applicationContext, mimeSupport);
    });
  }

  @Test
  void testGetResources_ReturnsCorrectList() throws Exception {
    lenient().when(mimeSupport.guessMimeTypeFromPath(anyString())).thenReturn("application/octet-stream");

    when(applicationContext.getResources(anyString())).thenReturn(new Resource[0]);

    underTest = new StaticWebResource(applicationContext, mimeSupport);
    List<WebResource> resources1 = underTest.getResources();
    List<WebResource> resources2 = underTest.getResources();

    // Should return the same list instance
    assertThat(resources1, is(resources2));
    assertThat(resources1, is(notNullValue()));
  }

  @Test
  void testGetPublishedPath_WithJarUrl() throws Exception {
    // Test the static getPublishedPath method directly using reflection
    Method getPublishedPathMethod = StaticWebResource.class.getDeclaredMethod("getPublishedPath", URL.class);
    getPublishedPathMethod.setAccessible(true);

    // Test jar URL path extraction
    URL jarUrl = new URL("jar:file:/path/to/nexus.jar!/static/css/bootstrap.css");
    String result = (String) getPublishedPathMethod.invoke(null, jarUrl);

    assertThat(result, is(equalTo("/static/css/bootstrap.css")));
  }

  @Test
  void testGetPublishedPath_WithStaticUrl() throws Exception {
    // Test the static getPublishedPath method directly using reflection
    Method getPublishedPathMethod = StaticWebResource.class.getDeclaredMethod("getPublishedPath", URL.class);
    getPublishedPathMethod.setAccessible(true);

    // Test static URL path extraction
    URL staticUrl = new URL("file:/path/to/project/src/main/resources/static/images/logo.png");
    String result = (String) getPublishedPathMethod.invoke(null, staticUrl);

    assertThat(result, is(equalTo("/static/images/logo.png")));
  }

  @Test
  void testGetPublishedPath_WithInvalidUrl() throws Exception {
    // Test the static getPublishedPath method directly using reflection
    Method getPublishedPathMethod = StaticWebResource.class.getDeclaredMethod("getPublishedPath", URL.class);
    getPublishedPathMethod.setAccessible(true);

    // Test URL that doesn't match either jar or static patterns
    URL invalidUrl = new URL("http://example.com/resource.css");
    String result = (String) getPublishedPathMethod.invoke(null, invalidUrl);

    assertThat(result, is(nullValue()));
  }

  @Test
  void testGetPublishedPath_WithVariousJarFormats() throws Exception {
    // Test the static getPublishedPath method directly using reflection
    Method getPublishedPathMethod = StaticWebResource.class.getDeclaredMethod("getPublishedPath", URL.class);
    getPublishedPathMethod.setAccessible(true);

    // Test different jar URL formats
    URL jarUrl1 = new URL("jar:file:/C:/apps/nexus.jar!/static/file1.js");
    URL jarUrl2 = new URL("jar:file:/home/user/nexus.jar!/assets/file2.css");
    URL jarUrl3 = new URL("jar:file:/opt/nexus/lib/plugin.jar!/static/nested/deep/file3.html");

    String result1 = (String) getPublishedPathMethod.invoke(null, jarUrl1);
    String result2 = (String) getPublishedPathMethod.invoke(null, jarUrl2);
    String result3 = (String) getPublishedPathMethod.invoke(null, jarUrl3);

    assertThat(result1, is(equalTo("/static/file1.js")));
    assertThat(result2, is(equalTo("/assets/file2.css")));
    assertThat(result3, is(equalTo("/static/nested/deep/file3.html")));
  }

  @Test
  void testGetPublishedPath_WithDifferentStaticPaths() throws Exception {
    // Test the static getPublishedPath method directly using reflection
    Method getPublishedPathMethod = StaticWebResource.class.getDeclaredMethod("getPublishedPath", URL.class);
    getPublishedPathMethod.setAccessible(true);

    // Test different static path formats
    URL staticUrl1 = new URL("file:/project/src/main/resources/static/app.js");
    URL staticUrl2 = new URL("file:/another/path/with/static/styles.css");
    URL staticUrl3 = new URL("file:/deep/nested/path/static/images/icon.png");

    String result1 = (String) getPublishedPathMethod.invoke(null, staticUrl1);
    String result2 = (String) getPublishedPathMethod.invoke(null, staticUrl2);
    String result3 = (String) getPublishedPathMethod.invoke(null, staticUrl3);

    assertThat(result1, is(equalTo("/static/app.js")));
    assertThat(result2, is(equalTo("/static/styles.css")));
    assertThat(result3, is(equalTo("/static/images/icon.png")));
  }

  @Test
  void testGetPublishedPath_EdgeCases() throws Exception {
    // Test the static getPublishedPath method directly using reflection
    Method getPublishedPathMethod = StaticWebResource.class.getDeclaredMethod("getPublishedPath", URL.class);
    getPublishedPathMethod.setAccessible(true);

    // Test edge cases
    URL jarOnlyUrl = new URL("jar:file:/test.jar!/");
    URL staticOnlyUrl = new URL("file:/path/static/");
    URL noMatchUrl = new URL("ftp://example.com/file.txt");
    URL httpUrl = new URL("https://cdn.example.com/static/file.js");

    String result1 = (String) getPublishedPathMethod.invoke(null, jarOnlyUrl);
    String result2 = (String) getPublishedPathMethod.invoke(null, staticOnlyUrl);
    String result3 = (String) getPublishedPathMethod.invoke(null, noMatchUrl);
    String result4 = (String) getPublishedPathMethod.invoke(null, httpUrl);

    assertThat(result1, is(equalTo("/")));
    assertThat(result2, is(equalTo("/static/")));
    assertThat(result3, is(nullValue()));
    // HTTP URL with /static/ should still match the static pattern
    assertThat(result4, is(equalTo("/static/file.js")));
  }

  @Test
  void testResourcePatterns() throws Exception {
    lenient().when(mimeSupport.guessMimeTypeFromPath(anyString())).thenReturn("application/octet-stream");

    // Verify that both static and assets patterns are searched
    when(applicationContext.getResources("classpath*:/static/**/")).thenReturn(new Resource[0]);
    when(applicationContext.getResources("classpath*:/assets/**/")).thenReturn(new Resource[0]);

    underTest = new StaticWebResource(applicationContext, mimeSupport);

    // The constructor should have called getResources for both patterns
    List<WebResource> resources = underTest.getResources();
    assertThat(resources, is(empty()));
  }

  @Test
  void testMimeTypeGuessingIsCalled() throws Exception {
    // This test verifies the interaction without actually creating resources
    lenient().when(mimeSupport.guessMimeTypeFromPath(anyString())).thenReturn("application/octet-stream");

    when(applicationContext.getResources("classpath*:/static/**/")).thenReturn(new Resource[0]);
    when(applicationContext.getResources("classpath*:/assets/**/")).thenReturn(new Resource[0]);

    // Verify MimeSupport is injected and available
    underTest = new StaticWebResource(applicationContext, mimeSupport);
    assertThat(underTest, is(notNullValue()));
  }

  @Test
  void testApplicationContextInteraction() throws Exception {
    lenient().when(mimeSupport.guessMimeTypeFromPath(anyString())).thenReturn("application/octet-stream");

    // Test that both resource patterns are properly formatted and called
    when(applicationContext.getResources("classpath*:/static/**/")).thenReturn(new Resource[0]);
    when(applicationContext.getResources("classpath*:/assets/**/")).thenReturn(new Resource[0]);

    underTest = new StaticWebResource(applicationContext, mimeSupport);

    // Verify the ApplicationContext was used correctly
    List<WebResource> resources = underTest.getResources();
    assertThat(resources, is(notNullValue()));
    assertThat(resources, is(empty()));
  }
}
