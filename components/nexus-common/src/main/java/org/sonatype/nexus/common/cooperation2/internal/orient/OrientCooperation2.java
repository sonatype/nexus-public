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
package org.sonatype.nexus.common.cooperation2.internal.orient;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.IOCall;
import org.sonatype.nexus.common.cooperation2.internal.Cooperation2Builder;
import org.sonatype.nexus.common.io.Cooperation;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.41
 */
public class OrientCooperation2
    extends ComponentSupport
    implements Cooperation2
{
  private Cooperation cooperation;

  public OrientCooperation2(final Cooperation cooperation) {
    this.cooperation = checkNotNull(cooperation);
  }

  @Override
  public <RET> Builder<RET> on(final IOCall<RET> workFunction) {
    return new OrientCooperation2Builder<>(workFunction);
  }

  @Override
  public Map<String, Integer> getThreadCountPerKey() {
    return cooperation.getThreadCountPerKey();
  }

  protected class OrientCooperation2Builder<RET>
      extends Cooperation2Builder<RET>
  {
    protected OrientCooperation2Builder(final IOCall<RET> workFunction) {
      super(workFunction);
    }

    @Override
    public RET cooperate(final String action, final String... scopes) throws IOException {
      StringJoiner joiner = new StringJoiner(":");
      joiner.add(action);
      Arrays.asList(scopes).forEach(joiner::add);
      return cooperation.cooperate(joiner.toString(), failover -> {
        Optional<RET> cachedValue;
        if (failover && (cachedValue = cooperation.join(checkFunction::check)).isPresent()) {
          return cachedValue.get();
        }
        return workFunction.call();
      });
    }
  }
}
