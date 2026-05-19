package com.example.smartfactory.worker.sqs;

import com.example.smartfactory.common.exception.ResourceNotFoundException;
import com.example.smartfactory.worker.anomaly.AnomalyDetectionService;
import com.example.smartfactory.worker.iotdata.IotDataService;
import com.example.smartfactory.worker.iotdata.IotMessagePayload;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IotMessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(IotMessageProcessor.class);

    private final IotDataService iotDataService;

    private final AnomalyDetectionService anomalyDetectionService;

    private final SqsClient sqsClient;

    private final SqsProperties sqsProperties;

    private final ObjectMapper objectMapper;

    public void process(Message message) {
        IotMessagePayload payload;
        try {
            payload = objectMapper.readValue(message.body(), IotMessagePayload.class);
        } catch (Exception e) {
            LOG.error("JSON parse error, discarding message: {}", e.getMessage());
            deleteMessage(message.receiptHandle());
            return;
        }

        if (payload.deviceId() == null || payload.recordedAt() == null) {
            LOG.error("Missing required fields, discarding: deviceId={} recordedAt={}",
                    payload.deviceId(), payload.recordedAt());
            deleteMessage(message.receiptHandle());
            return;
        }

        UUID userId;
        try {
            userId = iotDataService.save(payload);
        } catch (ResourceNotFoundException e) {
            LOG.error("Unknown device: {}, discarding message", payload.deviceId());
            deleteMessage(message.receiptHandle());
            return;
        } catch (Exception e) {
            LOG.error("Failed to save iot_data for device {}: {}", payload.deviceId(), e.getMessage());
            return;
        }

        deleteMessage(message.receiptHandle());

        try {
            anomalyDetectionService.detect(userId, payload.deviceId(), payload);
        } catch (Exception e) {
            LOG.error("anomaly_logs INSERT failed: {}", e.getMessage());
        }
    }

    private void deleteMessage(String receiptHandle) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(sqsProperties.queueUrl())
                .receiptHandle(receiptHandle)
                .build());
    }
}
