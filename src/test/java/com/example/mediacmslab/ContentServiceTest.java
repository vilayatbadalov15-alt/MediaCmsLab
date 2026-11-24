package com.example.mediacmslab;

import com.example.mediacmslab.entity.Content;
import com.example.mediacmslab.entity.User;
import com.example.mediacmslab.repository.ContentRepository;
import com.example.mediacmslab.repository.TagRepository;
import com.example.mediacmslab.repository.UserRepository;
import com.example.mediacmslab.service.ContentService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

class ContentServiceTest {
    private final ContentRepository repository = mock(ContentRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ContentService service = new ContentService(repository, mock(TagRepository.class),
            mock(UserRepository.class), events);
    private final Content content = Content.builder().id(1L)
            .author(User.builder().email("author@example.org").build()).build();

    @Test
    void otherEditorsCannotDeletePublication() {
        when(repository.findById(1L)).thenReturn(Optional.of(content));
        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> service.delete(1L, "other@example.org", false))
                .matches(e -> e.getStatusCode().equals(HttpStatus.FORBIDDEN));
        verify(repository, never()).delete(any(Content.class));
        verifyNoInteractions(events);
    }

    @Test
    void administratorCanDeleteAnotherAuthorsPublication() {
        when(repository.findById(1L)).thenReturn(Optional.of(content));
        service.delete(1L, "admin@example.org", true);
        verify(repository).delete(content);
    }
}
