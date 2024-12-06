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
package org.sonatype.nexus.blobstore.s3.internal;

import org.junit.Before;
import org.junit.Test;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreException;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

public class ParallelCopierTest
    extends TestSupport
{
  private ParallelCopier copier;

  @Mock
  private AmazonS3 s3;

  @Mock
  private InitiateMultipartUploadResult initiateMultipartUploadResult;

  @Before
  public void setUp() {
    when(initiateMultipartUploadResult.getUploadId()).thenReturn("uploadId");
    copier = new ParallelCopier(100, 4);
  }

  @Test
  public void testCalcFirstAndLastBytesProperly() {
    assertThat(ParallelCopier.getFirstByte(1, 500), is(0L));
    assertThat(ParallelCopier.getLastByte(1700, 1, 500), is(499L));
    assertThat(ParallelCopier.getFirstByte(2, 500), is(500L));
    assertThat(ParallelCopier.getLastByte(1700, 2, 500), is(999L));
    assertThat(ParallelCopier.getFirstByte(3, 500), is(1000L));
    assertThat(ParallelCopier.getLastByte(1700, 3, 500), is(1499L));
    assertThat(ParallelCopier.getFirstByte(4, 500), is(1500L));
    assertThat(ParallelCopier.getLastByte(1700, 4, 500), is(1699L));
    assertThat(ParallelCopier.getFirstByte(5, 500), is(2000L));
    assertThat(ParallelCopier.getLastByte(1700, 5, 500), is(1699L));
  }

  @Test
  public void testCopyWithMultipartApi() {
    when(s3.initiateMultipartUpload(any())).thenReturn(initiateMultipartUploadResult);
    when(s3.getObjectMetadata("bucketName", "source")).thenReturn(new ObjectMetadata() {{
      setContentLength(101);
    }});
    when(s3.copyPart(any())).thenReturn(new CopyPartResult());

    copier.copy(s3, "bucketName", "source", "destination");

    verify(s3).initiateMultipartUpload(any());
    verify(s3).getObjectMetadata("bucketName", "source");
    verify(s3, times(2)).copyPart(any());
    verify(s3).completeMultipartUpload(any());
    verify(s3, never()).abortMultipartUpload(any());
  }

  @Test
  public void testCopyAbortsMultipartOnError() {
    when(s3.initiateMultipartUpload(any())).thenReturn(initiateMultipartUploadResult);
    when(s3.getObjectMetadata("bucketName", "source")).thenReturn(new ObjectMetadata() {{
      setContentLength(101);
    }});
    when(s3.copyPart(any())).thenThrow(new SdkClientException(""));

    assertThrows(BlobStoreException.class, () -> copier.copy(s3, "bucketName", "source", "destination"));

    verify(s3).initiateMultipartUpload(any());
    verify(s3).getObjectMetadata("bucketName", "source");
    verify(s3, atLeastOnce()).copyPart(any());
    verify(s3).abortMultipartUpload(any());
  }

  @Test
  public void testCopySplitsParts() {
    when(s3.initiateMultipartUpload(any())).thenReturn(initiateMultipartUploadResult);
    when(s3.getObjectMetadata("bucketName", "source")).thenReturn(new ObjectMetadata() {{
      setContentLength(345);
    }});
    when(s3.copyPart(any())).thenReturn(new CopyPartResult());

    copier.copy(s3, "bucketName", "source", "destination");

    verify(s3).initiateMultipartUpload(any());
    verify(s3).getObjectMetadata("bucketName", "source");
    verify(s3, times(4)).copyPart(any());
    verify(s3).completeMultipartUpload(any());
    verify(s3, never()).abortMultipartUpload(any());
  }

  @Test
  public void testCopyUsesCopyObjectForSmallCopies() {
    when(s3.getObjectMetadata("bucketName", "source")).thenReturn(new ObjectMetadata() {{
      setContentLength(99);
    }});

    copier.copy(s3, "bucketName", "source", "destination");

    verify(s3).getObjectMetadata("bucketName", "source");
    verify(s3).copyObject(any(), any(), any(), any());
    verify(s3, never()).initiateMultipartUpload(any());
  }
}
