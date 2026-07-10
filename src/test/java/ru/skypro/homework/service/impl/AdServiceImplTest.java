package ru.skypro.homework.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.core.env.Environment;
import ru.skypro.homework.ad.Ad;
import ru.skypro.homework.ad.Ads;
import ru.skypro.homework.ad.CreateOrUpdateAd;
import ru.skypro.homework.ad.ExtendedAd;
import ru.skypro.homework.entity.AdEntity;
import ru.skypro.homework.entity.UserEntity;
import ru.skypro.homework.exception.ResourceNotFoundException;
import ru.skypro.homework.mapper.AdMapper;
import ru.skypro.homework.repository.AdRepository;
import ru.skypro.homework.repository.UserRepository;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

    //  Unit-тесты для AdServiceImpl

class AdServiceImplTest {

    @Mock
    private AdRepository adRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdMapper adMapper;

    @Mock
    private Environment env;

    @InjectMocks
    private AdServiceImpl adService;

    private Authentication authentication;
    private UserEntity user;
    private AdEntity adEntity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        user = new UserEntity();
        user.setId(1L);
        user.setUsername("testuser");

        adEntity = new AdEntity();
        adEntity.setId(1L);
        adEntity.setTitle("Тестовое объявление");
        adEntity.setPrice(1000);

        authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // Настройка env
        given(env.getProperty("app.images.root-path")).willReturn("/tmp/uploads");
        given(env.getProperty("app.images.url-prefix")).willReturn("/api/images/");
    }

    @Test
    void getAllAds_shouldReturnPaginatedAds() {
        // Given
        Page<AdEntity> page = new PageImpl<>(List.of(adEntity), PageRequest.of(0, 10), 1);
        given(adRepository.findAll(any(PageRequest.class))).willReturn(page);

        Ad adDto = new Ad();
        adDto.setPk(1);
        given(adMapper.entityToDto(adEntity)).willReturn(adDto);

        // When
        Ads result = adService.getAllAds(0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getCount());
        assertEquals(1, result.getResults().size());
        assertEquals(1, result.getResults().get(0).getPk());
    }

    @Test
    void getAdById_shouldReturnOptionalWhenExists() {
        // Given
        ExtendedAd extendedAd = new ExtendedAd();
        extendedAd.setPk(1);
        given(adRepository.findById(1L)).willReturn(Optional.of(adEntity));
        given(adMapper.entityToExtendedDto(adEntity)).willReturn(extendedAd);

        // When
        Optional<ExtendedAd> result = adService.getAdById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getPk());
    }

    @Test
    void getAdById_shouldReturnEmptyOptionalWhenNotFound() {
        // Given
        given(adRepository.findById(999L)).willReturn(Optional.empty());

        // When
        Optional<ExtendedAd> result = adService.getAdById(999L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void getMyAds_shouldReturnUserAds() {
        // Given
        Page<AdEntity> page = new PageImpl<>(List.of(adEntity), PageRequest.of(0, 10), 1);
        given(adRepository.findByAuthorId(1L, any(PageRequest.class))).willReturn(page);

        Ad adDto = new Ad();
        adDto.setPk(1);
        given(adMapper.entityToDto(adEntity)).willReturn(adDto);

        // When
        Ads result = adService.getMyAds(authentication);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getCount());
    }

    @Test
    void createAd_shouldSaveAdWithImage() throws IOException {
        // Given
        CreateOrUpdateAd createAd = new CreateOrUpdateAd();
        createAd.setTitle("Новое объявление");
        createAd.setPrice(5000);
        createAd.setDescription("Описание");

        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());

        AdEntity savedEntity = new AdEntity();
        savedEntity.setId(1L);
        savedEntity.setTitle("Новое объявление");
        savedEntity.setPrice(5000);
        savedEntity.setImageUrl("/api/images/uuid.jpg");

        given(adMapper.dtoToEntity(createAd)).willReturn(adEntity);
        given(adMapper.entityToDto(adEntity)).willReturn(new Ad());
        given(adRepository.save(adEntity)).willReturn(savedEntity);
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));

        // When
        Ad result = adService.createAd(createAd, image, authentication);

        // Then
        assertNotNull(result);
        verify(adRepository, times(1)).save(adEntity);
    }

    @Test
    void createAd_shouldSaveAdWithoutImage() {
        // Given
        CreateOrUpdateAd createAd = new CreateOrUpdateAd();
        createAd.setTitle("Новое объявление");
        createAd.setPrice(5000);

        given(adMapper.dtoToEntity(createAd)).willReturn(adEntity);
        given(adRepository.save(adEntity)).willReturn(adEntity);
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));

        // When
        Ad result = adService.createAd(createAd, null, authentication);

        // Then
        assertNotNull(result);
        verify(adRepository, times(1)).save(adEntity);
    }

    @Test
    void updateImage_shouldUpdateImage() throws IOException {
        // Given
        MockMultipartFile image = new MockMultipartFile("image", "new.jpg", "image/jpeg", "content".getBytes());
        given(adRepository.findById(1L)).willReturn(Optional.of(adEntity));
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));

        // When
        adService.updateImage(1L, image, authentication);

        // Then
        assertEquals("/api/images/uuid.jpg", adEntity.getImageUrl());
        verify(adRepository, times(1)).save(adEntity);
    }

    @Test
    void updateImage_shouldThrowExceptionWhenAdNotFound() {
        // Given
        MockMultipartFile image = new MockMultipartFile("image", "new.jpg", "image/jpeg", "content".getBytes());
        given(adRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            adService.updateImage(999L, image, authentication);
        });
    }

    @Test
    void updateAd_shouldUpdateAd() {
        // Given
        CreateOrUpdateAd updateAd = new CreateOrUpdateAd();
        updateAd.setTitle("Обновленное объявление");
        updateAd.setPrice(6000);

        given(adRepository.findById(1L)).willReturn(Optional.of(adEntity));
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(adMapper.entityToDto(adEntity)).willReturn(new Ad());

        // When
        Ad result = adService.updateAd(1L, updateAd, authentication);

        // Then
        assertNotNull(result);
        assertEquals("Обновленное объявление", adEntity.getTitle());
    }

    @Test
    void updateAd_shouldThrowExceptionWhenAdNotFound() {
        // Given
        CreateOrUpdateAd updateAd = new CreateOrUpdateAd();
        given(adRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            adService.updateAd(999L, updateAd, authentication);
        });
    }

    @Test
    void deleteAd_shouldDeleteAd() {
        // When
        adService.deleteAd(1L);

        // Then
        verify(adRepository, times(1)).deleteById(1L);
    }

    @Test
    void isOwner_shouldReturnTrueWhenOwner() {
        // Given
        AdEntity ad = new AdEntity();
        ad.setId(1L);
        UserEntity author = new UserEntity();
        author.setUsername("testuser");
        ad.setAuthor(author);

        given(adRepository.findById(1L)).willReturn(Optional.of(ad));

        // When
        boolean result = adService.isOwner(1L, "testuser");

        // Then
        assertTrue(result);
    }

    @Test
    void isOwner_shouldReturnFalseWhenNotOwner() {
        // Given
        AdEntity ad = new AdEntity();
        ad.setId(1L);
        UserEntity author = new UserEntity();
        author.setUsername("otheruser");
        ad.setAuthor(author);

        given(adRepository.findById(1L)).willReturn(Optional.of(ad));

        // When
        boolean result = adService.isOwner(1L, "testuser");

        // Then
        assertFalse(result);
    }

    @Test
    void isOwner_shouldReturnFalseWhenUsernameIsNull() {
        // Given
        given(adRepository.findById(1L)).willReturn(Optional.of(adEntity));

        // When
        boolean result = adService.isOwner(1L, null);

        // Then
        assertFalse(result);
    }

    @Test
    void isOwner_shouldReturnFalseWhenUsernameIsEmpty() {
        // Given
        given(adRepository.findById(1L)).willReturn(Optional.of(adEntity));

        // When
        boolean result = adService.isOwner(1L, "");

        // Then
        assertFalse(result);
    }

    @Test
    void isOwner_shouldThrowExceptionWhenAdNotFound() {
        // Given
        given(adRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            adService.isOwner(999L, "testuser");
        });
    }
}
