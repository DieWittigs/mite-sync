package org.twittig.mite.mitesync.web.model;

import java.time.LocalDateTime;

/**
 * Azure DevOps work item relevant for the daily booking (either changed by the user on the
 * given day, or open and currently assigned to them).
 */
public class WorkItemModel {

  private int id;
  private String type; // Product Backlog Item, Task, Bug
  private String state; // New, Approved, In Progress, Done
  private String title;
  private String assignedTo;
  private String changedBy;
  private LocalDateTime changedDate;
  private boolean changedByMe; // changed by the current user on the date
  private boolean assignedToMe; // currently assigned to the current user

  public WorkItemModel() {}

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getAssignedTo() {
    return assignedTo;
  }

  public void setAssignedTo(String assignedTo) {
    this.assignedTo = assignedTo;
  }

  public String getChangedBy() {
    return changedBy;
  }

  public void setChangedBy(String changedBy) {
    this.changedBy = changedBy;
  }

  public LocalDateTime getChangedDate() {
    return changedDate;
  }

  public void setChangedDate(LocalDateTime changedDate) {
    this.changedDate = changedDate;
  }

  public boolean isChangedByMe() {
    return changedByMe;
  }

  public void setChangedByMe(boolean changedByMe) {
    this.changedByMe = changedByMe;
  }

  public boolean isAssignedToMe() {
    return assignedToMe;
  }

  public void setAssignedToMe(boolean assignedToMe) {
    this.assignedToMe = assignedToMe;
  }
}
