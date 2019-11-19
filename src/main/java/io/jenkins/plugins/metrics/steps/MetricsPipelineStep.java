package io.jenkins.plugins.metrics.steps;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

import org.eclipse.collections.impl.factory.Sets;

import edu.umd.cs.findbugs.annotations.NonNull;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * Pipeline step that reports metrics from the build.
 */
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.ExcessiveImports", "PMD.TooManyFields", "PMD.DataClass"})
public class MetricsPipelineStep extends Step implements Serializable {
    private static final long serialVersionUID = 1L;

    private String filePattern;

    /**
     * Creates a new instance of {@link MetricsPipelineStep}.
     */
    @DataBoundConstructor
    public MetricsPipelineStep() {
        super();
        // empty constructor required for Stapler
    }

    @Override
    public StepExecution start(final StepContext context) {
        return new Execution(context, this);
    }

    @DataBoundSetter
    public void setFilePattern(final String filePattern) {
        this.filePattern = filePattern;
    }

    public String getFilePattern() {
        return filePattern;
    }

    /**
     * Actually performs the execution of the associated step.
     */
    static class Execution extends AnalysisExecution<Void> {
        private static final long serialVersionUID = -2840020502160375407L;

        private final MetricsPipelineStep step;

        Execution(@NonNull final StepContext context, final MetricsPipelineStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws IOException, InterruptedException {
            FilePath workspace = getWorkspace();
            workspace.mkdirs();

            MetricsRecorder recorder = new MetricsRecorder();
            recorder.perform(getRun(), workspace, getTaskListener());
            return null;
        }

    }

    /**
     * Descriptor for this step: defines the context and the UI labels.
     */
    @Extension
    @SuppressWarnings("unused") // most methods are used by the corresponding jelly view
    public static class Descriptor extends StepDescriptor {
        @Override
        public String getFunctionName() {
            return "reportMetrics";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Report Metrics";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Sets.immutable.of(FilePath.class, FlowNode.class, Run.class, TaskListener.class).castToSet();
        }
    }
}