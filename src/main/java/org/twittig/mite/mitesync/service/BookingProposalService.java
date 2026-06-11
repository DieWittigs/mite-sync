package org.twittig.mite.mitesync.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.twittig.mite.mitesync.config.DailyReportProperties.Profile;
import org.twittig.mite.mitesync.config.DailyReportProperties.Rules;
import org.twittig.mite.mitesync.web.model.CalendarEventModel;
import org.twittig.mite.mitesync.web.model.MiteEntryModel;
import org.twittig.mite.mitesync.web.model.PbiAssignmentModel;
import org.twittig.mite.mitesync.web.model.ProposalEntryModel;
import org.twittig.mite.mitesync.web.model.WorkItemModel;

/**
 * Combines calendar events, DevOps activity, already-booked Mite entries and the PBI assignment
 * into a daily booking proposal. All rule values come from the project profile.
 *
 * <p>Rules:
 * <ul>
 *   <li>The daily event is always booked at {@code daily-fixed-minutes} (default 15 min),
 *       regardless of calendar duration.
 *   <li>Other meetings are rounded up to the next 15-minute step.
 *   <li>Skip-summaries are excluded.
 *   <li>Declined events are excluded; {@code needsAction} events are included (the user can
 *       remove them manually).
 *   <li>Calendar events that are already booked in Mite are not proposed again (matched by
 *       note).
 *   <li>Dev minutes = target minutes − calendar minutes, assigned to the main PBI.
 * </ul>
 */
@Service
public class BookingProposalService {

  public List<ProposalEntryModel> buildProposal(
      Profile profile,
      List<CalendarEventModel> calendarEvents,
      List<MiteEntryModel> alreadyBooked,
      List<WorkItemModel> openWorkItems,
      PbiAssignmentModel pbiAssignment) {

    Rules rules = profile.getRules();
    int targetMinutes =
        pbiAssignment.getTargetHours() == null
            ? rules.getTargetMinutes()
            : (int) Math.round(pbiAssignment.getTargetHours() * 60);

    List<ProposalEntryModel> proposal = new ArrayList<>();
    int meetingMinutes = 0;

    // Already-booked notes (lowercased, trimmed) for the duplicate check
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
      if (rules.getDailyEventSummary() != null
          && rules.getDailyEventSummary().equalsIgnoreCase(summary)) {
        minutes = rules.getDailyFixedMinutes();
      } else {
        minutes = ev.getRoundedMinutes();
      }
      if (minutes <= 0) continue;

      String note = "#" + profile.getMeetingCollectorPbi() + " " + summary;
      // Skip when an entry with the same note is already booked
      if (alreadyNotes.contains(note.trim().toLowerCase())) continue;

      proposal.add(new ProposalEntryModel(minutes, note, "calendar", null, null));
      meetingMinutes += minutes;
    }

    // Already-booked minutes count toward the daily target
    int alreadyMinutes = alreadyBooked.stream().mapToInt(MiteEntryModel::getMinutes).sum();

    // Dev share: fill the rest up to the daily target, rounded down to the rounding step
    int remaining = targetMinutes - meetingMinutes - alreadyMinutes;
    remaining = Math.max(0, roundDownToStep(remaining, rules.getRoundingStepMinutes()));

    if (remaining > 0) {
      WorkItemModel mainPbi = findById(openWorkItems, pbiAssignment.getMainPbiId());
      String pbiTitle =
          mainPbi != null && mainPbi.getTitle() != null ? mainPbi.getTitle() : "(unknown title)";
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

  private static int roundDownToStep(int minutes, int step) {
    return (minutes / step) * step;
  }
}
