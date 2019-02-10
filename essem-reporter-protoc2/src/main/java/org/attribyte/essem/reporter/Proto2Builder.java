package org.attribyte.essem.reporter;

import com.codahale.metrics.MetricRegistry;

import java.net.URI;

class Proto2Builder extends Builder {

   /**
    * Creates a builder.
    * @param uri The essem endpoint URI.
    * @param registry The registry to report.
    */
   Proto2Builder(final URI uri, final MetricRegistry registry) {
      super(uri, registry);
   }

   @Override
   public EssemReporter build() {
      return new Proto2Reporter(uri, authValue, deflate,
              registry, clock, application, host, instance, filter, rateUnit, durationUnit,
              skipUnchangedMetrics, hdrReport);
   }
}
