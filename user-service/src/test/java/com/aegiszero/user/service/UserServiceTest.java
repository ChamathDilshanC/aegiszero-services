package com.aegiszero.user.service;

import com.aegiszero.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserRepository.search's "? IS NULL" JPQL clause can't take a null bind
 * parameter on Postgres — see UserRepository for the empirically-confirmed
 * failure (Postgres infers an untyped null as bytea, so lower(bytea) has no
 * overload and every plain page load 500'd). This locks down the contract
 * that made the query safe to simplify: UserService must never pass null.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository);
    }

    @Test
    void search_withNoQuery_passesEmptyStringNotNull() {
        Pageable pageable = PageRequest.of(0, 25);
        when(userRepository.search(any(), eq(pageable))).thenReturn(Page.empty(pageable));

        service.search(null, pageable);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(userRepository).search(captor.capture(), eq(pageable));
        assertThat(captor.getValue()).isNotNull().isEmpty();
    }

    @Test
    void search_withBlankQuery_normalizesToEmptyString() {
        Pageable pageable = PageRequest.of(0, 25);
        when(userRepository.search(any(), eq(pageable))).thenReturn(Page.empty(pageable));

        service.search("   ", pageable);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(userRepository).search(captor.capture(), eq(pageable));
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void search_withRealTerm_passesItTrimmed() {
        Pageable pageable = PageRequest.of(0, 25);
        when(userRepository.search(any(), eq(pageable))).thenReturn(Page.empty(pageable));

        service.search("  alice  ", pageable);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(userRepository).search(captor.capture(), eq(pageable));
        assertThat(captor.getValue()).isEqualTo("alice");
    }
}
