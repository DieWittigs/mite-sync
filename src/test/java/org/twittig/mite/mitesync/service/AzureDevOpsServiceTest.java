package org.twittig.mite.mitesync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.twittig.mite.mitesync.web.model.WorkItemModel;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class AzureDevOpsServiceTest {

  @InjectMocks
  private AzureDevOpsService service;

  private HttpClient httpClientMock;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "organization", "my-org");
    ReflectionTestUtils.setField(service, "project", "My Project");
    ReflectionTestUtils.setField(service, "pat", "test-pat-token");
    httpClientMock = mock(HttpClient.class);
    ReflectionTestUtils.setField(service, "httpClient", httpClientMock);
  }

  // --- joinIds static utility ---

  @Test
  void joinIds_singleId() {
    assertThat(AzureDevOpsService.joinIds(List.of(42))).isEqualTo("42");
  }

  @Test
  void joinIds_multipleIds() {
    assertThat(AzureDevOpsService.joinIds(List.of(1, 2, 3))).isEqualTo("1,2,3");
  }

  @Test
  void joinIds_empty() {
    assertThat(AzureDevOpsService.joinIds(List.of())).isEmpty();
  }

  // --- getWorkItemsChangedByMeOnDate ---

  @Test
  void getWorkItemsChangedByMeOnDate_returnsMappedWorkItem() throws Exception {
    String wiqlJson = "{\"workItems\":[{\"id\":123}]}";
    String batchJson = batchResponse(123, "Task", "My Feature", "In Progress",
        "Thomas Wittig", "Thomas Wittig", "2024-03-15T10:00:00");

    var wiqlResp = mockResponse(200, wiqlJson);
    var batchResp = mockResponse(200, batchJson);
    when(httpClientMock.send(any(HttpRequest.class), any()))
        .thenReturn(wiqlResp)
        .thenReturn(batchResp);

    List<WorkItemModel> result = service.getWorkItemsChangedByMeOnDate(LocalDate.of(2024, 3, 15));

    assertThat(result).hasSize(1);
    WorkItemModel item = result.get(0);
    assertThat(item.getId()).isEqualTo(123);
    assertThat(item.getType()).isEqualTo("Task");
    assertThat(item.getTitle()).isEqualTo("My Feature");
    assertThat(item.getState()).isEqualTo("In Progress");
    assertThat(item.getChangedBy()).isEqualTo("Thomas Wittig");
    assertThat(item.isChangedByMe()).isTrue();
    assertThat(item.isAssignedToMe()).isFalse();
  }

  @Test
  void getWorkItemsChangedByMeOnDate_noWorkItems_returnsEmpty() throws Exception {
    String wiqlJson = "{\"workItems\":[]}";
    var wiqlResp = mockResponse(200, wiqlJson);
    when(httpClientMock.send(any(HttpRequest.class), any())).thenReturn(wiqlResp);

    List<WorkItemModel> result = service.getWorkItemsChangedByMeOnDate(LocalDate.of(2024, 3, 15));

    assertThat(result).isEmpty();
  }

  @Test
  void getWorkItemsChangedByMeOnDate_httpError_returnsEmpty() throws Exception {
    var errorResp = mockResponse(401, "Unauthorized");
    when(httpClientMock.send(any(HttpRequest.class), any())).thenReturn(errorResp);

    List<WorkItemModel> result = service.getWorkItemsChangedByMeOnDate(LocalDate.of(2024, 3, 15));

    assertThat(result).isEmpty();
  }

  @Test
  void getWorkItemsChangedByMeOnDate_networkException_returnsEmpty() throws Exception {
    when(httpClientMock.send(any(HttpRequest.class), any()))
        .thenThrow(new RuntimeException("Connection refused"));

    List<WorkItemModel> result = service.getWorkItemsChangedByMeOnDate(LocalDate.of(2024, 3, 15));

    assertThat(result).isEmpty();
  }

  // --- getOpenWorkItemsAssignedToMe ---

  @Test
  void getOpenWorkItemsAssignedToMe_marksAssignedToMe() throws Exception {
    String wiqlJson = "{\"workItems\":[{\"id\":456}]}";
    String batchJson = batchResponse(456, "Product Backlog Item", "Sprint PBI",
        "Approved", "Thomas Wittig", "PM Name", "2024-03-14T09:00:00");

    var wiqlResp = mockResponse(200, wiqlJson);
    var batchResp = mockResponse(200, batchJson);
    when(httpClientMock.send(any(HttpRequest.class), any()))
        .thenReturn(wiqlResp)
        .thenReturn(batchResp);

    List<WorkItemModel> result = service.getOpenWorkItemsAssignedToMe();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).isAssignedToMe()).isTrue();
    assertThat(result.get(0).isChangedByMe()).isFalse();
    assertThat(result.get(0).getId()).isEqualTo(456);
  }

  @Test
  void getOpenWorkItemsAssignedToMe_httpError_returnsEmpty() throws Exception {
    var errorResp = mockResponse(403, "Forbidden");
    when(httpClientMock.send(any(HttpRequest.class), any())).thenReturn(errorResp);

    List<WorkItemModel> result = service.getOpenWorkItemsAssignedToMe();

    assertThat(result).isEmpty();
  }

  // --- getWorkItemById ---

  @Test
  void getWorkItemById_returnsItem() throws Exception {
    String batchJson = batchResponse(789, "Bug", "NullPointerException",
        "New", "Thomas Wittig", "Thomas Wittig", "2024-03-10T08:00:00");

    var batchResp = mockResponse(200, batchJson);
    when(httpClientMock.send(any(HttpRequest.class), any())).thenReturn(batchResp);

    WorkItemModel result = service.getWorkItemById(789);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(789);
    assertThat(result.getType()).isEqualTo("Bug");
    assertThat(result.getTitle()).isEqualTo("NullPointerException");
  }

  @Test
  void getWorkItemById_emptyBatchResponse_returnsNull() throws Exception {
    String batchJson = "{\"value\":[]}";
    var batchResp = mockResponse(200, batchJson);
    when(httpClientMock.send(any(HttpRequest.class), any())).thenReturn(batchResp);

    WorkItemModel result = service.getWorkItemById(999);

    assertThat(result).isNull();
  }

  @Test
  void getWorkItemById_httpError_returnsNull() throws Exception {
    var errorResp = mockResponse(404, "Not Found");
    when(httpClientMock.send(any(HttpRequest.class), any())).thenReturn(errorResp);

    WorkItemModel result = service.getWorkItemById(999);

    assertThat(result).isNull();
  }

  @Test
  void getWorkItemById_changedDateNull_doesNotSetChangedDate() throws Exception {
    String batchJson = """
        {"value":[{"fields":{
          "System.Id":100,
          "System.WorkItemType":"Task",
          "System.Title":"Test",
          "System.State":"New",
          "System.AssignedTo":{"displayName":"Dev"},
          "System.ChangedBy":{"displayName":"Dev"},
          "System.ChangedDate":null
        }}]}""";
    var batchResp = mockResponse(200, batchJson);
    when(httpClientMock.send(any(HttpRequest.class), any())).thenReturn(batchResp);

    WorkItemModel result = service.getWorkItemById(100);

    assertThat(result).isNotNull();
    assertThat(result.getChangedDate()).isNull();
  }

  // --- helpers ---

  @SuppressWarnings("rawtypes")
  private HttpResponse mockResponse(int statusCode, String body) {
    HttpResponse resp = mock(HttpResponse.class);
    when(resp.statusCode()).thenReturn(statusCode);
    when(resp.body()).thenReturn(body);
    return resp;
  }

  private String batchResponse(int id, String type, String title, String state,
      String assignedTo, String changedBy, String changedDate) {
    return """
        {"value":[{"fields":{
          "System.Id":%d,
          "System.WorkItemType":"%s",
          "System.Title":"%s",
          "System.State":"%s",
          "System.AssignedTo":{"displayName":"%s"},
          "System.ChangedBy":{"displayName":"%s"},
          "System.ChangedDate":"%s"
        }}]}""".formatted(id, type, title, state, assignedTo, changedBy, changedDate);
  }
}
