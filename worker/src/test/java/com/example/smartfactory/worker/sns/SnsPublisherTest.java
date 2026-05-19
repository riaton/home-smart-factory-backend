package com.example.smartfactory.worker.sns;

import com.example.smartfactory.worker.anomaly.AnomalyLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SnsPublisherTest {

    private static final String TOPIC_ARN = "arn:aws:sns:ap-northeast-1:000000000000:iot-anomaly-notification";

    @Mock
    private SnsClient snsClient;

    private SnsPublisher publisher;

    @BeforeEach
    void setup() {
        SnsProperties properties = new SnsProperties(TOPIC_ARN, "ap-northeast-1");
        publisher = new SnsPublisher(snsClient, properties);
    }

    private AnomalyLog sampleLog() {
        return AnomalyLog.create(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "room01",
                "temperature",
                new BigDecimal("35.0"),
                new BigDecimal("38.2"),
                "温度が上限閾値(35.0℃)を超えました: 38.2℃");
    }

    @Test
    @DisplayName("正常時は SNS Publish を1回呼び出すこと")
    void publish_success_callsOnce() {
        given(snsClient.publish(any(PublishRequest.class))).willReturn(PublishResponse.builder().build());

        publisher.publish(sampleLog());

        then(snsClient).should(times(1)).publish(any(PublishRequest.class));
    }

    @Test
    @DisplayName("InvalidParameterException は即座に返却しリトライしないこと")
    void publish_invalidParameter_doesNotRetry() {
        given(snsClient.publish(any(PublishRequest.class)))
                .willThrow(InvalidParameterException.builder().message("invalid").build());

        publisher.publish(sampleLog());

        then(snsClient).should(times(1)).publish(any(PublishRequest.class));
    }

    @Test
    @DisplayName("SnsException は最大 3 回リトライすること")
    void publish_snsException_retriesUpToMaxAttempts() {
        SnsException transientError = (SnsException) SnsException.builder().message("ServiceUnavailable").build();
        given(snsClient.publish(any(PublishRequest.class))).willThrow(transientError);

        publisher.publish(sampleLog());

        then(snsClient).should(times(3)).publish(any(PublishRequest.class));
    }

    @Test
    @DisplayName("2 回失敗して 3 回目で成功した場合 3 回呼び出されること")
    void publish_failsTwiceThenSucceeds_callsThreeTimes() {
        SnsException transientError = (SnsException) SnsException.builder().message("ServiceUnavailable").build();
        given(snsClient.publish(any(PublishRequest.class)))
                .willThrow(transientError)
                .willThrow(transientError)
                .willReturn(PublishResponse.builder().build());

        publisher.publish(sampleLog());

        then(snsClient).should(times(3)).publish(any(PublishRequest.class));
    }
}
