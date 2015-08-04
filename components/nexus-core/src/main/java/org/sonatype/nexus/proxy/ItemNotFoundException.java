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
package org.sonatype.nexus.proxy;

import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.router.RepositoryRouter;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.sisu.goodies.common.FormatTemplate;
import org.sonatype.sisu.goodies.common.SimpleFormat;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Thrown if the requested item is not found.
 *
 * @author cstamas
 */
public class ItemNotFoundException
    extends Exception
{
  private static final long serialVersionUID = -4964273361722823796L;

  // ==

  /**
   * Creates a new instance of {@link ItemNotFoundReason}.
   *
   * @param request The request that causes {@link ItemNotFoundException}.
   * @param message The reasoning message template (use {@code %s} as place holder). See {@link SimpleFormat}.
   * @param params  The parameters to fill place holders in message with content. See {@link SimpleFormat}.
   * @return the newly created reason.
   * @since 2.5
   */
  public static ItemNotFoundReason reasonFor(final ResourceStoreRequest request, final String message,
                                             final Object... params)
  {
    return new ItemNotFoundReason(SimpleFormat.template(message, params), request);
  }

  /**
   * Creates a new instance of {@link ItemNotFoundInRepositoryReason}.
   *
   * @param request    The request that causes {@link ItemNotFoundException}.
   * @param repository The repository within {@link ItemNotFoundException} is to be thrown.
   * @param message    The reasoning message template (use {@code %s} as place holder). See {@link SimpleFormat}.
   * @param params     The parameters to fill place holders in message with content. See {@link SimpleFormat}.
   * @return the newly created reason.
   * @since 2.5
   */
  public static ItemNotFoundInRepositoryReason reasonFor(final ResourceStoreRequest request,
                                                         final Repository repository, final String message,
                                                         final Object... params)
  {
    return new ItemNotFoundInRepositoryReason(SimpleFormat.template(message, params), request, repository);
  }

  // ==

  /**
   * Legacy support. Not to be used in any current code!
   *
   * @return reason.
   * @since 2.5
   * @deprecated Used for legacy support, new code should NOT use this method. See other methods:
   *             {@link #reasonFor(ResourceStoreRequest, String, Object...)} and
   *             {@link #reasonFor(ResourceStoreRequest, Repository, String, Object...)}
   */
  @Deprecated
  private static ItemNotFoundReason legacySupport(final String message, final ResourceStoreRequest request,
                                                  final Repository repository)
  {
    if (repository != null) {
      return new ItemNotFoundInRepositoryReason(SimpleFormat.template(message), request, repository);
    }
    else {
      return new ItemNotFoundReason(SimpleFormat.template(message), request);
    }
  }

  // ==

  /**
   * Reason of item not found when no repository is involved. Usually ther IS one, so you should use
   * {@link ItemNotFoundInRepositoryReason} instead. This one is used in places like {@link RepositoryRouter}, where
   * the "targeted" repository is still unknown or similar places.
   *
   * @since 2.5
   */
  public static class ItemNotFoundReason
  {
    private final FormatTemplate message;

    private final ResourceStoreRequest resourceStoreRequest;

    /**
     * @param message              reason message (might not be {@code null}).
     * @param resourceStoreRequest request (might not be {@code null}).
     */
    public ItemNotFoundReason(final FormatTemplate message, final ResourceStoreRequest resourceStoreRequest) {
      this.message = checkNotNull(message);
      this.resourceStoreRequest = checkNotNull(resourceStoreRequest).cloneAndDetach();
    }

    /**
     * Returns the reason message, never {@code null}.
     *
     * @return the reason message, never {@code null}.
     */
    public String getMessage() {
      return message.toString();
    }

    /**
     * Returns the request (originals detached clone, see {@link ResourceStoreRequest#cloneAndDetach()} method)
     * that
     * resulted in {@link ItemNotFoundException}.
     *
     * @return the request that resulted in {@link ItemNotFoundException}, never {@code null}.
     */
    public ResourceStoreRequest getResourceStoreRequest() {
      return resourceStoreRequest;
    }
  }

  /**
   * Reason of item not found that is triggered within a {@link Repository} instance.
   *
   * @since 2.5
   */
  public static class ItemNotFoundInRepositoryReason
      extends ItemNotFoundReason
  {
    private final Repository repository;

    /**
     * @param message              reason message (might not be {@code null}).
     * @param resourceStoreRequest request (might not be {@code null}).
     * @param repository           repository (might not be {@code null}).
     */
    public ItemNotFoundInRepositoryReason(final FormatTemplate message,
                                          final ResourceStoreRequest resourceStoreRequest,
                                          final Repository repository)
    {
      super(message, resourceStoreRequest);
      this.repository = checkNotNull(repository);
    }

    /**
     * Returns the involved {@link Repository} instance.
     *
     * @return the repository in which {@link ItemNotFoundException} occurred.
     */
    public Repository getRepository() {
      return repository;
    }
  }

  private final ItemNotFoundReason reason;

  /**
   * Constructor with reason.
   *
   * @param reason (might not be {@code null}).
   * @throws NullPointerException if passed in reason parameter is {@code null}.
   * @since 2.5
   */
  public ItemNotFoundException(final ItemNotFoundReason reason) {
    this(reason, null);
  }

  /**
   * Constructor with reason and cause.
   *
   * @param reason (might not be {@code null}).
   * @throws NullPointerException if passed in reason parameter is {@code null}.
   * @since 2.5
   */
  public ItemNotFoundException(final ItemNotFoundReason reason, final Throwable cause) {
    super(reason.getMessage(), cause);
    this.reason = reason;
  }

  /**
   * Returns the reason of the item not found exception (never {@code null}).
   *
   * @return the reason, never {@code null}.
   * @since 2.5
   */
  public ItemNotFoundReason getReason() {
    return reason;
  }

  // == Deprecated stuff below

  /**
   * Constructor. To be used in places where no Repository exists yet in context (like in a Router).
   *
   * @deprecated Use constructor with {@link ItemNotFoundReason} instead.
   */
  @Deprecated
  public ItemNotFoundException(final ResourceStoreRequest request) {
    this(request, null, null);
  }

  /**
   * Constructor. To be used in places whenever there IS a Repository in context.
   *
   * @deprecated Use constructor with {@link ItemNotFoundReason} instead.
   */
  @Deprecated
  public ItemNotFoundException(final ResourceStoreRequest request, final Repository repository) {
    this(request, repository, null);
  }

  /**
   * Constructor. To be used in places whenever there IS a Repository in context.
   *
   * @deprecated Use constructor with {@link ItemNotFoundReason} instead.
   */
  @Deprecated
  public ItemNotFoundException(final ResourceStoreRequest request, final Repository repository, final Throwable cause) {
    this(repository != null ? "Item not found for request \"" + String.valueOf(request) + "\" in repository \""
        + RepositoryStringUtils.getHumanizedNameString(repository) + "\"!" : "Item not found for request \""
        + String.valueOf(request) + "\"!", request, repository, cause);
  }

  /**
   * Protected constructor, to be used by this class and subclass constructors.
   *
   * @deprecated Use constructor with {@link ItemNotFoundReason} instead.
   */
  @Deprecated
  private ItemNotFoundException(final String message, final ResourceStoreRequest request,
                                final Repository repository, final Throwable cause)
  {
    this(legacySupport(message, request, repository), cause);
  }

}
