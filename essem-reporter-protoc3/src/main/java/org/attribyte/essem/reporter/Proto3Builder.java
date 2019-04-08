/*
 * Copyright 2018 Attribyte, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package org.attribyte.essem.reporter;

import com.codahale.metrics.MetricRegistry;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class Proto3Builder extends Builder {

   /**
    * Creates a builder.
    * @param uri The essem endpoint URI.
    * @param registry The registry to report.
    */
   Proto3Builder(final URI uri, final MetricRegistry registry) {
      super(uri, registry);
   }

   /**
    * Creates a builder.
    * @param props The properties.
    * @param registry The registry to report.
    * @throws IllegalArgumentException if a property is invalid.
    * @throws URISyntaxException if the report URI is invalid.
    */
   Proto3Builder(final Properties props, final MetricRegistry registry) throws IllegalArgumentException, URISyntaxException {
      super(props, registry);
   }

   @Override
   public EssemReporter build() {
      return new Proto3Reporter(uri, authValue, deflate,
              registry, clock, application, host, instance, role, description, statusSupplier, filter, rateUnit, durationUnit,
              skipUnchangedMetrics, hdrReport, alertSupplier);
   }

   /**
    * Creates a proto3 compatible report builder from properties.
    * @param props The properties.
    * @param registry The registry.
    * @return The builder.
    * @throws IllegalArgumentException if a property is invalid.
    * @throws URISyntaxException if the report URI is invalid.
    */
   public static Builder createBuilder(final Properties props, final MetricRegistry registry) throws IllegalArgumentException, URISyntaxException {
      return new Proto3Builder(props, registry);
   }

   /**
    * Creates a proto3 compatible report builder for a URI.
    * @param uri The URI.
    * @param registry The registry.
    * @return The builder.
    */
   public static Builder createBuilder(final URI uri, final MetricRegistry registry) {
      return new Proto3Builder(uri, registry);
   }
}
