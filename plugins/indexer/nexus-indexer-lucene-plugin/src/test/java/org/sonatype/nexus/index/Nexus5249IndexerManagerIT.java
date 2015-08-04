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
package org.sonatype.nexus.index;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.proxy.maven.MavenGroupRepository;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.apache.maven.index.ArtifactScanningListener;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.Scanner;
import org.apache.maven.index.ScanningRequest;
import org.apache.maven.index.ScanningResult;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdater;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Test for NEXUS-5249 and related ones (see linked issues). In general, we ensure that 404 happened during remote
 * update does not break the batch-processing of ALL repositories (task should not stop and should go on process other
 * repositories). Also, 401/403/50x errors will throw IOException at the processng end (and hence, make the task
 * failed), but again, the batch is not broken due to one repo being broken, the exceptions are supressed until batch
 * end.
 * <p>
 * {@link DefaultIndexerManager} "reindex" operation (invoked multiple times by "all" operations tested here) actually
 * does two things: first, if needed (repo is proxy, index download enabled), will try to update index from remote
 * (using {@link IndexUpdater#fetchAndUpdateIndex(IndexUpdateRequest)} component, and then, will invoke
 * {@link NexusIndexer#scan(IndexingContext, String, ArtifactScanningListener, boolean)}. This UT assumes this order of
 * invocation, and that following conditions are true:
 * <ul>
 * <li>if {@link IndexUpdater#fetchAndUpdateIndex(IndexUpdateRequest)} hits HTTP 404 response, no interruption of
 * execution happens at all (not for given repo being processed, nor for the "all" operation), this was the bug
 * NEXUS-5249</li>
 * <li>if if {@link IndexUpdater#fetchAndUpdateIndex(IndexUpdateRequest)} hits any other unexpected HTTP response other
 * than 404, no interruption of "all" operation happens (but currently processed repository is stopped from being
 * processed, no scan is invoked), and the "all" operation should fail at end</li>
 * <li></li>
 * </ul>
 *
 * @author cstamas
 */
public class Nexus5249IndexerManagerIT
    extends AbstractIndexerManagerTest
{
  protected int indexedRepositories;

  protected int indexedProxyRepositories;

  protected MavenProxyRepository failingRepository;

  protected FailingInvocationHandler fetchFailingInvocationHandler;

  protected CountingInvocationHandler fetchCountingInvocationHandler;

  protected int scanInvocationCount;

  protected void prepare(final IOException failure)
      throws Exception
  {
    // faking IndexUpdater that will fail with given exception for given "failing" repository
    final IndexUpdater realUpdater = lookup(IndexUpdater.class);
    // predicate to match invocation when the arguments are for the failingRepository
    final Predicate<Object[]> fetchAndUpdateIndexMethodArgumentsPredicate = new Predicate<Object[]>()
    {
      @Override
      public boolean apply(@Nullable Object[] input) {
        if (input != null) {
          final IndexUpdateRequest req = (IndexUpdateRequest) input[0];
          if (req != null) {
            return req.getIndexingContext().getId().startsWith(failingRepository.getId());
          }
        }
        return false;
      }
    };
    // method we want to fail and count
    final Method fetchAndUpdateIndexMethod =
        IndexUpdater.class.getMethod("fetchAndUpdateIndex", new Class[]{IndexUpdateRequest.class});
    fetchFailingInvocationHandler =
        new FailingInvocationHandler(new PassThruInvocationHandler(realUpdater), fetchAndUpdateIndexMethod,
            fetchAndUpdateIndexMethodArgumentsPredicate, failure);
    fetchCountingInvocationHandler =
        new CountingInvocationHandler(fetchFailingInvocationHandler, fetchAndUpdateIndexMethod);
    final IndexUpdater fakeUpdater =
        (IndexUpdater) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{IndexUpdater.class},
            fetchCountingInvocationHandler);

    final Scanner fakeScanner = new Scanner()
    {
      @Override
      public ScanningResult scan(ScanningRequest request) {
        scanInvocationCount++;
        return null;
      }
    };

    // applying faked components
    final DefaultIndexerManager dim = (DefaultIndexerManager) indexerManager;
    dim.setIndexUpdater(fakeUpdater);
    dim.setScanner(fakeScanner);

    // count total of indexed, and total of indexed proxy repositories, and set the one to make it really go
    // remotely
    indexedRepositories = 0;
    indexedProxyRepositories = 0;
    for (Repository repository : repositoryRegistry.getRepositories()) {
      if (!repository.getRepositoryKind().isFacetAvailable(ShadowRepository.class)
          && !repository.getRepositoryKind().isFacetAvailable(GroupRepository.class)
          && repository.getRepositoryKind().isFacetAvailable(MavenRepository.class) && repository.isIndexable()) {
        indexedRepositories++;
      }
      // leave only one to download remote indexes explicitly
      if (repository.getRepositoryKind().isFacetAvailable(MavenProxyRepository.class)) {
        final MavenProxyRepository mavenProxyRepository = repository.adaptToFacet(MavenProxyRepository.class);
        if (repository.getId().equals(central.getId())) {
          mavenProxyRepository.setDownloadRemoteIndexes(true);
          failingRepository = mavenProxyRepository;
          indexedProxyRepositories++;
        }
        else {
          mavenProxyRepository.setDownloadRemoteIndexes(false);
        }
        ((M2Repository)mavenProxyRepository).commitChanges();
      }
    }

    // as things above will trigger some bg tasks (failingRepository will be reindexed with a task)
    waitForTasksToStop();
    wairForAsyncEventsToCalmDown();

    // reset counters
    fetchCountingInvocationHandler.reset();
    scanInvocationCount = 0;
  }

  @Test
  public void remote404ResponseDoesNotFailsProcessing()
      throws Exception
  {
    // HTTP 404 pops up as FileNotFoundEx
    prepare(new FileNotFoundException("fluke"));

    try {
      // reindex all
      indexerManager.reindexAllRepositories(null, false);

      // we continue here as 404 should not end up with exception (is "swallowed")

      // ensure we fetched from one we wanted (failingRepository)
      Assert.assertEquals(indexedProxyRepositories, fetchCountingInvocationHandler.getInvocationCount());
      // ensure we scanned all the repositories, even the failing one, having 404 on remote update
      Assert.assertEquals(indexedRepositories, scanInvocationCount);
    }
    catch (IOException e) {
      Assert.fail("There should be no exception thrown!");
    }
  }

  @Test
  public void remoteNon404ResponseFailsProcessingAtTheEnd()
      throws Exception
  {
    // HTTP 401/403/etc boils down as some other IOException
    final IOException ex = new IOException("something bad happened");
    prepare(ex);

    try {
      // reindex all
      indexerManager.reindexAllRepositories(null, false);

      // the above line should throw IOex
      Assert.fail("There should be exception thrown!");
    }
    catch (IOException e) {
      // ensure we fetched from one we wanted (failingRepository)
      assertThat(fetchCountingInvocationHandler, new InvocationMatcher(indexedProxyRepositories));
      // ensure we scanned all the repositories (minus the one failed, as it failed _BEFORE_ scan invocation)
      Assert.assertEquals(indexedRepositories - 1, scanInvocationCount);

      // ensure suppressed exception detail is present
      assertThat(e.getSuppressed(), notNullValue());
      assertThat(e.getSuppressed()[0], is((Throwable)ex));
    }
  }

  /**
   * NEXUS-5542: Same as above, but for cascade operations.
   */
  @Test
  public void remoteIOExDoesNotFailsCascadeProcessingAtTheEnd()
      throws Exception
  {
    // HTTP 401/403/etc boils down as some other IOException
    final IOException ex = new IOException("something bad happened");
    prepare(ex);

    try {
      // reindex public group
      indexerManager.reindexRepository(null, "public", false);

      // the above line should throw IOex
      Assert.fail("There should be exception thrown!");
    }
    catch (IOException e) {
      final MavenGroupRepository publicGroup = repositoryRegistry
          .getRepositoryWithFacet("public", MavenGroupRepository.class);
      // ensure we fetched from one we wanted (failingRepository)
      assertThat(fetchCountingInvocationHandler, new InvocationMatcher(indexedProxyRepositories));
      // ensure we scanned all the repositories (minus the one failed, as it failed _BEFORE_ scan invocation)
      Assert.assertEquals(publicGroup.getMemberRepositoryIds().size() - 1, scanInvocationCount);

      // ensure suppressed exception detail is present
      assertThat(e.getSuppressed(), notNullValue());
      assertThat(e.getSuppressed()[0], is((Throwable) ex));
    }
  }

  // ==

  /**
   * {@link InvocationHandler} that simply passes the invocations to it's target.
   */
  public static class PassThruInvocationHandler
      implements InvocationHandler
  {
    private final Object delegate;

    public PassThruInvocationHandler(final Object delegate) {
      this.delegate = delegate;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args)
        throws Throwable
    {
      return method.invoke(delegate, args);
    }
  }

  /**
   * {@link InvocationHandler} that delegates the call to another {@link InvocationHandler}.
   */
  public static class DelegatingInvocationHandler
      implements InvocationHandler
  {
    private final InvocationHandler delegate;

    public DelegatingInvocationHandler(final InvocationHandler delegate) {
      this.delegate = delegate;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args)
        throws Throwable
    {
      return delegate.invoke(proxy, method, args);
    }
  }

  /**
   * {@link InvocationHandler} that counts the invocations of specified {@link Method}.
   */
  public static class CountingInvocationHandler
      extends DelegatingInvocationHandler
  {
    private final Method method;

    private int count;

    private List<Object[]> invocationsArguments;

    private List<StackTraceElement[]> invocationsStackTraces;

    public CountingInvocationHandler(final InvocationHandler delegate, final Method countedMethod) {
      super(delegate);
      this.method = countedMethod;
      this.count = 0;
      this.invocationsArguments = Lists.newArrayList();
      this.invocationsStackTraces = Lists.newArrayList();
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args)
        throws Throwable
    {
      if (method.equals(this.method)) {
        count++;
        invocationsArguments.add(args);

        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        final StackTraceElement[] actualStackTrace = new StackTraceElement[stackTrace.length - 3];
        System.arraycopy(stackTrace, 3, actualStackTrace, 0, stackTrace.length - 3);

        invocationsStackTraces.add(actualStackTrace);
      }
      return super.invoke(proxy, method, args);
    }

    public int getInvocationCount() {
      return count;
    }

    public void reset() {
      count = 0;
    }
  }

  /**
   * {@link InvocationHandler} that fails if specified {@link Method} with specified arguments (as matched by
   * {@link Predicate}) is invoked. Failing is simulated with preset {@link Exception}.
   */
  public static class FailingInvocationHandler
      extends DelegatingInvocationHandler
  {
    private final Method method;

    private final Predicate<Object[]> methodArgumentsPredicate;

    private final Exception failure;

    public FailingInvocationHandler(final InvocationHandler delegate, final Method failingMethod,
                                    final Predicate<Object[]> methodArgumentsPredicate, final Exception failure)
    {
      super(delegate);
      this.method = failingMethod;
      this.methodArgumentsPredicate = methodArgumentsPredicate;
      this.failure = failure;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args)
        throws Throwable
    {
      if (method.equals(this.method) && methodArgumentsPredicate.apply(args)) {
        throw failure;
      }
      else {
        return super.invoke(proxy, method, args);
      }
    }
  }

  private class InvocationMatcher
      extends TypeSafeMatcher<CountingInvocationHandler>
  {

    private final int expectedCount;

    InvocationMatcher(final int expectedCount) {
      this.expectedCount = expectedCount;
    }

    @Override
    protected boolean matchesSafely(final CountingInvocationHandler item) {
      return item.getInvocationCount() == expectedCount;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("to have ").appendValue(expectedCount).appendText(" invocations");
    }

    @Override
    protected void describeMismatchSafely(final CountingInvocationHandler item,
                                          final Description mismatchDescription)
    {
      mismatchDescription.appendText("had ").appendValue(item.getInvocationCount()).appendText(
          " invocations of method ").appendText(item.method.getDeclaringClass().getName()).appendText("#").appendText(
          item.method.getName()).appendText("\n");

      if (item.invocationsArguments.size() > 0) {
        for (int i = 0; i < item.invocationsArguments.size(); i++) {
          mismatchDescription.appendText("\n").appendText("Invocation ").appendValue(i + 1).appendText(
              ":\n");
          mismatchDescription.appendText("\t Arguments:\n");
          for (Object argument : item.invocationsArguments.get(i)) {
            if (argument instanceof IndexUpdateRequest) {
              IndexUpdateRequest arg = (IndexUpdateRequest) argument;
              mismatchDescription.appendText("\t\t ").appendText(
                  IndexUpdateRequest.class.getSimpleName()).appendText("->").appendText(
                  IndexingContext.class.getSimpleName()).appendText(" repository=").appendValue(
                  arg.getIndexingContext().getRepositoryId()).appendText("\n");
            }
            else {
              mismatchDescription.appendText("\t\t ").appendText(argument.toString()).appendText(
                  "\n");
            }
          }
          mismatchDescription.appendText("\t Stack trace:\n");
          for (StackTraceElement element : item.invocationsStackTraces.get(i)) {
            mismatchDescription.appendText("\t\t ").appendText(element.toString()).appendText("\n");
          }
        }
        mismatchDescription.appendText("\n");
      }
    }
  }

}
