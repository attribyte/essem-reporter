/*
 * Copyright 2015 Attribyte, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package org.attribyte.essem.reporter;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.protobuf.ByteString;
import org.attribyte.essem.metrics.HDRReservoir;
import org.attribyte.essem.proto.ReportProtos;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * A scheduled metric reporter that reports over HTTP(s) using the "essem"
 * protocol.
 */
@SuppressWarnings("rawtypes")
public class Proto2Reporter extends EssemReporter implements MetricSet {

   /**
    * Creates a builder.
    * @param uri The URI to report to.
    * @param registry the registry to report
    * @return The builder.
    */
   public static Builder newBuilder(final URI uri, final MetricRegistry registry) {
      return new Proto2Builder(uri, registry);
   }

   Proto2Reporter(final URI uri,
                  final String authValue,
                  final boolean deflate,
                  final MetricRegistry registry,
                  final Clock clock,
                  final String application,
                  final String host,
                  final String instance,
                  final MetricFilter filter,
                  final TimeUnit rateUnit,
                  final TimeUnit durationUnit,
                  final boolean skipUnchangedMetrics,
                  final HdrReport hdrReport) {
      super(uri, authValue, deflate, registry, clock, application, host, instance, filter,
              rateUnit, durationUnit, skipUnchangedMetrics, hdrReport);
   }

   ReportProtos.EssemReport buildReport(SortedMap<String, Gauge> gauges,
                                        SortedMap<String, Counter> counters,
                                        SortedMap<String, Histogram> histograms,
                                        SortedMap<String, Meter> meters,
                                        SortedMap<String, Timer> timers) {

      ReportProtos.EssemReport.Builder builder = ReportProtos.EssemReport.newBuilder();
      builder.setTimestamp(clock.getTime());
      builder.setDurationUnit(toProto(durationUnit));
      builder.setRateUnit(toProto(rateUnit));
      if(application != null) builder.setApplication(application);
      if(host != null) builder.setHost(host);
      if(instance != null) builder.setInstance(instance);

      lastMetricCount.set(gauges.size() + counters.size() + histograms.size() + meters.size() + timers.size());

      for(Map.Entry<String, Gauge> gauge : gauges.entrySet()) {
         Object val = gauge.getValue().getValue();
         ReportProtos.EssemReport.Gauge.Builder gaugeBuilder = builder.addGaugeBuilder();
         gaugeBuilder.setName(gauge.getKey());
         if(val instanceof Number) {
            gaugeBuilder.setValue(((Number)val).doubleValue());
         } else {
            gaugeBuilder.setComment(val.toString());
         }
      }

      for(Map.Entry<String, Counter> counter : counters.entrySet()) {
         String name = counter.getKey();
         long value = counter.getValue().getCount();
         if(!skipCountedReport(name, value)) {
            builder.addCounterBuilder()
                    .setName(name)
                    .setCount(value);
         }
      }

      for(Map.Entry<String, Meter> nv : meters.entrySet()) {
         String name = nv.getKey();
         Meter meter = nv.getValue();
         if(!skipCountedReport(name, meter.getCount())) {
            builder.addMeterBuilder()
                    .setName(name)
                    .setCount(meter.getCount())
                    .setOneMinuteRate(convertRate(meter.getOneMinuteRate()))
                    .setFiveMinuteRate(convertRate(meter.getFiveMinuteRate()))
                    .setFifteenMinuteRate(convertRate(meter.getFifteenMinuteRate()))
                    .setMeanRate(convertRate(meter.getMeanRate()));
         }
      }

      for(Map.Entry<String, Histogram> nv : histograms.entrySet()) {
         String name = nv.getKey();
         Histogram histogram = nv.getValue();
         if(!skipCountedReport(name, histogram.getCount())) {
            Snapshot snapshot = histogram.getSnapshot();
            final HDRReservoir.HDRSnapshot hdrSnapshot;
            if(snapshot instanceof HDRReservoir.HDRSnapshot && hdrReport != HdrReport.NONE) {
               hdrSnapshot = (HDRReservoir.HDRSnapshot)snapshot;
               switch(hdrReport) {
                  case TOTAL:
                     snapshot = hdrSnapshot.totalSnapshot();
                     break;
                  case SNAPSHOT:
                     snapshot = hdrSnapshot.sinceLastSnapshot();
                     break;
               }
            } else {
               hdrSnapshot = null;
            }

            ReportProtos.EssemReport.Histogram.Builder histogramBuilder = builder.addHistogramBuilder();
            histogramBuilder
                    .setName(name)
                    .setCount(histogram.getCount())
                    .setMax(snapshot.getMax())
                    .setMin(snapshot.getMin())
                    .setMedian(snapshot.getMedian())
                    .setMean(snapshot.getMean())
                    .setStd(snapshot.getStdDev())
                    .setPercentile75(snapshot.get75thPercentile())
                    .setPercentile95(snapshot.get95thPercentile())
                    .setPercentile98(snapshot.get98thPercentile())
                    .setPercentile99(snapshot.get99thPercentile())
                    .setPercentile999(snapshot.get999thPercentile());

            if(hdrSnapshot != null) {
               org.HdrHistogram.Histogram storedHistogram = hdrSnapshot.sinceLastSnapshot().getHistogram();
               ByteBuffer buf = ByteBuffer.allocate(storedHistogram.getNeededByteBufferCapacity());
               int compressedSize = storedHistogram.encodeIntoCompressedByteBuffer(buf);
               buf.rewind();
               histogramBuilder.setHdrHistogram(ByteString.copyFrom(buf, compressedSize));
            }
         }
      }

      for(Map.Entry<String, Timer> nv : timers.entrySet()) {
         final String name = nv.getKey();
         Timer timer = nv.getValue();
         if(!skipCountedReport(name, timer.getCount())) {
            Snapshot snapshot = timer.getSnapshot();
            final HDRReservoir.HDRSnapshot hdrSnapshot;
            if(snapshot instanceof HDRReservoir.HDRSnapshot && hdrReport != HdrReport.NONE) {
               hdrSnapshot = (HDRReservoir.HDRSnapshot)snapshot;
               switch(hdrReport) {
                  case TOTAL:
                     snapshot = hdrSnapshot.totalSnapshot();
                     break;
                  case SNAPSHOT:
                     snapshot = hdrSnapshot.sinceLastSnapshot();
                     break;
               }
            } else {
               hdrSnapshot = null;
            }

            ReportProtos.EssemReport.Timer.Builder timerBuilder = builder.addTimerBuilder();
            timerBuilder
                    .setName(nv.getKey())
                    .setOneMinuteRate(convertRate(timer.getOneMinuteRate()))
                    .setFiveMinuteRate(convertRate(timer.getFiveMinuteRate()))
                    .setFifteenMinuteRate(convertRate(timer.getFifteenMinuteRate()))
                    .setMeanRate(convertRate(timer.getMeanRate()))
                    .setCount(timer.getCount())
                    .setMax(convertDuration(snapshot.getMax()))
                    .setMin(convertDuration(snapshot.getMin()))
                    .setMedian(convertDuration(snapshot.getMedian()))
                    .setMean(convertDuration(snapshot.getMean()))
                    .setStd(convertDuration(snapshot.getStdDev()))
                    .setPercentile75(convertDuration(snapshot.get75thPercentile()))
                    .setPercentile95(convertDuration(snapshot.get95thPercentile()))
                    .setPercentile98(convertDuration(snapshot.get98thPercentile()))
                    .setPercentile99(convertDuration(snapshot.get99thPercentile()))
                    .setPercentile999(convertDuration(snapshot.get999thPercentile()));

            if(hdrSnapshot != null) {
               org.HdrHistogram.Histogram storedHistogram = hdrSnapshot.sinceLastSnapshot().getHistogram();
               ByteBuffer buf = ByteBuffer.allocate(storedHistogram.getNeededByteBufferCapacity());
               int compressedSize = storedHistogram.encodeIntoCompressedByteBuffer(buf);
               buf.rewind();
               timerBuilder.setHdrHistogram(ByteString.copyFrom(buf, compressedSize));
            }
         }
      }

      return builder.build();
   }

