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
package org.sonatype.nexus.repository.storage;

import java.io.InputStream;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Supplier;

/**
 * Storage {@link Facet}, providing component and asset storage for a repository.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface StorageFacet
    extends Facet
{

  /**
   * Registers format specific selector for {@link WritePolicy}. If not set, the {@link
   * WritePolicySelector#DEFAULT} is used which returns the configured write policy.
   */
  void registerWritePolicySelector(WritePolicySelector writePolicySelector);

  /**
   * Supplies transactions for use in {@link UnitOfWork}.
   */
  Supplier<StorageTx> txSupplier();

  /**
   * Creates a new temporary blob based using the contents of the input stream. Disposal of the temp blob must be
   * managed by the caller, typically using a try-with-resources block.
   *
   * @since 3.1
   */
  TempBlob createTempBlob(InputStream inputStream, Iterable<HashAlgorithm> hashAlgorithms);

  /**
   * Creates a new temporary blob based using the contents of the payload. Disposal of the temp blob must be
   * managed by the caller, typically using a try-with-resources block.
   *
   * @since 3.1
   */
  TempBlob createTempBlob(Payload payload, Iterable<HashAlgorithm> hashAlgorithms);

}
