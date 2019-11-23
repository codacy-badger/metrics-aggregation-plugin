package io.jenkins.plugins.metrics.model;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MetricsDetailTest {

    @Test
    void shouldReturnQuotedCSV() {
        MetricsDetail metricsDetail = new MetricsDetail(null, null);

        String result = metricsDetail.toCSV("hello", "world", "with,some,commas");

        assertThat(result).isEqualTo("\"hello\",\"world\",\"with,some,commas\"");
    }

    @Test
    public void shouldGetHistogram() {
        final String key = "key";
        List<MetricsMeasurement> measurements = new ArrayList<>();
        measurements.add(getMeasurementWithMetric(key, 5.0));
        measurements.add(getMeasurementWithMetric(key, 2.0));
        measurements.add(getMeasurementWithMetric(key, 1.1));
        measurements.add(getMeasurementWithMetric(key, 10.0));
        measurements.add(getMeasurementWithMetric(key, 1.0));
        measurements.add(getMeasurementWithMetric(key, 17.0));

        measurements.add(getMeasurementWithMetric(key + "foo", 3.0));

        MetricsDetail metricsDetail = new MetricsDetail(null, measurements);

        String json = metricsDetail.getHistogram(key);
        assertThat(json).isEqualTo("{\"data\":[3,0,1,0,0,1,0,0,0,1],"
                + "\"labels\":[\"1,0 - 2,6\",\"2,6 - 4,2\",\"4,2 - 5,8\",\"5,8 - 7,4\",\"7,4 - 9,0\","
                + "\"9,0 - 10,6\",\"10,6 - 12,2\",\"12,2 - 13,8\",\"13,8 - 15,4\",\"15,4 - 17,0\"]}");
    }

    @Test
    public void shouldGetStatistics() {
        final String key = "key";

        List<MetricsMeasurement> measurements = new ArrayList<>();
        measurements.add(getMeasurementWithMetric(key, 5.0));
        measurements.add(getMeasurementWithMetric(key, 2.0));
        measurements.add(getMeasurementWithMetric(key, 1.1));
        measurements.add(getMeasurementWithMetric(key, 10.0));
        measurements.add(getMeasurementWithMetric(key, 1.0));
        measurements.add(getMeasurementWithMetric(key, 17.0));

        MetricsDetail metricsDetail = new MetricsDetail(null, measurements);

        String json = metricsDetail.getStatistics(key);
        
    }

    private MetricsMeasurement getMeasurementWithMetric(final String key, final double value) {
        MetricsMeasurement metricsMeasurement = new MetricsMeasurement();
        metricsMeasurement.addMetric(key, value);
        return metricsMeasurement;
    }
}
