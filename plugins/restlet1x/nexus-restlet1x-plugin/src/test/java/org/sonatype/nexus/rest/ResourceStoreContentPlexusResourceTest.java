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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.sonatype.nexus.build.BuildApplicationStatusSource;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.ResourceStore;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.attributes.Attributes;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.item.uid.IsRemotelyAccessibleAttribute;
import org.sonatype.nexus.proxy.router.RepositoryRouter;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.security.SecuritySystem;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.collect.Maps;
import com.noelios.restlet.http.HttpResponse;
import com.noelios.restlet.http.HttpServerCall;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.restlet.Context;
import org.restlet.data.ClientInfo;
import org.restlet.data.Conditions;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.data.Tag;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AbstractResourceStoreContentPlexusResource}
 */
public class ResourceStoreContentPlexusResourceTest
    extends TestSupport
{

  private Map<String, ArtifactViewProvider> views = Maps.newHashMap();

  private AbstractResourceStoreContentPlexusResource underTest;

  @Mock
  private SecuritySystem security;

  @Mock
  private RepositoryRouter rootRouter;

  @Mock
  private Context context;

  @Mock
  private Request request;

  @Mock
  private Variant variant;

  @Mock
  private ResourceStore resourceStore;

  @Mock
  private StorageCollectionItem collectionItem;

  @Mock
  private StorageFileItem fileItem;

  @Mock
  private StorageLinkItem linkItem;

  @Mock
  private HttpResponse response;

  @Mock
  private HttpServerCall httpCall;

  @Mock
  private Series headers;

  @Mock
  private Reference reference;

  @Mock
  private Attributes attributes;

  @Mock
  private RepositoryItemUid itemUid;

  @Before
  public void setup() {

    underTest = new AbstractResourceStoreContentPlexusResource(security, new BuildApplicationStatusSource(), views)
    {
      @Override
      protected ResourceStore getResourceStore(final Request request)
          throws NoSuchResourceStoreException, ResourceException
      {
        return null;
      }

      @Override
      public String getResourceUri() {
        return null;
      }

      @Override
      public PathProtectionDescriptor getResourceProtection() {
        return null;
      }

      @Override
      public Object getPayloadInstance() {
        return null;
      }

      @Override
      protected Logger getLogger() {
        return LoggerFactory.getLogger(ResourceStoreContentPlexusResourceTest.class);
      }

      @Override
      protected String getValidRemoteIPAddress(Request request) {
        // for test simplicity, to not have to mock layers of HTTPCall/Request
        return "127.0.0.1";
      }

      protected Reference getContextRoot(Request request) {
        // unimportant, as we don't go anywhere else than resource itself
        return reference;
      }

      @Override
      protected RepositoryRouter getRepositoryRouter() {
        return rootRouter;
      }
    };

    when(request.getResourceRef()).thenReturn(reference);
    when(reference.toString()).thenReturn("");
    when(reference.getQueryAsForm()).thenReturn(new Form());

    when(request.getConditions()).thenReturn(new Conditions());

    when(response.getHttpCall()).thenReturn(httpCall);
    when(httpCall.getResponseHeaders()).thenReturn(headers);

    when(itemUid.getBooleanAttributeValue(IsRemotelyAccessibleAttribute.class)).thenReturn(true);

    when(collectionItem.getRepositoryItemUid()).thenReturn(itemUid);
    when(fileItem.getRepositoryItemUid()).thenReturn(itemUid);
    when(fileItem.getResourceStoreRequest()).thenReturn(new ResourceStoreRequest("/some/path"));
    when(fileItem.getRepositoryItemAttributes()).thenReturn(attributes);
    when(fileItem.getItemContext()).thenReturn(new RequestContext());

  }

  @Test
  public void testNexus5155CacheHeadersForCollectionItems()
      throws ResourceException, NoSuchResourceStoreException, IOException, IllegalOperationException,
             ItemNotFoundException, AccessDeniedException
  {
    underTest.renderStorageCollectionItem(context, request, response, variant, resourceStore, collectionItem);

    verify(headers).add("Pragma", "no-cache");
    verify(headers).add("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
  }

  @Test
  public void testNexus5155CacheHeadersForLinkedCollectionItems()
      throws Exception
  {
    when(rootRouter.dereferenceLink(linkItem)).thenReturn(collectionItem);

    underTest.renderStorageLinkItem(context, request, response, variant, resourceStore, linkItem);

    verify(headers).add("Pragma", "no-cache");
    verify(headers).add("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
  }

  @Test
  public void testNexus5155OmitCacheHeadersForFileItems()
      throws Exception
  {
    underTest.renderStorageFileItem(request, fileItem);

    verifyZeroInteractions(headers);
  }

  @Test
  public void testNexus5155OmitCacheHeadersForLinkedFileItems()
      throws Exception
  {
    when(rootRouter.dereferenceLink(linkItem)).thenReturn(fileItem);

    underTest.renderStorageLinkItem(context, request, response, variant, resourceStore, linkItem);

    verifyZeroInteractions(headers);
  }

  /**
   * This test simulates the Request call in a way Restlet does when no if-modified-since condition is present. So we
   * verify that created ResourceStoreRequest does not have condition set.
   */
  @Test
  public void testNexus5704IfNoneMatchNoNPEWithNoETag() {
    final Conditions conditions = new Conditions();
    when(request.getConditions()).thenReturn(conditions);

    // we need more mock responses as getResourceStoreRequest tries to gather
    // as much info as it can (from client, reference, baseUrl etc
    when(request.getClientInfo()).thenReturn(new ClientInfo());
    when(request.getOriginalRef()).thenReturn(reference);
    final ResourceStoreRequest rsr = underTest.getResourceStoreRequest(request, "/some/path");

    assertThat(rsr.getIfNoneMatch(), nullValue());
  }

  /**
   * This test simulates the Request call in a way Restlet would do when ETag is badly formatted (is not quoted):
   * parsing of HTTP request with ETag without quotes will detect the presence of condition (Conditions
   * if-modified-since will be non empty list), but the only one member of the list is actually {@code null} (as saw
   * when debugged NEXUS-5704). So we verify that created ResourceStoreRequest does not have condition set AND we
   * don't have NPE either.
   */
  @Test
  public void testNexus5704IfNoneMatchNoNPEWithBadlyParsedETag() {
    final Conditions conditions = new Conditions();
    // ETag will be list with one element: null when ETag field is badly formatted
    // (as seen from DEBUG)
    conditions.setNoneMatch(Collections.singletonList((Tag) null));
    when(request.getConditions()).thenReturn(conditions);

    // we need more mock responses as getResourceStoreRequest tries to gather
    // as much info as it can (from client, reference, baseUrl etc
    when(request.getClientInfo()).thenReturn(new ClientInfo());
    when(request.getOriginalRef()).thenReturn(reference);
    final ResourceStoreRequest rsr = underTest.getResourceStoreRequest(request, "/some/path");

    assertThat(rsr.getIfNoneMatch(), nullValue());
  }

  /**
   * This test simulates the Request in a way Restlet does: parsing of HTTP request with ETag with quotes will be OK.
   * So we verify is it detected and added to created ResourceStoreRequest.
   */
  @Test
  public void testNexus5704IfNoneMatchNoNPEWithParsedETag() {
    final Conditions conditions = new Conditions();
    conditions.setNoneMatch(Collections.singletonList(new Tag("{SHA1{fake-sha1-string}}", false)));
    when(request.getConditions()).thenReturn(conditions);

    // we need more mock responses as getResourceStoreRequest tries to gather
    // as much info as it can (from client, reference, baseUrl etc
    when(request.getClientInfo()).thenReturn(new ClientInfo());
    when(request.getOriginalRef()).thenReturn(reference);
    final ResourceStoreRequest rsr = underTest.getResourceStoreRequest(request, "/some/path");

    assertThat(rsr.getIfNoneMatch(), equalTo("{SHA1{fake-sha1-string}}"));
  }

  /**
   * With matching condition (client known ETag and Nexus ETag matches) should result in {@link ResourceException}
   * carrying status 304 Not Modified.
   */
  @Test
  public void testNexus5704renderStorageFileItemWithMatchingCondition()
      throws Exception
  {
    try {
      final ResourceStoreRequest rsr = fileItem.getResourceStoreRequest();
      rsr.setIfNoneMatch("{SHA1{1234567890}}");
      when(attributes.containsKey(StorageFileItem.DIGEST_SHA1_KEY)).thenReturn(true);
      when(attributes.get(StorageFileItem.DIGEST_SHA1_KEY)).thenReturn("1234567890");

      underTest.renderStorageFileItem(request, fileItem);
      fail("ResourceException is expected to be thrown");
    }
    catch (ResourceException e) {
      assertThat(e.getStatus(), equalTo(Status.REDIRECTION_NOT_MODIFIED));
    }
  }

  /**
   * With non matching condition (client known ETag and Nexus ETag differs), the {@link
   * StorageFileItemRepresentation}
   * should be returned that wraps the passed in {@link StorageFileItem} instance.
   */
  @Test
  public void testNexus5704renderStorageFileItemWithNonMatchingCondition()
      throws Exception
  {
    final ResourceStoreRequest rsr = fileItem.getResourceStoreRequest();
    rsr.setIfNoneMatch("{SHA1{1234567890}}"); // client "knows" hash 1234567890
    when(attributes.containsKey(StorageFileItem.DIGEST_SHA1_KEY)).thenReturn(true);
    when(attributes.get(StorageFileItem.DIGEST_SHA1_KEY)).thenReturn("0987654321"); // item hash is
    // "0987654321"

    final StorageFileItemRepresentation representation =
        (StorageFileItemRepresentation) underTest.renderStorageFileItem(request, fileItem);

    assertThat(representation.getStorageItem(), sameInstance(fileItem));
  }
}
