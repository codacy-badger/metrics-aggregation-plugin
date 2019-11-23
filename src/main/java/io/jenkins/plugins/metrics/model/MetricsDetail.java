package io.jenkins.plugins.metrics.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.google.common.collect.Lists;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.Report;

import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.ExportedBean;
import hudson.model.ModelObject;
import hudson.model.Run;

import io.jenkins.plugins.analysis.core.model.AnalysisResult;
import io.jenkins.plugins.analysis.core.model.ResultAction;
import io.jenkins.plugins.coverage.CoverageAction;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;
import io.jenkins.plugins.forensics.blame.Blames;
import io.jenkins.plugins.forensics.blame.FileBlame;
import io.jenkins.plugins.forensics.miner.FileStatistics;
import io.jenkins.plugins.forensics.miner.RepositoryStatistics;
import io.jenkins.plugins.metrics.util.JacksonFacade;

/**
 * Build view for metrics.
 *
 * @author Andreas Pabst
 */
@SuppressWarnings({"PMD.ExcessiveImports", "ClassDataAbstractionCoupling", "ClassFanOutComplexity"})
@ExportedBean
public class MetricsDetail implements ModelObject {
    private final Run<?, ?> owner;
    private final List<MetricsMeasurement> metricsMeasurements;

    public MetricsDetail(final Run<?, ?> owner, List<MetricsMeasurement> metricsMeasurements) {
        this.owner = owner;
        this.metricsMeasurements = metricsMeasurements;
    }

    @Override
    public String getDisplayName() {
        return "Metrics Detail View";
    }

    /**
     * Returns the build as owner of this object.
     *
     * @return the owner
     */
    public final Run<?, ?> getOwner() {
        return owner;
    }

    private String toJson(final Object object) {
        JacksonFacade facade = new JacksonFacade();
        return facade.toJson(object);
    }

    String toCSV(final String... values) {
        final String quote = "\"";
        final String separator = quote + "," + quote;

        return quote + String.join(separator, values) + quote;
    }

    private static final String ISSUES_NAMES = "package,basename,linestart,origin,severity,category,type,message";

    private String issueAsCSV(final Issue issue) {
        return toCSV(
                issue.getPackageName(),
                issue.getBaseName(),
                Integer.toString(issue.getLineStart()),
                //Integer.toString(issue.getLineEnd()),
                issue.getOrigin(),
                issue.getSeverity().getName(),
                issue.getCategory(),
                issue.getType(),
                issue.getMessage()
        );
    }

    private static final String STATISTICS_NAMES = "creationtime,lastmodified,authors,commits";

    private String fileStatisticsAsCSV(final FileStatistics stats) {
        return toCSV(
                //Integer.toString(stats.getCreationTime()),
                //Integer.toString(stats.getLastModificationTime()),
                "-1",
                "-1",
                Integer.toString(stats.getNumberOfAuthors()),
                Integer.toString(stats.getNumberOfCommits())
        );
    }

    private static final String BLAME_NAMES = "lastcommit,lastauthorname,lastauthoremail,lastcommittime";

    private String fileBlameAsCSV(final FileBlame blame, int line) {
        return toCSV(
                blame.getCommit(line),
                blame.getName(line),
                blame.getEmail(line),
                Integer.toString(blame.getTime(line))
        );
    }

