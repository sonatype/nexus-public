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
/**
 *
 */

package org.sonatype.nexus.yum.internal.rest;

import java.util.Collection;
import java.util.Map;

import org.sonatype.nexus.plugins.capabilities.CapabilityContext;
import org.sonatype.nexus.plugins.capabilities.CapabilityIdentity;
import org.sonatype.nexus.plugins.capabilities.CapabilityReference;
import org.sonatype.nexus.plugins.capabilities.CapabilityRegistry;
import org.sonatype.nexus.yum.YumHosted;
import org.sonatype.nexus.yum.YumRegistry;
import org.sonatype.nexus.yum.internal.capabilities.GenerateMetadataCapability;
import org.sonatype.nexus.yum.internal.capabilities.GenerateMetadataCapabilityConfiguration;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.restlet.data.Method.GET;
import static org.restlet.data.Method.POST;
import static org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST;
import static org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND;
import static org.sonatype.nexus.yum.internal.rest.AliasResource.RESOURCE_URI;

/**
 * @author BVoss
 */
public class AliasResourceTest
    extends TestSupport
{

  private static final String EXISTING_ALIAS = "trunk";

  private static final String RELEASES = "releases";

  private static final String TRUNK_VERSION = "5.1.15-2";

  private static final String VERSION_TO_CREATE = "new-version";

  private static final String ALIAS_TO_CREATE = "alias-to-create";

  private PlexusResource underTest;

  @Mock
  private YumRegistry yumRegistry;

  @Mock
  private CapabilityRegistry capabilityRegistry;

  @Before
  public void prepareResource() {
    underTest = new AliasResource(yumRegistry, capabilityRegistry);
    final YumHosted yum = mock(YumHosted.class);
    when(yumRegistry.get(RELEASES)).thenReturn(yum);
    when(yum.getVersion(EXISTING_ALIAS)).thenReturn(TRUNK_VERSION);
    when(yum.getVersion(ALIAS_TO_CREATE)).thenReturn(VERSION_TO_CREATE);

    final CapabilityReference reference = mock(CapabilityReference.class);
    final Collection<CapabilityReference> references = Lists.newArrayList();
    references.add(reference);
    final GenerateMetadataCapability yumRepositoryCapability = mock(GenerateMetadataCapability.class);
    final CapabilityContext capabilityContext = mock(CapabilityContext.class);
    when(reference.context()).thenReturn(capabilityContext);
    when(capabilityContext.id()).thenReturn(CapabilityIdentity.capabilityIdentity("ID"));
    when(capabilityContext.isEnabled()).thenReturn(true);
    when(capabilityContext.notes()).thenReturn("Notes");
    when(reference.capabilityAs(GenerateMetadataCapability.class)).thenReturn(yumRepositoryCapability);
    Map<String, String> aliases = Maps.newHashMap();
    aliases.put("foo", "bar");
    when(yumRepositoryCapability.getConfig()).thenReturn(
        new GenerateMetadataCapabilityConfiguration(RELEASES, aliases, true, 1, "/comps.xml")
    );

    doReturn(references).when(capabilityRegistry).get(Mockito.<Predicate<CapabilityReference>>any());
  }

  @Test
  public void getShouldReturn400ForMissingRepositoryId()
      throws Exception
  {
    assert400OnGet(createRequest(null, "bla"), "Repository Id must be specified");
  }

  @Test
  public void getShouldReturn400ForMissingAlias()
      throws Exception
  {
    assert400OnGet(createRequest(RELEASES, null), "Alias must be specified");
  }

  @Test
  public void getShouldReturn404ForInexistentRepository()
      throws Exception
  {
    assert404OnGet(createRequest("foo", EXISTING_ALIAS));
  }

  @Test
  public void getShouldReturn404ForInexistentAlias()
      throws Exception
  {
    assert404OnGet(createRequest(RELEASES, "foo"));
  }

  @Test
  public void versionReturnedForExistentAlias()
      throws Exception
  {
    final Request request = createRequest(RELEASES, EXISTING_ALIAS);
    final StringRepresentation version = (StringRepresentation) underTest.get(null, request, null, null);
    assertThat(version.getText(), Matchers.is(TRUNK_VERSION));
  }

  @Test
  public void shouldRetrieveRestRequirements()
      throws Exception
  {
    assertThat(underTest.getResourceUri(), is(RESOURCE_URI));
    assertThat(underTest.getPayloadInstance(), nullValue());
    assertThat(underTest.getPayloadInstance(GET), nullValue());
    assertThat(underTest.getPayloadInstance(POST), instanceOf(String.class));
  }

  @Test
  public void postShouldReturn400ForMissingRepositoryId()
      throws Exception
  {
    assert400OnPost(null, EXISTING_ALIAS, VERSION_TO_CREATE);
  }

  @Test
  public void postShouldReturn400ForMissingAlias()
      throws Exception
  {
    assert400OnPost(RELEASES, null, VERSION_TO_CREATE);
  }

  @Test
  public void postShouldReturn404ForInexistentRepositoryId()
      throws Exception
  {
    assert404OnPost("foo", EXISTING_ALIAS, VERSION_TO_CREATE);
  }

  @Test
  public void postShouldReturn400ForEmptyPayload()
      throws Exception
  {
    assert400OnPost(RELEASES, EXISTING_ALIAS, null);
  }

  @Test
  public void postShouldReturn400ForObjectPayload()
      throws Exception
  {
    assert400OnPost(RELEASES, EXISTING_ALIAS, new Object());
  }

  @Test
  public void shouldSetVersion()
      throws Exception
  {
    final Request request = createRequest(RELEASES, ALIAS_TO_CREATE);
    StringRepresentation result = (StringRepresentation) underTest.post(null, request, null, VERSION_TO_CREATE);
    assertThat(result.getText(), is(VERSION_TO_CREATE));
    result = (StringRepresentation) underTest.get(null, request, null, null);
    assertThat(result.getText(), is(VERSION_TO_CREATE));

    final ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);

    verify(capabilityRegistry).update(
        Mockito.eq(CapabilityIdentity.capabilityIdentity("ID")),
        Mockito.eq(true),
        Mockito.eq("Notes"),
        mapCaptor.capture()
    );

    final Map<String, String> actualMap = mapCaptor.getValue();
    assertThat(actualMap.get(GenerateMetadataCapabilityConfiguration.REPOSITORY_ID), is(RELEASES));
    assertThat(actualMap.get(GenerateMetadataCapabilityConfiguration.ALIASES), is(
        ALIAS_TO_CREATE + "=" + VERSION_TO_CREATE + ",foo=bar")
    );
    assertThat(actualMap.get(GenerateMetadataCapabilityConfiguration.DELETE_PROCESSING), is("true"));
    assertThat(actualMap.get(GenerateMetadataCapabilityConfiguration.DELETE_PROCESSING_DELAY), is("1"));
    assertThat(actualMap.get(GenerateMetadataCapabilityConfiguration.YUM_GROUPS_DEFINITION_FILE), is("/comps.xml"));
  }

  private void assert400OnPost(final String repositoryId, final String alias, final Object payload) {
    try {
      underTest.post(null, createRequest(repositoryId, alias), null, payload);
      Assert.fail();
    }
    catch (ResourceException e) {
      assertThat(e.getStatus(), is(CLIENT_ERROR_BAD_REQUEST));
    }
  }

  private void assert404OnPost(final String repositoryId, final String alias, final Object payload) {
    try {
      underTest.post(null, createRequest(repositoryId, alias), null, payload);
      Assert.fail();
    }
    catch (ResourceException e) {
      assertThat(e.getStatus(), is(CLIENT_ERROR_NOT_FOUND));
    }
  }

  private void assert404OnGet(final Request request) {
    try {
      underTest.get(null, request, null, null);
      Assert.fail(ResourceException.class + " expected");
    }
    catch (ResourceException e) {
      assertThat(e.getStatus(), is(Status.CLIENT_ERROR_NOT_FOUND));
    }
  }

  private void assert400OnGet(final Request request, final String message) {
    try {
      underTest.get(null, request, null, null);
      Assert.fail(ResourceException.class + " expected");
    }
    catch (ResourceException e) {
      assertThat(e.getStatus(), is(Status.CLIENT_ERROR_BAD_REQUEST));
      assertThat(e.getMessage(), is(message));
    }
  }

  private Request createRequest(final String repoValue, final String aliasValue) {
    final Request request = new Request();
    request.getAttributes().put(AliasResource.REPOSITORY_ID_PARAM, repoValue);
    request.getAttributes().put(AliasResource.ALIAS_PARAM, aliasValue);
    return request;
  }

}
