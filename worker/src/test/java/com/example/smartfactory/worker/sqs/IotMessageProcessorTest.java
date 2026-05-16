package com.example.smartfactory.worker.sqs;

import com.example.smartfactory.common.exception.ResourceNotFoundException;
import com.example.smartfactory.worker.iotdata.IotDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class IotMessageProcessorTest {

    private static final String QUEUE_URL = "http://localhost:4566/test/iot-data-queue";

    private static final String RECEIPT_HANDLE = "test-receipt-handle";

    @Mock
    private IotDataService iotDataService;

    @Mock
    private SqsClient sqsClient;

    private IotMessageProcessor processor;

    @BeforeEach
    void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        SqsProperties properties = new SqsProperties(QUEUE_URL, "ap-northeast-1");
        processor = new IotMessageProcessor(iotDataService, sqsClient, properties, objectMapper);
    }

    private Message message(String body) {
        return Message.builder().body(body).receiptHandle(RECEIPT_HANDLE).build();
    }

    @Test
    @DisplayName("正常なメッセージは save を呼び出し DeleteMessage すること")
    void process_validMessage_savesAndDeletes() {
        String body = "{\"device_id\":\"room01\",\"temperature\":25.3,\"humidity\":60.1,"
                + "\"motion\":1,\"power_w\":120.5,\"recorded_at\":\"2026-01-15T10:00:00+09:00\"}";
        willDoNothing().given(iotDataService).save(any());

        processor.process(message(body));

        then(iotDataService).should().save(any());
        then(sqsClient).should().deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("JSONパースエラーの場合 save を呼ばず即 DeleteMessage すること")
    void process_invalidJson_deletesWithoutSaving() {
        processor.process(message("not-json"));

        then(iotDataService).shouldHaveNoInteractions();
        then(sqsClient).should().deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("device_id が null の場合 save を呼ばず即 DeleteMessage すること")
    void process_missingDeviceId_deletesWithoutSaving() {
        String body = "{\"temperature\":25.3,\"recorded_at\":\"2026-01-15T10:00:00+09:00\"}";
        processor.process(message(body));

        then(iotDataService).shouldHaveNoInteractions();
        then(sqsClient).should().deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("recorded_at が null の場合 save を呼ばず即 DeleteMessage すること")
    void process_missingRecordedAt_deletesWithoutSaving() {
        String body = "{\"device_id\":\"room01\",\"temperature\":25.3}";
        processor.process(message(body));

        then(iotDataService).shouldHaveNoInteractions();
        then(sqsClient).should().deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("未登録デバイスの場合 ResourceNotFoundException をキャッチして DeleteMessage すること")
    void process_unknownDevice_deletesMessage() {
        String body = "{\"device_id\":\"unknown\",\"temperature\":25.3,\"humidity\":60.1,"
                + "\"motion\":0,\"power_w\":100.0,\"recorded_at\":\"2026-01-15T10:00:00+09:00\"}";
        willThrow(new ResourceNotFoundException("Unknown device: unknown"))
                .given(iotDataService).save(any());

        processor.process(message(body));

        then(sqsClient).should().deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("RDS 障害時は DeleteMessage を呼ばずリトライを許すこと")
    void process_rdsError_doesNotDeleteMessage() {
        String body = "{\"device_id\":\"room01\",\"temperature\":25.3,\"humidity\":60.1,"
                + "\"motion\":1,\"power_w\":120.5,\"recorded_at\":\"2026-01-15T10:00:00+09:00\"}";
        willThrow(new RuntimeException("Connection refused"))
                .given(iotDataService).save(any());

        processor.process(message(body));

        then(sqsClient).shouldHaveNoInteractions();
    }
}
