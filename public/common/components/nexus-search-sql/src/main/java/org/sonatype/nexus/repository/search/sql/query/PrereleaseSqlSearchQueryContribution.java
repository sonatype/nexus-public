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
package org.sonatype.nexus.repository.search.sql.query;

import java.util.Optional;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.sql.query.syntax.BooleanTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;
import org.sonatype.nexus.rest.ValidationErrorsException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.repository.search.sql.query.syntax.Operand.EQ;

@Component
@Qualifier(PrereleaseSqlSearchQueryContribution.NAME)
public class PrereleaseSqlSearchQueryContribution
    extends ComponentSupport
    implements SqlSearchQueryContribution
{
  public static final String NAME = "isPrerelease";

  @Override
  public Optional<Expression> createPredicate(SearchFilter filter) {
    log.debug("Creating predicate for {}", filter);

    if (filter == null) {
      return Optional.empty();
    }

    String valueString = filter.getValue().trim();
    Boolean value = Boolean.valueOf(valueString);

    if (!value.toString().equalsIgnoreCase(valueString)) {
      throw new ValidationErrorsException("Pre-release only supports true or false");
    }

    return Optional.of(new SqlPredicate(EQ, SearchField.PRERELEASE, new BooleanTerm(value)));
  }
}
