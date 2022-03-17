/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.group.NpmGroupRepository;
import com.bolyuba.nexus.plugin.npm.service.Generator;
import com.bolyuba.nexus.plugin.npm.service.PackageRequest;
import com.bolyuba.nexus.plugin.npm.service.PackageRoot;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class GroupMetadataServiceImplTest
    extends TestSupport
{

  @Mock
  PackageRequest packageRequest;

  @Mock
  NpmGroupRepository npmGroupRepository;

  @Mock
  MetadataParser metadataParser;

  @Mock
  Repository repositoryMember1;

  @Mock
  NpmRepository npmRepositoryMember1;

  @Mock
  Generator memberGenerator1;

  PackageRoot packageRoot1 = new PackageRoot("hosted", new HashMap<String, Object>()
  {{
    put("name", "test");
    put("versions", new HashMap<>());
    put("dist-tags", new HashMap<String, Object>()
    {{
      put("latest", "1.1.0");
    }});
  }});

  @Mock
  Repository repositoryMember2;

  @Mock
  NpmRepository npmRepositoryMember2;

  @Mock
  Generator memberGenerator2;

  PackageRoot packageRoot2 = new PackageRoot("proxy", new HashMap<String, Object>()
  {{
    put("name", "test");
    put("versions", new HashMap<>());
    put("dist-tags", new HashMap<String, Object>()
    {{
      put("latest", "1.0.0");
    }});
  }});

  GroupMetadataServiceImpl underTest;

  @Before
  public void setup() throws IOException {
    when(npmGroupRepository.getMemberRepositories()).thenReturn(asList(repositoryMember1, repositoryMember2));
    when(npmGroupRepository.getId()).thenReturn("group");

    when(repositoryMember1.adaptToFacet(NpmRepository.class)).thenReturn(npmRepositoryMember1);
    when(npmRepositoryMember1.getMetadataService()).thenReturn(memberGenerator1);
    when(memberGenerator1.generatePackageRoot(packageRequest)).thenReturn(packageRoot1);

    when(repositoryMember2.adaptToFacet(NpmRepository.class)).thenReturn(npmRepositoryMember2);
    when(npmRepositoryMember2.getMetadataService()).thenReturn(memberGenerator2);
    when(memberGenerator2.generatePackageRoot(packageRequest)).thenReturn(packageRoot2);
  }

  public void initializeUnderTest(Boolean mergeMetadata) throws Exception {
    System.setProperty("nexus.npm.mergeGroupMetadata", mergeMetadata.toString());
    underTest = new GroupMetadataServiceImpl(npmGroupRepository, metadataParser);
  }

  @After
  public void clearMergeMetadataProperty() {
    System.clearProperty("nexus.npm.mergeGroupMetadata");
  }

  /**
   * It should only return the PackageRoot found from the first group repository, since merge metadata is false and
   * the request is not scoped.
   */
  @Test
  public void testDoGeneratePackageRoot_notScoped_mergeMetadataFalse() throws Exception {
    initializeUnderTest(false);
    PackageRoot actualPackageRoot = underTest.doGeneratePackageRoot(packageRequest);

    assertThat(actualPackageRoot, is(packageRoot1));
  }

  /**
   * It should return a merged package root when the request is not scoped but mergeMetadata is true
   */
  @Test
  public void testDoGeneratePackageRoot_notScoped_mergeMetadataTrue() throws Exception {
    initializeUnderTest(true);
    PackageRoot actualPackageRoot = underTest.doGeneratePackageRoot(packageRequest);

    Map<String, Object> distTags = (Map) actualPackageRoot.getRaw().get("dist-tags");
    assertThat((String) distTags.get("latest"), Matchers.is("1.1.0"));
  }

  /**
   * It should return a merged package root when mergeMetadata is false but the request is scoped
   */
  @Test
  public void testDoGeneratePackageRoot_Scoped_mergeMetadataFalse() throws Exception {
    initializeUnderTest(false);
    when(packageRequest.isScoped()).thenReturn(true);
    PackageRoot actualPackageRoot = underTest.doGeneratePackageRoot(packageRequest);

    Map<String, Object> distTags = (Map) actualPackageRoot.getRaw().get("dist-tags");
    assertThat((String) distTags.get("latest"), Matchers.is("1.1.0"));
  }

}
