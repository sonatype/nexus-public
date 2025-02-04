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
package org.sonatype.nexus.common.filter;

import java.util.LinkedList;
import java.util.List;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Creates a bloom filter that increases in size as the number of elements increase to keep the probability of a
 * false positive down to a minimum when mightContain is called.
 *
 * For more information on how a standard bloom filter works see here https://llimllib.github.io/bloomfilter-tutorial/
 *
 * Some stats on the probabilities can be found here https://github.com/google/guava/issues/2520#issuecomment-231233736
 *
 * @since 3.11
 */
public class ScalableBloomFilter<T>
{
  private final List<BloomFilter<T>> filters = new LinkedList<>();

  private final Funnel<? super T> funnel;

  private final int filterCapacity;

  private final double falsePositiveProbability;

  public ScalableBloomFilter(
      final Funnel<? super T> funnel,
      final int filterCapacity,
      final double falsePositiveProbability)
  {
    checkArgument(filterCapacity > 0, "filter capacity must be greater than 0");
    checkArgument(falsePositiveProbability > 0, "fpp must be greater than 0");

    this.funnel = checkNotNull(funnel);
    this.filterCapacity = filterCapacity;
    this.falsePositiveProbability = falsePositiveProbability;
  }

  /**
   * Determines whether across all filters there is a chance that this element has already been added.
   *
   * @param input - the element to check.
   * @return whether the element may exist in the filter.
   */
  public boolean mightContain(final T input) {
    for (BloomFilter<T> filter : filters) {
      if (filter.mightContain(input)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Adds an element to the filter if chances are it isn't already contained (i.e. mightContain returns false).
   *
   * @param input - element to add
   * @return - whether the element was added or not
   */
  public boolean put(final T input) {
    return !mightContain(input) && getFilter().put(input);
  }

  /**
   * @return the probability of encountering a false positive.
   */
  public double expectedFpp() {
    double probabilitySum = 0.0;
    double combinatorialAnd = 0.0;

    List<Double> probabilities = filters.stream().mapToDouble(BloomFilter::expectedFpp).boxed().collect(toList());
    for (int i = 0; i < probabilities.size(); i++) {
      Double probability = probabilities.get(i);
      probabilitySum += probability;
      for (int j = i + 1; j < probabilities.size(); j++) {
        combinatorialAnd += (probability * probabilities.get(j));
      }
    }

    double andProbability = filters.stream()
        .mapToDouble(BloomFilter::expectedFpp)
        .reduce((a, b) -> a * b)
        .getAsDouble();

    // These events are not mutually exclusive so the formula for calculating the probability is
    // P(A) + P(B) + P(C) ... - P(A and B) - P(A and C) - P(B and C) ... + P (A and B and C...)
    return probabilitySum - combinatorialAnd + andProbability;
  }

  private BloomFilter<T> getFilter() {
    if (filters.isEmpty()) {
      filters.add(createFilter());
    }

    // expectedFpp() is an O(n) call so we create a new filter on count instead
    if (filters.size() == filterCapacity) {
      filters.add(createFilter());
    }

    return filters.get(filters.size() - 1);
  }

  private BloomFilter<T> createFilter() {
    return BloomFilter.create(funnel, filterCapacity, falsePositiveProbability);
  }
}
