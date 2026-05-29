package org.twittig.mite.mitesync.web.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Antwort des POST /daily-reports/{date}/preview-Endpoints. Enthält alle Quelldaten und einen
 * Buchungsvorschlag, den der Client editieren kann bevor er an /book geschickt wird.
 */
public class DailyReportModel {

  private LocalDate date;
  private String dayOfWeek;

  /** Calendar-Events des Tages (gefilterte: alle die nicht skipped sind). */
  private List<CalendarEventModel> calendarEvents;

  /** Mite-Einträge die schon für diesen Tag gebucht sind. */
  private List<MiteEntryModel> alreadyBookedInMite;

  /** Work Items, die der User am Stichtag bewegt hat. */
  private List<WorkItemModel> devOpsActivityOnDate;

  /** Aktuell offene Work Items, die dem User zugewiesen sind (für manuelle Auswahl). */
  private List<WorkItemModel> openWorkItems;

  /** Generierter Vorschlag: Liste an Einträgen, die gebucht werden würden. */
  private List<ProposalEntryModel> proposal;

  /** Gesamtminuten der vorgeschlagenen Einträge. */
  private int proposalTotalMinutes;

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public String getDayOfWeek() {
    return dayOfWeek;
  }

  public void setDayOfWeek(String dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }

  public List<CalendarEventModel> getCalendarEvents() {
    return calendarEvents;
  }

  public void setCalendarEvents(List<CalendarEventModel> calendarEvents) {
    this.calendarEvents = calendarEvents;
  }

  public List<MiteEntryModel> getAlreadyBookedInMite() {
    return alreadyBookedInMite;
  }

  public void setAlreadyBookedInMite(List<MiteEntryModel> alreadyBookedInMite) {
    this.alreadyBookedInMite = alreadyBookedInMite;
  }

  public List<WorkItemModel> getDevOpsActivityOnDate() {
    return devOpsActivityOnDate;
  }

  public void setDevOpsActivityOnDate(List<WorkItemModel> devOpsActivityOnDate) {
    this.devOpsActivityOnDate = devOpsActivityOnDate;
  }

  public List<WorkItemModel> getOpenWorkItems() {
    return openWorkItems;
  }

  public void setOpenWorkItems(List<WorkItemModel> openWorkItems) {
    this.openWorkItems = openWorkItems;
  }

  public List<ProposalEntryModel> getProposal() {
    return proposal;
  }

  public void setProposal(List<ProposalEntryModel> proposal) {
    this.proposal = proposal;
  }

  public int getProposalTotalMinutes() {
    return proposalTotalMinutes;
  }

  public void setProposalTotalMinutes(int proposalTotalMinutes) {
    this.proposalTotalMinutes = proposalTotalMinutes;
  }
}
