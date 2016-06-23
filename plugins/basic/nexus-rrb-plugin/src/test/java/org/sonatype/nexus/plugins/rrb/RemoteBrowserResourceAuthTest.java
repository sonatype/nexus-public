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
package org.sonatype.nexus.plugins.rrb;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;

import org.sonatype.nexus.AbstractPluginTestCase;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.UsernamePasswordRemoteAuthenticationSettings;
import org.sonatype.nexus.templates.TemplateProvider;
import org.sonatype.nexus.templates.repository.DefaultRepositoryTemplateProvider;
import org.sonatype.nexus.templates.repository.maven.Maven2ProxyRepositoryTemplate;
import org.sonatype.nexus.test.http.RemoteRepositories;
import org.sonatype.nexus.test.http.RemoteRepositories.AuthInfo;
import org.sonatype.nexus.test.http.RemoteRepositories.RemoteRepository;
import org.sonatype.plexus.rest.resource.PlexusResource;

import com.google.common.collect.ImmutableMap;
import junit.framework.Assert;
import org.codehaus.plexus.context.Context;
import org.junit.Test;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.data.Request;

public class RemoteBrowserResourceAuthTest
    extends AbstractPluginTestCase
{
  private RemoteRepositories remoteRepositories = null;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();
    HashMap<String, Object> users = new HashMap<>();
    this.remoteRepositories = RemoteRepositories.builder()
        .repo(RemoteRepository.repo("auth-test").resourceBase("target")
            .authInfo(new AuthInfo("BASIC", ImmutableMap.of("admin", "admin"))).build())
        .build().start();

    // ping nexus to wake up
    startNx();
  }

  @Test
  public void testSiteWithAuth()
      throws Exception
  {
    String remoteUrl = remoteRepositories.getUrl("auth-test");

    String repoId = "testSiteWithAuth";
    RepositoryRegistry repoRegistry = this.lookup(RepositoryRegistry.class);

    TemplateProvider templateProvider =
        this.lookup(TemplateProvider.class, DefaultRepositoryTemplateProvider.PROVIDER_ID);
    Maven2ProxyRepositoryTemplate template =
        (Maven2ProxyRepositoryTemplate) templateProvider.getTemplateById("default_proxy_release");
    template.getCoreConfiguration().getConfiguration(true).setId(repoId);
    template.getCoreConfiguration().getConfiguration(true).setName(repoId + "-name");
    template.getCoreConfiguration().getConfiguration(true).setIndexable(false); // disable index
    template.getCoreConfiguration().getConfiguration(true).setSearchable(false); // disable index

    M2Repository m2Repo = (M2Repository) template.create();
    repoRegistry.addRepository(m2Repo);

    m2Repo.setRemoteUrl(remoteUrl);
    m2Repo.setRemoteAuthenticationSettings(new UsernamePasswordRemoteAuthenticationSettings("admin", "admin"));
    m2Repo.commitChanges();

    Reference rootRef = new Reference("http://localhost:8081/nexus/service/local/repositories/" + repoId + "");
    Reference resourceRef =
        new Reference(rootRef, "http://localhost:8081/nexus/service/local/repositories/" + repoId + "/");

    // now call the REST resource
    Request request = new Request();
    request.setRootRef(new Reference("http://localhost:8081/nexus/"));
    request.setOriginalRef(rootRef);
    request.setResourceRef(resourceRef);
    request.getAttributes().put("repositoryId", repoId);
    Form form = new Form();
    form.add("Accept", "application/json");
    form.add("Referer", "http://localhost:8081/nexus/index.html#view-repositories;" + repoId);
    form.add("Host", " localhost:8081");
    request.getAttributes().put("org.restlet.http.headers", form);

    PlexusResource plexusResource = this.lookup(PlexusResource.class, RemoteBrowserResource.class.getName());
    String jsonString = plexusResource.get(null, request, null, null).toString();

    // TODO: do some better validation then this
    Assert.assertTrue(jsonString.contains("/classes/"));
    Assert.assertTrue(jsonString.contains("/test-classes/"));

  }

  @Override
  protected void tearDown()
      throws Exception
  {
    if (remoteRepositories != null) {
      remoteRepositories.stop();
    }
    super.tearDown();
  }
}
