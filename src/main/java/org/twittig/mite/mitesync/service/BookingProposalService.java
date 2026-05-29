package org.twittig.mite.mitesync.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.twittig.mite.mitesync.web.model.CalendarEventModel;
import org.twittig.mite.mitesync.web.model.MiteEntryModel;
import org.twittig.mite.mitesync.web.model.PbiAssignmentModel;
import org.twittig.mite.mitesync.web.model.ProposalEntryModel;
import org.twittig.mite.mitesync.web.model.WorkItemModel;

/**
 * Erzeugt aus Calendar-Events, DevOps-Aktivität, bereits gebuchten Mite-Einträgen und der
 * PBI-Zuordnung einen Tagesbuchungs-Vorschlag.
 *
 * <p>Regeln:
 * <ul>
 *   <li>Daily-Event ist immer fix auf {@code daily-fixed-minutes} (default 15 min), unabhängig von
 *       Calendar-Dauer.
 *   <li>Andere Meetings: aufgerundet auf nächstes 15-Min-Vielfaches.
 *   <li>Skip-Summaries werden ausgelassen.
 *   <li>Nicht akzeptierte Events ("declined") werden ausgelassen; "needsAction" wird mit aufgenommen
 *       (User kann manuell entfernen).
 *   <li>Bereits in Mite gebuchte Calendar-Events werden NICHT erneut vorgeschlagen (anhand der
 *       Notiz-Übereinstimmung).
 *   <li>Dev-Stunden = (Ziel-Minuten − Calendar-Minuten), zugewiesen an die Haupt-PBI.
 * </ul>
 */
@Service
public class BookingProposalService {

  @Value("${daily-reports.rules.daily-event-summary}")
  private String dailySummary;

  @Value("${daily-reports.rules.daily-fixed-minutes}")
  private int dailyFixedMinutes;

  @Value("${mite-sync.source.meeting-collector-pbi}")
  private String meetingCollectorPbi;

  /**
   * Default-Tagesziel in Minuten, wenn vom Client nicht im Body übersteuert.
   *
   * <p>6,25 h = 375 Min entspricht ca. 125h/20 Werktage.
   */
  private static final int DEFAULT_TARGET_MINUTES = 375;

  public List<ProposalEntryModel> buildProposal(
      List<CalendarEventModel> calendarEvents,
      List<MiteEntryModel> alreadyBooked,
      List<WorkItemModel> openWorkItems,
      PbiAssignmentModel pbiAssignment) {

    int targetMinutes =
        pbiAssignment.getTargetHours() == null
            ? DEFAULT_TARGET_MINUTES
            : (int) Math.round(pbiAssignment.getTargetHours() * 60);

    List<ProposalEntryModel> proposal = new ArrayList<>();
    int meetingMinutes = 0;

    // Already-booked notes (lowercase, getrimmt) für Duplikat-Check
    var alreadyNotes =
        alreadyBooked.stream()
            .map(MiteEntryModel::getNote)
            .filter(java.util.Objects::nonNull)
            .map(s -> s.trim().toLowerCase())
            .toList();

    for (CalendarEventModel ev : calendarEvents) {
      if (ev.isSkipped()) continue;
      if ("declined".equals(ev.getResponseStatus())) continue;

      int minutes;
      String summary = ev.getSummary();
      if (dailySummary != null && dailySummary.equalsIgnoreCase(summary)) {
        minutes = dailyFixedMinutes;
      } else {
        minutes = ev.getRoundedMinutes();
      }
      if (minutes <= 0) continue;

      String note = "#" + meetingCollectorPbi + " " + summary;
      // Skip wenn schon gebucht (gleiche Note)
      if (alreadyNotes.contains(note.trim().toLowerCase())) continue;

      proposal.add(new ProposalEntryModel(minutes, note, "calendar", null, null));
      meetingMinutes += minutes;
    }

    // Bereits gebuchte Minuten zählen mit fürs Tagesziel
    int alreadyMinutes = alreadyBooked.stream().mapToInt(MiteEntryModel::getMinutes).sum();

    // Dev-Anteil: rest auf Tagesziel füllen, auf 15 Min runden
    int remaining = targetMinutes - meetingMinutes - alreadyMinutes;
    remaining = Math.max(0, roundDownTo15(remaining));

    if (remaining > 0) {
      WorkItemModel mainPbi = findById(openWorkItems, pbiAssignment.getMainPbiId());
      String pbiTitle =
          mainPbi != null && mainPbi.getTitle() != null ? mainPbi.getTitle() : "(Titel unbekannt)";
      String note = "#" + pbiAssignment.getMainPbiId() + " " + pbiTitle;
      proposal.add(
          new ProposalEntryModel(
              remaining, note, "main-pbi-fill", pbiAssignment.getMainPbiId(), pbiTitle));
    }

    return proposal;
  }

  private WorkItemModel findById(List<WorkItemModel> items, Integer id) {
    if (id == null || items == null) return null;
    for (WorkItemModel w : items) {
      if (w.getId() == id) return w;
    }
    return null;
  }

  private static int roundDownTo15(int minutes) {
    return (minutes / 15) * 15;
  }
}