   @Override
   public void report(SortedMap<String, Gauge> gauges,
                      SortedMap<String, Counter> counters,
                      SortedMap<String, Histogram> histograms,
                      SortedMap<String, Meter> meters,
                      SortedMap<String, Timer> timers) {

      try {
         int responseCode = send(buildReport(gauges, counters, histograms, meters, timers).toByteArray());
         if(responseCode / 100 != 2) {
            LOGGER.warn("EssemReporter: Unable to report (" + responseCode + ")");
            sendErrors.mark();
         } else {
            LOGGER.debug("EssemReporter: Reported (" + responseCode + ")");
         }
      } catch(Throwable ioe) {
         ioe.printStackTrace();
         LOGGER.warn("Unable to report to Essem", ioe);
         sendErrors.mark();
      }
   }

   /**
    * Converts time units to the proto enum.
    * @param timeUnit The time unit.
    * @return The protobuf enum value.
    */
   protected ReportProtos.EssemReport.TimeUnit toProto(final TimeUnit timeUnit) {
      switch(timeUnit) {
         case NANOSECONDS: return ReportProtos.EssemReport.TimeUnit.NANOS;
         case MICROSECONDS: return ReportProtos.EssemReport.TimeUnit.MICROS;
         case MILLISECONDS: return ReportProtos.EssemReport.TimeUnit.MILLIS;
         case SECONDS: return ReportProtos.EssemReport.TimeUnit.SECONDS;
         case MINUTES: return ReportProtos.EssemReport.TimeUnit.MINUTES;
         case HOURS: return ReportProtos.EssemReport.TimeUnit.HOURS;
         case DAYS: return ReportProtos.EssemReport.TimeUnit.DAYS;
         default: throw new AssertionError();
      }
   }
}