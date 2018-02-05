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
package org.sonatype.nexus.repository.transaction;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.sonatype.nexus.repository.storage.MissingBlobException;
import org.sonatype.nexus.transaction.Operations;
import org.sonatype.nexus.transaction.Transactional;

import com.orientechnologies.common.concur.ONeedRetryException;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates the method will delete blobs; while doing this it may touch other blobs.
 *
 * @since 3.2
 */
@Transactional(retryOn = { ONeedRetryException.class, MissingBlobException.class })
@Target(METHOD)
@Retention(RUNTIME)
public @interface TransactionalDeleteBlob
{
  /**
   * Helper to apply this transactional behaviour to lambdas.
   */
  Operations<RuntimeException, ?> operation = Transactional.operation.stereotype(TransactionalDeleteBlob.class);
}
