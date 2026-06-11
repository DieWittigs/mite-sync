package org.twittig.mite.mitesync.facade;

import org.twittig.mite.mitesync.config.DailyReportProperties.WorkflowType;

/** Thrown when a profile requests a workflow type that is not implemented yet. Mapped to 501. */
public class UnsupportedWorkflowException extends RuntimeException {

  public UnsupportedWorkflowException(WorkflowType workflowType) {
    super("Workflow type '" + workflowType + "' is not implemented yet");
  }
}
