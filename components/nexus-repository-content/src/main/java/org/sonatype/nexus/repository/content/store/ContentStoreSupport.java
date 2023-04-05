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
package org.sonatype.nexus.repository.content.store;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.sonatype.nexus.common.property.SystemPropertiesHelper;
import org.sonatype.nexus.datastore.TransactionalStoreSupport;
import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.inject.TypeLiteral;
import org.eclipse.sisu.inject.TypeArguments;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * Support class for transactional domain stores backed by a content data store.
 *
 * @since 3.21
 */
public abstract class ContentStoreSupport<T extends ContentDataAccess>
    extends TransactionalStoreSupport
{
  private static final int DELETE_BATCH_SIZE_DEFAULT =
      SystemPropertiesHelper.getInteger("nexus.content.deleteBatchSize", 1000);

  private final Class<T> daoClass;

  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected ContentStoreSupport(final DataSessionSupplier sessionSupplier, final String contentStoreName) {
    super(sessionSupplier, contentStoreName);

    // use generic type information to discover the DAO class from the concrete implementation
    TypeLiteral<?> superType = TypeLiteral.get(getClass()).getSupertype(ContentStoreSupport.class);
    this.daoClass = (Class) TypeArguments.get(superType, 0).getRawType();
  }

  // alternative constructor that overrides discovery of the DAO class
  protected ContentStoreSupport(final DataSessionSupplier sessionSupplier,
                                final String contentStoreName,
                                final Class<T> daoClass)
  {
    super(sessionSupplier, contentStoreName);
    this.daoClass = checkNotNull(daoClass);
  }

  protected DataSession<?> thisSession() {
    return UnitOfWork.currentSession();
  }

  protected T dao() {
    return thisSession().access(daoClass);
  }

  /**
   * Commits any batched changes so far.
   *
   * Also checks to see if the current (potentially long-running) operation has been cancelled.
   */
  protected void commitChangesSoFar() {
    Transaction tx = UnitOfWork.currentTx();
    tx.commit();
    tx.begin();
    checkCancellation();
  }

  protected int deleteBatchSize() {
    return DELETE_BATCH_SIZE_DEFAULT;
  }

  /**
   * Helper to find content in this store before creating it with the given supplier.
   * Automatically retries the operation if another thread creates it just before us.
   */
  @Transactional(retryOn = DuplicateKeyException.class)
  public <D> D getOrCreate(final Supplier<Optional<D>> find, final Supplier<D> create) {
    return find.get().orElseGet(create);
  }

  public <D> D save(
      final Supplier<Optional<D>> find,
      final Supplier<D> create,
      final UnaryOperator<D> update,
      final Consumer<D> postTransaction)
  {
    D result = transactionalSave(find, create, update);
    postTransaction.accept(result);
    return result;
  }

  /**
   * Helper to find content in this store before creating or updating it with the given suppliers.
   *
   * @since 3.30
   */
  @Transactional(retryOn = DuplicateKeyException.class)
  protected  <D> D transactionalSave(final Supplier<Optional<D>> find, final Supplier<D> create, final UnaryOperator<D> update) {
    return find.get().map(update).orElseGet(create);
  }
}
