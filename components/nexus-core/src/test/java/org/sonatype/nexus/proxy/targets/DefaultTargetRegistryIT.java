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
package org.sonatype.nexus.proxy.targets;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.junit.Ignore;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Long run test that tries DefaultTargetRegistry with 100 threads and more then a thousand operations looking for
 * concurrency exceptions.
 * <p>
 * Note: this class contains one test method that is actually ignored on purpose. It's ignored, as it tests a thing
 * known that will not work -- and cannot work -- with current configuration framework: simultaneous changes made by
 * multiple threads of same component. The simple culprit is that "dirty" (the changed configuration) is kept as member
 * variable, hence, is shared and modified by all changing threads causing all kinds of problems.
 *
 * @author Marvin Froeder ( velo at sonatype.com )
 */
public class DefaultTargetRegistryIT
    extends AbstractDefaultTargetRegistryTest
{
  private Function<Target, String> toIds = new Function<Target, String>()
  {
    @Override
    public String apply(Target input) {
      checkNotNull(input);
      return input.getId();
    }
  };

  private static final int OPERATIONS = 200;

  private static final int THREADS = OPERATIONS / 10;

  public void single(final int ref)
      throws Exception
  {
    List<String> ids = Lists.newArrayList();
    for (int j = 0; j < 5; j++) {

      String id = Integer.toString(ref) + "-" + Integer.toString(j);
      // Long.toHexString( System.nanoTime() + ref + j + ref * j );
      Target target = new Target(id, "name" + id, j % 3 == 0 ? maven1 : maven2, Arrays.asList(".*/" + id));
      targetRegistry.addRepositoryTarget(target);
      ids.add(id);
    }
    applicationConfiguration.saveConfiguration();

    for (String id : ids) {
      Target t = targetRegistry.getRepositoryTarget(id);
      assertThat(t, notNullValue());
      assertThat(t.getId(), equalTo(id));
      assertThat(t.getName(), equalTo("name" + id));
    }

    Collection<String> targets = transform(targetRegistry.getRepositoryTargets(), toIds);
    targets.containsAll(ids);

    for (String id : ids) {
      targetRegistry.removeRepositoryTarget(id);
      // I really wanna try to break this
      applicationConfiguration.saveConfiguration();
    }

    targets = transform(targetRegistry.getRepositoryTargets(), toIds);
    for (String id : ids) {
      Target t = targetRegistry.getRepositoryTarget(id);
      assertThat("ref: " + ref + " ids: " + ids, t, nullValue());
      assertThat("ref: " + ref + " ids: " + ids, targets, not(containsInAnyOrder(id)));
    }
  }

  @Test
  @Ignore
  public void testConcurrency()
      throws Exception
  {
    List<FutureTask<String>> calls = Lists.newArrayList();
    for (int i = 0; i < OPERATIONS; i++) {
      final int j = i;
      Callable<String> c = new Callable<String>()
      {
        @Override
        public String call()
            throws Exception
        {
          single(j);

          return "ok";
        }
      };
      calls.add(new FutureTask<String>(c));
    }

    Executor executor = Executors.newFixedThreadPool(THREADS);
    for (FutureTask<String> futureTask : calls) {
      executor.execute(futureTask);
    }

    for (FutureTask<String> futureTask : calls) {
      assertThat(futureTask.get(), equalTo("ok"));
    }
  }

}