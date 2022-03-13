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
package org.sonatype.nexus.repository.internal.blobstore;

import java.util.Arrays;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.internal.blobstore.BlobStoreOverrideImpl.NEXUS_BLOB_STORE_OVERRIDE;

/**
 * Test {@code BlobStoreOverrideImpl} with various override strings in a situation where only the default blob store
 * exists and the override string is not expected to trigger an update the the blob store configuration.
 */
@RunWith(Parameterized.class)
public class BlobStoreOverrideImplParsingTest
    extends BlobStoreOverrideImplTestSupport
{
  @Parameter
  public String envString;

  @Parameters(name = "NEXUS_BLOB_STORE_OVERRIDE: {0}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {null},
        {"{}"},
        {""},
        {"null"},
        {"{\"default\":{\"file\":{\"path\":\"default\"}}}"},
        {"{\"default\":{\"file\":{}}}"},
        {"{\"default\":{\"file\":null}}"},
        {"{\"default\":{}}"},
        {"{\"default\":null}"},
        {"{\"wrong name\":{\"file\":{\"path\":\"other_path\"}}}"},
        {"{\"default\":{\"s3\":{\"path\":\"default\"}}}"},
        {"{\"one\":null, \"two\":null}"},
        {"{\"one\":{}, \"two\":{}}"},
    });
  }

  @Test
  public void testParseWithNoUpdate() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE, envString);
    BlobStoreConfiguration config = defaultConfig();
    when(configStore.list()).thenReturn(singletonList(config));
    underTest.apply();
    verify(configStore, never()).update(any());
  }
}
