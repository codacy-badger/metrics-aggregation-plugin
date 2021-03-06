package io.jenkins.plugins.metrics.model.measurement;

import java.util.Objects;

public class MethodMetricsMeasurement extends MetricsMeasurement {
    private static final long serialVersionUID = 6103621887323104682L;

    private ClassMetricsMeasurement parent = new ClassMetricsMeasurement();
    private String methodName = "";
    private int beginLine = -1;
    private int beginColumn = -1;
    private int endLine = -1;
    private int endColumn = -1;

    public ClassMetricsMeasurement getParent() {
        return parent;
    }

    public void setParent(final ClassMetricsMeasurement parent) {
        this.parent = parent;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(final String methodName) {
        this.methodName = methodName;
    }

    public int getBeginLine() {
        return beginLine;
    }

    public void setBeginLine(final int beginLine) {
        this.beginLine = beginLine;
    }

    public int getBeginColumn() {
        return beginColumn;
    }

    public void setBeginColumn(final int beginColumn) {
        this.beginColumn = beginColumn;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(final int endLine) {
        this.endLine = endLine;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public void setEndColumn(final int endColumn) {
        this.endColumn = endColumn;
    }

    /**
     * Merge another {@link MetricsMeasurement} with this one. Returns itself to be usable for chaining.
     *
     * @param metricsMeasurement
     *         the {@link MetricsMeasurement} to merge. If it is a {@link MethodMetricsMeasurement},their metrics are
     *         merged together, if it is a {@link ClassMetricsMeasurement}, a merge with the parent will be attempted.
     *
     * @return itself
     */
    @Override
    public MetricsMeasurement merge(final MetricsMeasurement metricsMeasurement) {
        if (metricsMeasurement instanceof ClassMetricsMeasurement && parent != null) {
            parent.merge(metricsMeasurement);
        }
        else if (metricsMeasurement instanceof MethodMetricsMeasurement) {
            metrics.putAll(metricsMeasurement.getMetrics());
        }
        return this;
    }

    @Override
    public String getQualifiedClassName() {
        if (parent == null) {
            return "";
        }
        return parent.getQualifiedClassName();
    }

    @Override
    public String toString() {
        if (this.equals(new MethodMetricsMeasurement())) {
            return "MethodMetricsMeasurement[empty]";
        }

        return String.format("MethodMetricsMeasurement[%s.%s#%s:%d:%d]",
                parent.getPackageName(),
                parent.getClassName(),
                methodName,
                beginLine,
                endLine);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof MethodMetricsMeasurement)) {
            return false;
        }

        MethodMetricsMeasurement other = (MethodMetricsMeasurement) o;

        return this.beginLine == other.beginLine
                && this.beginColumn == other.beginColumn
                && this.endLine == other.endLine
                && this.endColumn == other.endColumn
                && Objects.equals(methodName, other.methodName)
                && Objects.equals(metrics, other.metrics)
                && Objects.equals(parent, other.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beginColumn, beginLine, endLine, endColumn, methodName, parent, metrics);
    }
}
