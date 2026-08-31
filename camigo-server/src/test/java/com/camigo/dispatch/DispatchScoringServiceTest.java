package com.camigo.dispatch;

import static org.assertj.core.api.Assertions.assertThat;

import com.camigo.config.DispatchProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DispatchScoringServiceTest {

    private final DispatchScoringService service = new DispatchScoringService(
            new DispatchProperties(180, 10, 3, 15,
                    40, 35, 25, 500, "dispatch-score-v1"));

    @Test
    void shouldCalculateDocumentedThreeScoresAndWeights() {
        DispatchScore score = service.score(500, 0, BigDecimal.valueOf(4));

        assertThat(score.distanceScore()).isEqualByComparingTo("50.0000");
        assertThat(score.workloadScore()).isEqualByComparingTo("100.0000");
        assertThat(score.serviceScore()).isEqualByComparingTo("80.0000");
        assertThat(score.totalScore()).isEqualByComparingTo("75.0000");
    }

    @Test
    void shouldUseThreePointDefaultRating() {
        DispatchScore score = service.score(0, 4, null);

        assertThat(score.distanceScore()).isEqualByComparingTo("100.0000");
        assertThat(score.workloadScore()).isEqualByComparingTo("50.0000");
        assertThat(score.serviceScore()).isEqualByComparingTo("60.0000");
        assertThat(score.totalScore()).isEqualByComparingTo("72.5000");
    }
}
