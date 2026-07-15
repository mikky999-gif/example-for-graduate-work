package ru.skypro.homework.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.skypro.homework.ad.Ad;
import ru.skypro.homework.ad.CreateOrUpdateAd;
import ru.skypro.homework.ad.ExtendedAd;
import ru.skypro.homework.comment.Comment;
import ru.skypro.homework.comment.CreateOrUpdateComment;
import ru.skypro.homework.entity.AdEntity;
import ru.skypro.homework.entity.CommentEntity;
import ru.skypro.homework.entity.UserEntity;
import ru.skypro.homework.user.UpdateUser;
import ru.skypro.homework.user.User;

import static org.junit.jupiter.api.Assertions.*;

    // Unit-тесты для мапперов

class MapperTest {

    private AdMapper adMapper;
    private CommentMapper commentMapper;
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        adMapper = Mappers.getMapper(AdMapper.class);
        commentMapper = Mappers.getMapper(CommentMapper.class);
        userMapper = Mappers.getMapper(UserMapper.class);
    }

    @Test
    void adMapper_entityToDto_shouldMapCorrectly() {
        // Given
        AdEntity entity = new AdEntity();
        entity.setId(1L);
        entity.setTitle("Тестовое объявление");
        entity.setPrice(1000);
        entity.setImageUrl("/api/images/test.jpg");

        UserEntity author = new UserEntity();
        author.setId(1L);
        entity.setAuthor(author);

        // When
        Ad ad = adMapper.entityToDto(entity);

        // Then
        assertNotNull(ad);
        assertEquals(1, ad.getPk());
        assertEquals("Тестовое объявление", ad.getTitle());
        assertEquals(1000, ad.getPrice());
        assertEquals("/api/images/test.jpg", ad.getImage());
        assertEquals(1, ad.getAuthor());
    }

    @Test
    void adMapper_entityToExtendedDto_shouldMapCorrectly() {
        // Given
        AdEntity entity = new AdEntity();
        entity.setId(1L);
        entity.setTitle("Тестовое объявление");
        entity.setPrice(1000);
        entity.setDescription("Описание");
        entity.setImageUrl("/api/images/test.jpg");

        UserEntity author = new UserEntity();
        author.setId(1L);
        author.setFirstName("Иван");
        author.setLastName("Иванов");
        author.setUsername("user@example.com");
        author.setPhone("+79000000000");
        entity.setAuthor(author);

        // When
        ExtendedAd extendedAd = adMapper.entityToExtendedDto(entity);

        // Then
        assertNotNull(extendedAd);
        assertEquals(1, extendedAd.getPk());
        assertEquals("Тестовое объявление", extendedAd.getTitle());
        assertEquals(1000, extendedAd.getPrice());
        assertEquals("Описание", extendedAd.getDescription());
        assertEquals("Иван", extendedAd.getAuthorFirstName());
        assertEquals("Иванов", extendedAd.getAuthorLastName());
        assertEquals("user@example.com", extendedAd.getEmail());
        assertEquals("+79000000000", extendedAd.getPhone());
    }

    @Test
    void adMapperDtoToEntity_shouldMapCorrectly() {
        // Given
        CreateOrUpdateAd dto = new CreateOrUpdateAd();
        dto.setTitle("Новое объявление");
        dto.setDescription("Описание");
        dto.setPrice(5000);

        // When
        AdEntity entity = adMapper.dtoToEntity(dto);

        // Then
        assertNotNull(entity);
        assertEquals("Новое объявление", entity.getTitle());
        assertEquals("Описание", entity.getDescription());
        assertEquals(5000, entity.getPrice());
        assertNull(entity.getId());
        assertNull(entity.getAuthor());
        assertNull(entity.getImageUrl());
    }

    @Test
    void adMapper_updateFromDto_shouldUpdateEntity() {
        // Given
        AdEntity entity = new AdEntity();
        entity.setTitle("Старое название");
        entity.setPrice(1000);
        entity.setDescription("Старое описание");

        CreateOrUpdateAd dto = new CreateOrUpdateAd();
        dto.setTitle("Новое название");
        dto.setDescription("Новое описание");
        dto.setPrice(2000);

        // When
        adMapper.updateFromDto(dto, entity);

        // Then
        assertEquals("Новое название", entity.getTitle());
        assertEquals("Новое описание", entity.getDescription());
        assertEquals(2000, entity.getPrice());
    }

    @Test
    void commentMapper_entityToDto_shouldMapCorrectly() {
        // Given
        CommentEntity entity = new CommentEntity();
        entity.setId(1L);
        entity.setText("Тестовый комментарий");
        entity.setCreatedAt(1704067200000L);

        UserEntity author = new UserEntity();
        author.setId(1L);
        author.setFirstName("Иван");
        author.setImageUrl("/api/images/avatar.jpg");
        entity.setAuthor(author);

        // When
        Comment comment = commentMapper.entityToDto(entity);

        // Then
        assertNotNull(comment);
        assertEquals(1, comment.getPk());
        assertEquals("Тестовый комментарий", comment.getText());
        assertEquals(1704067200000L, comment.getCreatedAt());
        assertEquals(1, comment.getAuthor());
        assertEquals("Иван", comment.getAuthorFirstName());
        assertEquals("/api/images/avatar.jpg", comment.getAuthorImage());
    }

    @Test
    void commentMapperDtoToEntity_shouldMapCorrectly() {
        // Given
        CreateOrUpdateComment dto = new CreateOrUpdateComment();
        dto.setText("Новый комментарий");

        // When
        CommentEntity entity = commentMapper.dtoToEntity(dto);

        // Then
        assertNotNull(entity);
        assertEquals("Новый комментарий", entity.getText());
        assertNull(entity.getId());
        assertNull(entity.getAuthor());
        assertNull(entity.getAd());
        assertNull(entity.getCreatedAt());
    }

    @Test
    void commentMapper_initCreatedAt_shouldSetCurrentTime() {
        // Given
        CommentEntity entity = new CommentEntity();
        entity.setText("Тест");

        // When
        commentMapper.dtoToEntity(new CreateOrUpdateComment());
        // Note: The @AfterMapping annotation will set createdAt

        // Then
        assertNotNull(entity.getCreatedAt());
        // Should be close to current time
        long currentTime = System.currentTimeMillis();
        assertTrue(entity.getCreatedAt() <= currentTime);
    }

    @Test
    void commentMapper_updateFromDto_shouldUpdateEntity() {
        // Given
        CommentEntity entity = new CommentEntity();
        entity.setText("Старый текст");

        CreateOrUpdateComment dto = new CreateOrUpdateComment();
        dto.setText("Новый текст");

        // When
        commentMapper.updateFromDto(dto, entity);

        // Then
        assertEquals("Новый текст", entity.getText());
    }

    @Test
    void userMapper_entityToDto_shouldMapCorrectly() {
        // Given
        UserEntity entity = new UserEntity();
        entity.setId(1L);
        entity.setUsername("user@example.com");
        entity.setFirstName("Иван");
        entity.setLastName("Иванов");
        entity.setPhone("+79000000000");
        entity.setRole(ru.skypro.homework.dto.Role.USER);
        entity.setImageUrl("/api/images/avatar.jpg");

        // When
        User user = userMapper.entityToDto(entity);

        // Then
        assertNotNull(user);
        assertEquals("user@example.com", user.getEmail());
        assertEquals("Иван", user.getFirstName());
        assertEquals("Иванов", user.getLastName());
        assertEquals("+79000000000", user.getPhone());
        assertEquals(ru.skypro.homework.dto.Role.USER, user.getRole());
        assertEquals("/api/images/avatar.jpg", user.getImage());
    }

    @Test
    void userMapperDtoToEntity_shouldMapCorrectly() {
        // Given
        ru.skypro.homework.dto.Register dto = new ru.skypro.homework.dto.Register();
        dto.setUsername("user@example.com");
        dto.setFirstName("Иван");
        dto.setLastName("Иванов");
        dto.setPhone("+79000000000");
        dto.setRole(ru.skypro.homework.dto.Role.USER);

        // When
        UserEntity entity = userMapper.dtoToEntity(dto);

        // Then
        assertNotNull(entity);
        assertEquals("user@example.com", entity.getUsername());
        assertEquals("Иван", entity.getFirstName());
        assertEquals("Иванов", entity.getLastName());
        assertEquals("+79000000000", entity.getPhone());
        assertEquals(ru.skypro.homework.dto.Role.USER, entity.getRole());
        assertEquals("", entity.getImageUrl());
    }

    @Test
    void userMapper_updateFromDto_shouldUpdateEntity() {
        // Given
        UserEntity entity = new UserEntity();
        entity.setFirstName("Старое имя");
        entity.setLastName("Старая фамилия");
        entity.setPhone("000");

        UpdateUser dto = new UpdateUser();
        dto.setFirstName("Новое имя");
        dto.setLastName("Новая фамилия");
        dto.setPhone("+79001111111");

        // When
        userMapper.updateFromDto(dto, entity);

        // Then
        assertEquals("Новое имя", entity.getFirstName());
        assertEquals("Новая фамилия", entity.getLastName());
        assertEquals("+79001111111", entity.getPhone());
    }
}
