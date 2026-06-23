package com.pmrodrigues.security.service;

import com.pmrodrigues.security.config.JwtProperties;
import com.pmrodrigues.security.model.User;
import com.pmrodrigues.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import com.pmrodrigues.condominio.model.Condominio;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtServiceTest {

    @Mock
    JwtEncoder jwtEncoder;

    @Mock
    JwtProperties jwtProperties;

    @Mock
    UserRepository userRepository;

    @Mock
    ProprietarioClaimsProvider proprietarioClaimsProvider;

    @Mock
    MoradorClaimsProvider moradorClaimsProvider;

    @InjectMocks
    JwtService service;

    @BeforeEach
    void setUp() {
        when(jwtProperties.getIssuer()).thenReturn("http://test-issuer");
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(3600L);

        var mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("signed-token");
        when(jwtEncoder.encode(any())).thenReturn(mockJwt);

        var condominio = new Condominio();
        condominio.setId(42L);
        var user = new User();
        user.setId(7L);
        user.setCondominios(Set.of(condominio));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        lenient().when(proprietarioClaimsProvider.findClaimsByEmail(anyString())).thenReturn(Optional.empty());
        lenient().when(moradorClaimsProvider.findApartamentoIdByEmail(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void generateAccessToken_returnsTokenValue() {
        var auth = authWith("user@test.com", "ROLE_USER");

        var token = service.generateAccessToken(auth);

        assertThat(token).isEqualTo("signed-token");
    }

    @Test
    void generateAccessToken_setsSubjectFromAuthentication() {
        var auth = authWith("user@test.com", "ROLE_USER");

        service.generateAccessToken(auth);

        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        assertThat(captor.getValue().getClaims().getSubject()).isEqualTo("user@test.com");
    }

    @Test
    void generateAccessToken_includesRolesClaimFromAuthorities() {
        var auth = authWith("user@test.com", "ROLE_USER", "ROLE_ADMIN");

        service.generateAccessToken(auth);

        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        assertThat((List<String>) captor.getValue().getClaims().getClaim("roles"))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void generateAccessToken_setsExpirationFromProperties() {
        var auth = authWith("user@test.com", "ROLE_USER");
        var before = Instant.now();

        service.generateAccessToken(auth);

        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        JwtClaimsSet claims = captor.getValue().getClaims();
        assertThat(claims.getExpiresAt()).isAfterOrEqualTo(before.plusSeconds(3599));
    }

    @Test
    void generateAccessToken_setsUniqueJtiOnEachCall() {
        var auth = authWith("user@test.com", "ROLE_USER");
        service.generateAccessToken(auth);
        service.generateAccessToken(auth);

        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder, times(2)).encode(captor.capture());

        var ids = captor.getAllValues().stream()
                .map(p -> p.getClaims().getId())
                .toList();
        assertThat(ids.get(0)).isNotEqualTo(ids.get(1));
    }

    @Test
    void generateAccessToken_setsConfiguredIssuer() {
        var auth = authWith("user@test.com", "ROLE_USER");

        service.generateAccessToken(auth);

        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        assertThat(captor.getValue().getClaims().getIssuer()).hasToString("http://test-issuer");
    }

    @Test
    void generateAccessToken_includesCondominioIdsClaim() {
        var auth = authWith("user@test.com", "ROLE_USER");

        service.generateAccessToken(auth);

        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        List<Long> claim = captor.getValue().getClaims().getClaim("condominio_ids");
        assertThat(claim).isNotNull().containsExactly(42L);
    }

    @Test
    void generateAccessToken_whenUserHasNoCondominio_condominioIdsClaimIsEmpty() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        var auth = authWith("user@test.com", "ROLE_USER");

        service.generateAccessToken(auth);

        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        List<Long> claim = captor.getValue().getClaims().getClaim("condominio_ids");
        assertThat(claim).isNotNull().isEmpty();
    }

    @Test
    void generateAccessToken_whenUserHasMultipleCondominios_allIdsIncluded() {
        var cond1 = new Condominio(); cond1.setId(10L);
        var cond2 = new Condominio(); cond2.setId(20L);
        var user = new User();
        user.setCondominios(Set.of(cond1, cond2));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        var auth = authWith("user@test.com", "ROLE_USER");
        service.generateAccessToken(auth);

        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        List<Long> claim = captor.getValue().getClaims().getClaim("condominio_ids");
        assertThat(claim).containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    void generateAccessToken_withNoAuthorities_includesEmptyRolesClaim() {
        var auth = new UsernamePasswordAuthenticationToken("user@test.com", null, List.of());

        service.generateAccessToken(auth);

        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        assertThat((List<String>) captor.getValue().getClaims().getClaim("roles")).isEmpty();
    }

    @Test
    void generateAccessToken_includesUserIdClaim() {
        var auth = authWith("user@test.com", "ROLE_USER");

        service.generateAccessToken(auth);

        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        Long claim = captor.getValue().getClaims().getClaim("user_id");
        assertThat(claim).isEqualTo(7L);
    }

    @Test
    void generateAccessToken_whenUserNotFound_userIdClaimAbsent() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        var auth = authWith("user@test.com", "ROLE_USER");

        service.generateAccessToken(auth);

        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        assertThat((Object) captor.getValue().getClaims().getClaim("user_id")).isNull();
    }

    @Test
    void generateAccessToken_whenProprietario_userIdClaimPresent() {
        var auth = authWith("owner@test.com", "ROLE_PROPRIETARIO");

        service.generateAccessToken(auth);

        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        Long userId = captor.getValue().getClaims().getClaim("user_id");
        assertThat(userId).isEqualTo(7L);
    }

    @Test
    void generateAccessToken_whenNotProprietario_doesNotCallClaimsProvider() {
        var auth = authWith("user@test.com", "ROLE_USER");

        service.generateAccessToken(auth);

        verify(proprietarioClaimsProvider, never()).findClaimsByEmail(anyString());
    }

    @Test
    void generateAccessToken_whenMorador_includesApartamentoIdClaim() {
        when(moradorClaimsProvider.findApartamentoIdByEmail("morador@test.com")).thenReturn(Optional.of(17L));
        var auth = authWith("morador@test.com", "ROLE_MORADOR");

        service.generateAccessToken(auth);

        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        Long apartamentoId = captor.getValue().getClaims().getClaim("apartamento_id");
        assertThat(apartamentoId).isEqualTo(17L);
    }

    @Test
    void generateAccessToken_whenMoradorHasNoApartamento_apartamentoIdClaimAbsent() {
        when(moradorClaimsProvider.findApartamentoIdByEmail("morador@test.com")).thenReturn(Optional.empty());
        var auth = authWith("morador@test.com", "ROLE_MORADOR");

        service.generateAccessToken(auth);

        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        assertThat((Object) captor.getValue().getClaims().getClaim("apartamento_id")).isNull();
    }

    @Test
    void generateAccessToken_whenNotMorador_doesNotCallMoradorClaimsProvider() {
        var auth = authWith("owner@test.com", "ROLE_PROPRIETARIO");

        service.generateAccessToken(auth);

        verify(moradorClaimsProvider, never()).findApartamentoIdByEmail(anyString());
    }

    @Test
    void generateRefreshToken_returnsNonNullString() {
        assertThat(service.generateRefreshToken()).isNotBlank();
    }

    @Test
    void generateRefreshToken_isUniqueOnEachCall() {
        assertThat(service.generateRefreshToken()).isNotEqualTo(service.generateRefreshToken());
    }

    private UsernamePasswordAuthenticationToken authWith(String email, String... roles) {
        var authorities = List.of(roles).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(email, null, authorities);
    }
}
