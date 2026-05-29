package org.twittig.mite.mitesync.web.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request body für POST /daily-reports/{date}/book — enthält die finalen Einträge (typischerweise
 * vom Preview-Endpoint generiert und ggf. vom User editiert).
 */
public class BookingRequestModel {

  @NotEmpty(message = "entries darf nicht leer sein")
  @Valid
  private List<ProposalEntryModel> entries;

  public BookingRequestModel() {}

  public List<ProposalEntryModel> getEntries() {
    return entries;
  }

  public void setEntries(List<ProposalEntryModel> entries) {
    this.entries = entries;
  }
}
