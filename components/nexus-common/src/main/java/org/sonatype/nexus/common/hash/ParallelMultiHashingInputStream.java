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
package org.sonatype.nexus.common.hash;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.hash.Hasher;

/**
 * An {@link MultiHashingInputStream} which uses the {@link ForkJoinPool} commonPool to asynchronously compute hashes
 *
 * @see MultiHashingInputStream
 */
public class ParallelMultiHashingInputStream
    extends MultiHashingInputStream
{
  private List<ForkJoinTask<?>> hashingFutures = Collections.emptyList();

  public ParallelMultiHashingInputStream(final Iterable<HashAlgorithm> algorithms, final InputStream inputStream) {
    super(algorithms, inputStream);
  }

  @Override
  protected void submitHashing(final Consumer<Hasher> runnable) {
    hashingFutures = hashers.values()
        .stream()
        .map(hasher -> ForkJoinPool.commonPool().submit(() -> runnable.accept(hasher)))
        .collect(Collectors.toList());
  }

  @Override
  protected void waitForHashes() throws IOException {
    for (ForkJoinTask<?> future : hashingFutures) {
      if (!future.isDone() && !future.isCancelled()) {
        try {
          future.get();
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        catch (ExecutionException e) {
          throw new IOException(e);
        }
      }
    }
  }
}
