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
package org.sonatype.nexus.siesta;

import java.util.Collections;

import org.sonatype.nexus.siesta.internal.resteasy.ComponentContainerImpl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResourceInvoker;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiestaResourceMethodFinderTest
{
  private SiestaResourceMethodFinder underTest;

  @Mock
  private ComponentContainerImpl componentContainer;

  @Mock
  private ResteasyDeployment deployment;

  @Mock
  private Registry registry;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private ResourceMethodInvoker resourceMethodInvoker;

  @BeforeEach
  void setUp() {
    underTest = new SiestaResourceMethodFinder(componentContainer, deployment);
  }

  @Test
  void testConstructorWithNullComponentContainer() {
    assertThrows(NullPointerException.class, () -> new SiestaResourceMethodFinder(null, deployment));
  }

  @Test
  void testConstructorWithNullDeployment() {
    assertThrows(NullPointerException.class, () -> new SiestaResourceMethodFinder(componentContainer, null));
  }

  @Test
  void testGetResourceMethodPath() {
    when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    when(request.getContextPath()).thenReturn("/nexus");
    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8081/nexus/service/rest"));
    when(deployment.getRegistry()).thenReturn(registry);
    when(deployment.getRegistry().getResourceInvoker(any())).thenReturn(resourceMethodInvoker);
    doReturn(TestResource.class).when(resourceMethodInvoker).getResourceClass();
    when(resourceMethodInvoker.getMethod()).thenReturn(TestResource.class.getMethods()[0]);

    String path = underTest.getResourceMethodPath(request, response);

    assertEquals("/test/class/path/test/method/path", path);
  }

  @Test
  void testGetResourceMethodPathWithoutLeadingSlashes() {
    when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    when(request.getContextPath()).thenReturn("/nexus");
    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8081/nexus/service/rest"));
    when(deployment.getRegistry()).thenReturn(registry);
    when(deployment.getRegistry().getResourceInvoker(any())).thenReturn(resourceMethodInvoker);
    doReturn(ResourceWithoutLeadingSlash.class).when(resourceMethodInvoker).getResourceClass();
    when(resourceMethodInvoker.getMethod()).thenReturn(ResourceWithoutLeadingSlash.class.getMethods()[0]);

    String path = underTest.getResourceMethodPath(request, response);

    assertEquals("/class/path/method/path", path);
  }

  @Test
  void testGetResourceMethodPathWithDoubleSlashes() {
    when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    when(request.getContextPath()).thenReturn("/nexus");
    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8081/nexus/service/rest"));
    when(deployment.getRegistry()).thenReturn(registry);
    when(deployment.getRegistry().getResourceInvoker(any())).thenReturn(resourceMethodInvoker);
    doReturn(ResourceWithDoubleSlashes.class).when(resourceMethodInvoker).getResourceClass();
    when(resourceMethodInvoker.getMethod()).thenReturn(ResourceWithDoubleSlashes.class.getMethods()[0]);

    String path = underTest.getResourceMethodPath(request, response);

    assertEquals("/class/path/method/path/", path);
  }

  /*
   * NEXUS-53168 - Saw an instanceof org.jboss.resteasy.core.registry.ConstantResourceInvoker
   */
  @Test
  void testGetResourceMethodPathWhenInvokerIsNotResourceMethodInvoker() {
    when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    when(request.getContextPath()).thenReturn("/nexus");
    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8081/nexus/service/rest"));
    when(deployment.getRegistry()).thenReturn(registry);
    ResourceInvoker otherInvoker = org.mockito.Mockito.mock(ResourceInvoker.class);
    when(deployment.getRegistry().getResourceInvoker(any())).thenReturn(otherInvoker);

    String path = underTest.getResourceMethodPath(request, response);

    assertEquals("", path);
  }

  @Test
  void testGetResourceMethodPathWithOnlyClassPath() {
    when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    when(request.getContextPath()).thenReturn("/nexus");
    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8081/nexus/service/rest"));
    when(deployment.getRegistry()).thenReturn(registry);
    when(deployment.getRegistry().getResourceInvoker(any())).thenReturn(resourceMethodInvoker);
    doReturn(ResourceWithOnlyClassPath.class).when(resourceMethodInvoker).getResourceClass();
    when(resourceMethodInvoker.getMethod()).thenReturn(ResourceWithOnlyClassPath.class.getMethods()[0]);

    String path = underTest.getResourceMethodPath(request, response);

    assertEquals("/class/path/only", path);
  }

  @Test
  void testGetResourceMethodPathWithOnlyMethodPath() {
    when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    when(request.getContextPath()).thenReturn("/nexus");
    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8081/nexus/service/rest"));
    when(deployment.getRegistry()).thenReturn(registry);
    when(deployment.getRegistry().getResourceInvoker(any())).thenReturn(resourceMethodInvoker);
    doReturn(ResourceWithOnlyMethodPath.class).when(resourceMethodInvoker).getResourceClass();
    when(resourceMethodInvoker.getMethod()).thenReturn(ResourceWithOnlyMethodPath.class.getMethods()[0]);

    String path = underTest.getResourceMethodPath(request, response);

    assertEquals("/method/path/only", path);
  }

  @Test
  void testGetResourceMethodPathWithNoPaths() {
    when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    when(request.getContextPath()).thenReturn("/nexus");
    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8081/nexus/service/rest"));
    when(deployment.getRegistry()).thenReturn(registry);
    when(deployment.getRegistry().getResourceInvoker(any())).thenReturn(resourceMethodInvoker);
    doReturn(ResourceWithNoPaths.class).when(resourceMethodInvoker).getResourceClass();
    when(resourceMethodInvoker.getMethod()).thenReturn(ResourceWithNoPaths.class.getMethods()[0]);

    String path = underTest.getResourceMethodPath(request, response);

    assertEquals("", path);
  }

  @Test
  void testGetResourceMethod() {
    when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    when(request.getContextPath()).thenReturn("/nexus");
    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8081/nexus/service/rest"));
    when(request.getMethod()).thenReturn("GET");
    when(request.getServletContext()).thenReturn(org.mockito.Mockito.mock(jakarta.servlet.ServletContext.class));
    when(deployment.getRegistry()).thenReturn(registry);
    when(deployment.getRegistry().getResourceInvoker(any())).thenReturn(resourceMethodInvoker);

    ResourceInvoker invoker = underTest.getResourceMethod(request, response);

    assertNotNull(invoker);
  }

  @Path("/test/class/path")
  private static class TestResource
  {
    @Path("/test/method/path")
    public void testMethod() {
      // empty
    }
  }

  @Path("class/path")
  private static class ResourceWithoutLeadingSlash
  {
    @Path("method/path")
    public void testMethod() {
      // empty
    }
  }

  @Path("/class/path/")
  private static class ResourceWithDoubleSlashes
  {
    @Path("/method/path/")
    public void testMethod() {
      // empty
    }
  }

  @Path("/class/path/only")
  private static class ResourceWithOnlyClassPath
  {
    public void testMethod() {
      // empty
    }
  }

  private static class ResourceWithOnlyMethodPath
  {
    @Path("/method/path/only")
    public void testMethod() {
      // empty
    }
  }

  private static class ResourceWithNoPaths
  {
    public void testMethod() {
      // empty
    }
  }
}