    @SuppressWarnings("unused") // used by jelly view
    public String getMetrics() {
        Map<String, Report> issues = getAnalysisResults()
                .map(AnalysisResult::getIssues)
                .map(r -> r.groupByProperty("fileName"))
                .reduce(new HashMap<>(), (acc, map) -> {
                    map.forEach((key, report) -> acc.merge(key, report,
                            Report::addAll));
                    return acc;
                });

        return toJson(
                metricsMeasurements.stream()
                        .collect(Collectors.groupingBy(m -> m.getPackageName() + ";" + m.getClassName()))
                        .entrySet()
                        .stream()
                        .map(entry -> {
                            List<MetricsMeasurement> group = entry.getValue();

                            Optional<MetricsMeasurement> optionalClassMeasurement = group.stream()
                                    .filter(m -> m.getMethodName() == null || m.getMethodName().isEmpty())
                                    .findFirst();

                            if (optionalClassMeasurement.isPresent()) {
                                MetricsMeasurement classMeasurement = optionalClassMeasurement.get();
                                List<MetricsMeasurement> children = group.stream()
                                        .filter(m -> !m.equals(classMeasurement))
                                        .collect(Collectors.toList());

                                classMeasurement.setChildren(children);
                                return classMeasurement;
                            }
                            else {
                                MetricsMeasurement classMeasurement = new MetricsMeasurement();
                                String[] name = entry.getKey().split(";");
                                classMeasurement.setPackageName(name[0]);
                                classMeasurement.setClassName(name[1]);

                                classMeasurement.setChildren(group);
                                return classMeasurement;
                            }
                        })
                        .peek(measurement -> {
                            final int numberOfIssues = issues
                                    .getOrDefault(measurement.getFileName(), new Report())
                                    .getSize();
                            measurement.addMetric("ISSUES", numberOfIssues);
                        })
                        .collect(Collectors.toList())
        );
    }

    @JavaScriptMethod
    @SuppressWarnings("unused") // used by jelly view
    public String getMetricsTree(final String valueKey) {
        MetricsTreeNode root = metricsMeasurements.stream()
                .filter(m -> m.getMethodName() == null || m.getMethodName().isEmpty())
                .map(measurement -> {
                    double value = measurement.getMetrics().getOrDefault(valueKey, 0.0);
                    String qualifiedName = String.format("%s.%s",
                            measurement.getPackageName(),
                            measurement.getClassName());

                    return new MetricsTreeNode(qualifiedName, value);
                })
                .reduce(new MetricsTreeNode(""), (acc, node) -> {
                    acc.insertNode(node);
                    return acc;
                });

        root.collapsePackage();

        return toJson(root);
    }

