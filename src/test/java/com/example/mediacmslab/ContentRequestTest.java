package com.example.mediacmslab;

import com.example.mediacmslab.dto.ContentRequest;
import com.example.mediacmslab.entity.Content.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContentRequestTest {
    @Test
    void podcastRequiresAnHttpUrlAndEpisodes() {
        var request = ContentRequest.builder().type(ContentType.PODCAST).episodes(List.of("Первый")).build();
        for (String invalid : List.of("file:///tmp/clip", "без ссылки", "https:/audio")) {
            request.setMediaUrl(invalid);
            assertThat(request.isFormatValid()).isFalse();
        }
        request.setMediaUrl("https://example.org/podcast.mp3");
        assertThat(request.isFormatValid()).isTrue();
        request.setEpisodes(List.of());
        assertThat(request.isFormatValid()).isFalse();
    }

    @Test
    void videoRequiresPositiveDurationAndArticleRequiresText() {
        var request = ContentRequest.builder().type(ContentType.VIDEO).mediaUrl("https://example.org/v").build();
        request.setDurationSeconds(0);
        assertThat(request.isFormatValid()).isFalse();
        request.setType(ContentType.ARTICLE);
        request.setBody("  ");
        assertThat(request.isFormatValid()).isFalse();
        request.setBody("Текст");
        assertThat(request.isFormatValid()).isTrue();
    }
}
