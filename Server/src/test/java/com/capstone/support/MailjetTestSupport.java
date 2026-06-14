package com.capstone.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

public final class MailjetTestSupport {

  private MailjetTestSupport() {}

  public static void stubSuccessfulSend(MailjetClient mailjetClient) throws MailjetException {
    MailjetResponse response = org.mockito.Mockito.mock(MailjetResponse.class);
    when(response.getStatus()).thenReturn(HttpStatus.OK.value());
    when(response.getData()).thenReturn(successResponseData());
    when(mailjetClient.post(any())).thenReturn(response);
  }

  private static JSONArray successResponseData() {
    return new JSONArray()
        .put(
            new JSONObject()
                .put("Status", "success")
                .put(
                    "To",
                    new JSONArray()
                        .put(
                            new JSONObject()
                                .put("Email", "test@example.com")
                                .put("MessageUUID", "123")
                                .put("MessageID", 456)
                                .put("MessageHref", "https://example.com/message/456"))));
  }
}