    @JavaScriptMethod
    @SuppressWarnings("unused") // used by jelly view
    public String getHistogram(final String valueKey) {
        List<Double> values = metricsMeasurements.stream()
                .filter(m -> m.getMethodName() == null || m.getMethodName().isEmpty())
                .map(m -> m.getMetrics().getOrDefault(valueKey, Double.NaN))
                .filter(d -> !d.isNaN())
                .collect(Collectors.toList());

        final int numBins = 10;
        final int[] histogramData = new int[numBins];
        final double min = values.stream().min(Double::compareTo).orElse(0.0);
        final double max = values.stream().max(Double::compareTo).orElse(0.0);
        final double binWidth = (max - min) / numBins;

        for (double v : values) {
            int binId = (int) ((v - min) / binWidth);
            if (binId < 0) {
                binId = 0;
            }
            else if (binId >= numBins) {
                binId = numBins - 1;
            }

            histogramData[binId] += 1;
        }

        final String[] binLabels = new String[numBins];
        for (int i = 0; i < numBins; i++) {
            double left = min + i * binWidth;
            double right = min + (i + 1) * binWidth;
            binLabels[i] = String.format("%.1f - %.1f", left, right);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", histogramData);
        result.put("labels", binLabels);
        return toJson(result);
    }

    @JavaScriptMethod
    @SuppressWarnings("unused") // used by jelly view
    public String getStatistics(final String valueKey) {
        SummaryStatistics statistics = new SummaryStatistics();

        metricsMeasurements.stream()
                .filter(m -> m.getMethodName() == null || m.getMethodName().isEmpty())
                .map(m -> m.getMetrics().getOrDefault(valueKey, Double.NaN))
                .filter(d -> !d.isNaN())
                .forEach(statistics::addValue);

        Double[] values = metricsMeasurements.stream()
                .filter(m -> m.getMethodName() == null || m.getMethodName().isEmpty())
                .map(m -> m.getMetrics().getOrDefault(valueKey, Double.NaN))
                .filter(d -> !d.isNaN())
                .toArray(Double[]::new);

        EmpiricalDistribution distribution = new EmpiricalDistribution();
        distribution.load(ArrayUtils.toPrimitive(values));

        distribution.getBinCount();
        distribution.getNumericalVariance();
        distribution.getNumericalMean();

        return "";
    }

    private Stream<AnalysisResult> getAnalysisResults() {
        return owner.getActions(ResultAction.class)
                .stream()
                .map(ResultAction::getResult);
    }

    private String getIssues() {
        Map<String, Report> issues = getAnalysisResults()
                .map(AnalysisResult::getIssues)
                .map(r -> r.groupByProperty("fileName"))
                .reduce(new HashMap<>(), (acc, map) -> {
                    map.forEach((key, report) -> acc.merge(key, report,
                            Report::addAll));
                    return acc;
                });

        Blames blames = getAnalysisResults()
                .map(AnalysisResult::getBlames)
                .reduce(new Blames(), (acc, b) -> {
                    acc.addAll(b);
                    return acc;
                });

        RepositoryStatistics stats = getAnalysisResults()
                .map(AnalysisResult::getForensics)
                .reduce(new RepositoryStatistics(), (acc, r) -> {
                    acc.addAll(r);
                    return acc;
                });

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ISSUES_NAMES);
        stringBuilder.append(",");
        stringBuilder.append(STATISTICS_NAMES);
        stringBuilder.append(",");
        stringBuilder.append(BLAME_NAMES);
        stringBuilder.append("\n");

        issues.forEach((fileName, report) -> {
            FileBlame fileBlame = blames.getBlame(fileName);
            FileStatistics fileStatistics = stats.get(fileName);

            report.forEach(issue -> {
                stringBuilder.append(issueAsCSV(issue));
                stringBuilder.append(",");
                stringBuilder.append(fileStatisticsAsCSV(fileStatistics));
                stringBuilder.append(",");
                stringBuilder.append(fileBlameAsCSV(fileBlame, issue.getLineStart()));
                stringBuilder.append("\n");
            });
        });

        return stringBuilder.toString();
    }

    public String getCoverage() {
        return COVERAGE_NAMES + "\n"
                + owner.getActions(CoverageAction.class)
                .stream()
                .map(CoverageAction::getResult)
                .map(this::getChildrenRecursive)
                .flatMap(List::stream)
                .filter(r -> r.getElement().is("File"))
                .map(this::coverageAsCSV)
                .collect(Collectors.joining("\n"));
    }

    private static final String COVERAGE_NAMES = "package,basename,classcoverage,methodcoverage,instructioncoverage,conditionalcoverage,linecoverage";

    private String coverageAsCSV(final CoverageResult result) {
        return toCSV(
                normalizePackageName(result.getParent().getName()),
                result.getName(),
                ratioToString(result.getCoverage(CoverageElement.get("Class"))),
                ratioToString(result.getCoverage(CoverageElement.get("Method"))),
                ratioToString(result.getCoverage(CoverageElement.get("Instruction"))),
                ratioToString(result.getCoverage(CoverageElement.get("Conditional"))),
                ratioToString(result.getCoverage(CoverageElement.get("Line")))
        );
    }

    private String normalizePackageName(final String packageName) {
        return packageName != null ? packageName.replaceAll("/", ".") : "";
    }

    private String ratioToString(final Ratio ratio) {
        if (ratio == null) {
            return "";
        }
        return ratio.getPercentageString();
    }

    private List<CoverageResult> getChildrenRecursive(final CoverageResult result) {
        List<CoverageResult> children = Lists.newArrayList(result);

        for (CoverageResult res : result.getChildrenReal().values()) {
            children.addAll(getChildrenRecursive(res));
        }

        return children;
    }
}
