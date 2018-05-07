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
package org.sonatype.nexus.repository.maven.internal.filter;

import org.sonatype.nexus.common.filter.ScalableBloomFilter;

import org.apache.maven.index.reader.Record;

import static com.google.common.hash.Funnels.stringFunnel;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonatype.nexus.repository.maven.internal.utils.RecordUtils.gavceForRecord;

/**
 * Filters Maven records based on unique GAV-CE using a scalable bloom filter. This reduces the amount of memory
 * required to generate the maven indexes significantly but still has an upper limit (i.e. it won't use the disk if we
 * run out of memory) and is probabilistic in that there is a very small chance that a false positive will be returned
 * from maybeContains resulting in a recording being missed from the index.
 *
 * If records are missed and the dataset is suitably large then recommend switching to
 * {@link DiskBackedDuplicateDetectionStrategy}
 *
 * @since 3.11
 */
public class BloomFilterDuplicateDetectionStrategy
    implements DuplicateDetectionStrategy<Record>
{
  private static double MAX_PROBABILITY = 10e-17;

  // Bloom filter size of 1,000,000 with a probability of 0.01% (0.00001) uses 2.857 MB)
  // As we scale we will get a filter each time the number of elements exceeds NUMBER_OF_ELEMENTS_BEFORE_NEW_FILTER
  // i.e. 100,000. Therefore for 1m entries we will use ~28MB.
  // More info on memory usage can be found here https://github.com/google/guava/issues/2520#issuecomment-231233736
  private static int BLOOM_FILTER_SIZE = 1000000;

  private final ScalableBloomFilter<String> bloomFilter =
      new ScalableBloomFilter<>(stringFunnel(UTF_8), BLOOM_FILTER_SIZE, MAX_PROBABILITY);

  @Override
  public boolean apply(final Record record) {
    return bloomFilter.put(gavceForRecord(record));
  }
}
