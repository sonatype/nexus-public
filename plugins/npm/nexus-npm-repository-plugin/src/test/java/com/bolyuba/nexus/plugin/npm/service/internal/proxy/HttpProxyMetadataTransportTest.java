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
package com.bolyuba.nexus.plugin.npm.service.internal.proxy;

import org.sonatype.nexus.proxy.storage.remote.httpclient.HttpClientManager;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.bolyuba.nexus.plugin.npm.service.internal.MetadataParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class HttpProxyMetadataTransportTest
    extends TestSupport
{

  @Mock
  MetadataParser metadataParser;

  @Mock
  HttpClientManager httpClientManager;

  HttpProxyMetadataTransport underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new HttpProxyMetadataTransport(metadataParser, httpClientManager);
  }

  @Test
  public void testEncodePackageName_noScope() {
    assertThat(underTest.encodePackageName("angular-forms"), is("angular-forms"));
  }

  @Test
  public void testEncodePackageName_WithScope() {
    assertThat(underTest.encodePackageName("@angular/forms"), is("@angular%2Fforms"));
  }

}
