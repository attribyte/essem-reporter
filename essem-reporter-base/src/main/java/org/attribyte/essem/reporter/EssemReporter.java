package org.attribyte.essem.reporter;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import org.attribyte.essem.metrics.HDRReservoir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public abstract class EssemReporter extends ScheduledReporter implements MetricSet {

   /**
    * Selects the HDR histogram reporting method.
    */
   public enum HdrReport {

      /**
       * Don't report.
       */
      NONE,

      /**
       * Report the total histogram.
       */
      TOTAL,

      /**
       * Report the snapshot histogram.
       */
      SNAPSHOT
   }

   protected EssemReporter(final URI uri,
                           final String authValue,
                           final boolean deflate,
                           final MetricRegistry registry,
                           final Clock clock,
                           final String application,
                           final String host,
                           final String instance,
                           final String role,
                           final String description,
                           final Supplier<String> statusSupplier,
                           final MetricFilter filter,
                           final TimeUnit rateUnit,
                           final TimeUnit durationUnit,
                           final boolean skipUnchangedMetrics,
                           final HdrReport hdrReport) {
      super(registry, "essem-reporter", filter, rateUnit, durationUnit);
      this.uri = uri;
      this.authValue = authValue;
      this.deflate = deflate;
      this.clock = clock;
      this.application = application;
      this.host = host;
      this.instance = instance;
      this.role = role;
      this.description = description;
      this.statusSupplier = statusSupplier;
      this.rateUnit = rateUnit;
      this.durationUnit = durationUnit;
      this.lastReportedCount = skipUnchangedMetrics ? Maps.newConcurrentMap() : null;
      this.hdrReport = hdrReport;
   }

   /**
    * Reads and discards all input from a stream.
    * @param is The input stream.
    * @throws IOException on read error.
    */
   protected void discardInputAndClose(final InputStream is) throws IOException {
      if(is != null) {
         ByteStreams.toByteArray(is);
         try {
            is.close();
         } catch(IOException ioe) {
            //Ignore
         }
      }
   }

   /**
    * Deflates bytes.
    * @param b The bytes to deflate.
    * @return The deflated bytes.
    */
   protected static byte[] deflate(final byte[] b) {
      try {
         Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, false); //nowrap = false
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater);
         dos.write(b);
         dos.close();
         return baos.toByteArray();
      } catch(IOException ioe) {
         throw new AssertionError("I/O exception on in-memory stream");
      }
   }

   /**
    * Sends the report bytes.
    * @param reportBytes The report bytes.
    * @return The number of bytes sent.
    * @throws IOException on output error.
    */
   protected int send(byte[] reportBytes) throws IOException {
      HttpURLConnection conn = null;
      InputStream is = null;
      try(Timer.Context ignore = sendTimer.time()) {
         URL url = uri.toURL();
         conn = (HttpURLConnection)url.openConnection();
         if(conn != null) {
            conn.setRequestMethod("PUT");
            conn.setRequestProperty(CONTENT_TYPE_HEADER, PROTOBUF_CONTENT_TYPE);
            if(!Strings.isNullOrEmpty(authValue)) {
               conn.setRequestProperty(AUTHORIZATION_HEADER, authValue);
            }
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);

            if(deflate) {
               conn.setRequestProperty(CONTENT_ENCODING_HEADER, DEFLATE_ENCODING);
               reportBytes = deflate(reportBytes);
            }
            conn.setFixedLengthStreamingMode(reportBytes.length);
            reportSize.update(reportBytes.length);
            conn.connect();
            OutputStream os = conn.getOutputStream();
            os.write(reportBytes);
            os.flush();
            os.close();
            int code = conn.getResponseCode();
            is = code / 100 == 2 ? conn.getInputStream() : conn.getErrorStream();
            discardInputAndClose(is);
            is = null;
            return code;
         } else {
            throw new IOException("Unable to 'PUT' to " + uri.toString());
         }
      } catch(IOException ioe) {
         if(conn != null && is == null) discardInputAndClose(conn.getErrorStream());
         throw ioe;
      } finally {
         if(conn != null) {
            conn.disconnect();
         }
      }
   }

   /**
    * The authorization header.
    */
   public static final String AUTHORIZATION_HEADER = "Authorization";

   /**
    * The content type header.
    */
   public static final String CONTENT_TYPE_HEADER = "Content-Type";

   /**
    * The protocol buffer content type (application/x-protobuf).
    */
   public static final String PROTOBUF_CONTENT_TYPE = "application/x-protobuf";

   /**
    * The content encoding header.
    */
   public static final String CONTENT_ENCODING_HEADER = "Content-Encoding";

   /**
    * The 'deflate' content type.
    */
   public static final String DEFLATE_ENCODING = "deflate";

   protected final String application;
   protected final String host;
   protected final String instance;
   protected String role;
   protected String description;
   protected Supplier<String> statusSupplier;
   protected final Clock clock;
   protected final URI uri;
   protected final TimeUnit rateUnit;
   protected final TimeUnit durationUnit;
   protected final String authValue;
   protected final boolean deflate;

   private final Timer sendTimer = new org.attribyte.essem.metrics.Timer();
   protected final Meter sendErrors = new Meter();
   protected final Histogram reportSize = new Histogram(new HDRReservoir(2, HDRReservoir.REPORT_SNAPSHOT_HISTOGRAM));
   protected final Counter skippedUnchanged = new Counter();

   private final ImmutableMap<String, Metric> metrics =
           ImmutableMap.of(
                   "reports", sendTimer,
                   "failed-reports", sendErrors,
                   "report-size-bytes", reportSize,
                   "skipped-unchanged", skippedUnchanged,
                   "report-count", new Gauge<Integer>() {
                      public Integer getValue() {
                         return lastMetricCount.get();
                      }
                   }
           );

   @Override
   public Map<String, Metric> getMetrics() {
      return metrics;
   }


   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
              .add("application", application)
              .add("host", host)
              .add("instance", instance)
              .add("role", role)
              .add("description", description)
              .add("uri", uri)
              .add("rateUnit", rateUnit)
              .add("durationUnit", durationUnit)
              .add("deflate", deflate)
              .add("skippedUnchanged", lastReportedCount != null)
              .add("hdrReport", hdrReport)
              .toString();
   }

   /**
    * A map that contains the last reported value for counters, meters, histograms and timers.
    * <p>
    *    If configured, and the previously reported value is unchanged, the metric
    *    will not be reported.
    * </p>
    */
   protected final Map<String, Long> lastReportedCount;


   /**
    * The number of metrics last reported.
    */
   protected final AtomicInteger lastMetricCount = new AtomicInteger();

   /**
    * Should reporting be skipped for this counted metric?
    *
    * <p>
    *    If the count is zero, no values have ever been added to the metric.
    *    If the count is unchanged, no measurements have been recorded since the last report.
    * </p>
    * @param name The metric name.
    * @param currentValue The current value.
    * @return Should reporting be skipped?
    */
   protected boolean skipCountedReport(final String name, final long currentValue) {
      if(lastReportedCount == null) {
         return false;
      } else if(lastReportedCount.getOrDefault(name, Long.MIN_VALUE) == currentValue) {
         skippedUnchanged.inc();
         return true;
      } else {
         lastReportedCount.put(name, currentValue);
         return false;
      }
   }

   /**
    * The HDR histogram report mode.
    */
   protected final HdrReport hdrReport;

   /**
    * The default logger.
    */
   protected static final Logger LOGGER = LoggerFactory.getLogger(EssemReporter.class);
}
