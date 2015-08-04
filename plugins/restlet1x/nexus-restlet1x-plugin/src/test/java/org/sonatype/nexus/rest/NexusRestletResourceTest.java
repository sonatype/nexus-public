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
package org.sonatype.nexus.rest;

import java.util.concurrent.ConcurrentHashMap;

import org.sonatype.plexus.rest.resource.PlexusResource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author plynch
 */
@RunWith(MockitoJUnitRunner.class)
public class NexusRestletResourceTest
{

  @Mock
  private Request request;

  @Mock
  private Response response;

  @Mock
  private Context context;

  @Mock
  private PlexusResource plexusResource;

  @Mock
  private Variant variant;

  private NexusRestletResource instance;

  @Before
  public void setup() {
    // prevent NPE only caused by mocking
    when(context.getAttributes()).thenReturn(new ConcurrentHashMap<String, Object>());
    this.instance = spy(new NexusRestletResource(context, request, response, plexusResource));
  }

  @Test
  public void representResourceExceptionWithStatusNull() throws ResourceException {

    // possible that status is null in ResourceException, make sure our status checking does not throw NPE checking status
    // make the call to super.represent throw ResourceException
    // fyi there is no way to directly mock a super call, so we have to mock the underlying bits
    ResourceException expected = new ResourceException((Status) null, new Throwable());
    doThrow(expected).when(plexusResource).get(context, request, response, variant);

    try {
      instance.represent(variant);
      fail("ResourceException expected.");
    }
    catch (ResourceException actual) {
      assertThat(actual, equalTo(expected));
    }

    // verify underlying bits - this verifies our assumption that the super implementation calls plexuResource.get()
    // to protect against changes in super hiding a change in test prepare
    verify(plexusResource).get(context, request, response, variant);
    //verify no apr since we don't have a status code
    verify(instance).handleError(expected);
  }

  @Test
  public void representResourceExceptionWithStatus5xx() throws ResourceException {
    // brute force 5xx errors
    for (int i = 500; i < 600; i++) {
      ResourceException expected = new ResourceException(i);
      doThrow(expected).when(plexusResource).get(context, request, response, variant);

      try {
        instance.represent(variant);
        fail("ResourceException expected.");
      }
      catch (ResourceException actual) {
        assertThat(actual, equalTo(expected));
      }

      // verify when APR is called
      if (503 == i) {
        verify(instance, never()).handleError(expected);
      }
      else {
        verify(instance).handleError(expected);
      }


    }


  }
}
