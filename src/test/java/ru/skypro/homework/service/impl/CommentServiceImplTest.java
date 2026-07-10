package ru.skypro.homework.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ru.skypro.homework.comment.Comment;
import ru.skypro.homework.comment.Comments;
import ru.skypro.homework.comment.CreateOrUpdateComment;
import ru.skypro.homework.entity.AdEntity;
import ru.skypro.homework.entity.CommentEntity;
import ru.skypro.homework.entity.UserEntity;
import ru.skypro.homework.mapper.CommentMapper;
import ru.skypro.homework.repository.AdRepository;
import ru.skypro.homework.repository.CommentRepository;
import ru.skypro.homework.repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

    //  Unit-тесты для CommentServiceImpl

class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private AdRepository adRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Authentication authentication;
    private UserEntity user;
    private CommentEntity commentEntity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new UserEntity();
        user.setId(1L);
        user.setUsername("testuser");

        commentEntity = new CommentEntity();
        commentEntity.setId(1L);
        commentEntity.setText("Тестовый комментарий");

        authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void getCommentsForAd_shouldReturnCommentsList() {
        // Given
        CommentEntity comment1 = createCommentEntity(1, "Комментарий 1");
        CommentEntity comment2 = createCommentEntity(2, "Комментарий 2");

        List<CommentEntity> comments = List.of(comment1, comment2);
        given(commentRepository.findByAdId(1L)).willReturn(comments);

        Comment commentDto1 = new Comment();
        commentDto1.setPk(1);
        Comment commentDto2 = new Comment();
        commentDto2.setPk(2);
        given(commentMapper.entityToDto(comment1)).willReturn(commentDto1);
        given(commentMapper.entityToDto(comment2)).willReturn(commentDto2);

        // When
        Comments result = commentService.getCommentsForAd(1L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getCount());
        assertEquals(2, result.getResults().size());
    }

    @Test
    void getCommentsForAd_shouldReturnEmptyListWhenNoComments() {
        // Given
        given(commentRepository.findByAdId(999L)).willReturn(Collections.emptyList());

        // When
        Comments result = commentService.getCommentsForAd(999L);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCount());
    }

    @Test
    void getMyAds_shouldReturnUserComments() {
        // Given
        CommentEntity comment1 = createCommentEntity(1, "Комментарий 1");
        CommentEntity comment2 = createCommentEntity(2, "Комментарий 2");

        Page<CommentEntity> page = new PageImpl<>(List.of(comment1, comment2), PageRequest.of(0, 10), 2);
        given(commentRepository.findByAuthorId(1L, any(PageRequest.class))).willReturn(page);

        Comment commentDto1 = new Comment();
        commentDto1.setPk(1);
        Comment commentDto2 = new Comment();
        commentDto2.setPk(2);
        given(commentMapper.entityToDto(comment1)).willReturn(commentDto1);
        given(commentMapper.entityToDto(comment2)).willReturn(commentDto2);

        // When
        Comments result = commentService.getMyAds(authentication);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getCount());
    }

    @Test
    void createComment_shouldSaveComment() {
        // Given
        CreateOrUpdateComment createComment = new CreateOrUpdateComment();
        createComment.setText("Новый комментарий");

        AdEntity ad = new AdEntity();
        ad.setId(1L);

        CommentEntity savedComment = new CommentEntity();
        savedComment.setId(1L);
        savedComment.setText("Новый комментарий");
        savedComment.setAuthor(user);
        savedComment.setAd(ad);

        given(adRepository.findById(1L)).willReturn(Optional.of(ad));
        given(commentMapper.dtoToEntity(createComment)).willReturn(commentEntity);
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(commentRepository.save(commentEntity)).willReturn(savedComment);
        given(commentMapper.entityToDto(savedComment)).willReturn(new Comment());

        // When
        Comment result = commentService.createComment(1L, createComment, authentication);

        // Then
        assertNotNull(result);
        verify(commentRepository, times(1)).save(commentEntity);
    }

    @Test
    void createComment_shouldThrowExceptionWhenAdNotFound() {
        // Given
        CreateOrUpdateComment createComment = new CreateOrUpdateComment();
        given(adRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            commentService.createComment(999L, createComment, authentication);
        });
    }

    @Test
    void updateComment_shouldUpdateComment() {
        // Given
        CreateOrUpdateComment updateComment = new CreateOrUpdateComment();
        updateComment.setText("Обновленный комментарий");

        CommentEntity existingComment = createCommentEntity(1, "Старый комментарий");
        existingComment.setAuthor(user);

        CommentEntity updatedComment = createCommentEntity(1, "Обновленный комментарий");
        updatedComment.setAuthor(user);

        given(commentRepository.findById(1L)).willReturn(Optional.of(existingComment));
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(commentRepository.save(existingComment)).willReturn(updatedComment);
        given(commentMapper.entityToDto(updatedComment)).willReturn(new Comment());

        // When
        Comment result = commentService.updateComment(1L, 1L, updateComment, authentication);

        // Then
        assertNotNull(result);
        assertEquals("Обновленный комментарий", existingComment.getText());
    }

    @Test
    void updateComment_shouldThrowExceptionWhenCommentNotFound() {
        // Given
        CreateOrUpdateComment updateComment = new CreateOrUpdateComment();
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            commentService.updateComment(1L, 999L, updateComment, authentication);
        });
    }

    @Test
    void updateComment_shouldAllowAdminToUpdateAnyComment() {
        // Given
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        CreateOrUpdateComment updateComment = new CreateOrUpdateComment();
        updateComment.setText("Обновленный комментарий");

        CommentEntity existingComment = createCommentEntity(1, "Старый комментарий");
        UserEntity otherUser = new UserEntity();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        existingComment.setAuthor(otherUser);

        CommentEntity updatedComment = createCommentEntity(1, "Обновленный комментарий");
        updatedComment.setAuthor(otherUser);

        given(commentRepository.findById(1L)).willReturn(Optional.of(existingComment));
        given(commentRepository.save(existingComment)).willReturn(updatedComment);
        given(commentMapper.entityToDto(updatedComment)).willReturn(new Comment());

        // When
        Comment result = commentService.updateComment(1L, 1L, updateComment, adminAuth);

        // Then
        assertNotNull(result);
        verify(commentRepository, times(1)).save(existingComment);
    }

    @Test
    void updateComment_shouldPreventUserToUpdateOtherUsersComment() {
        // Given
        CommentEntity existingComment = createCommentEntity(1, "Чужой комментарий");
        UserEntity otherUser = new UserEntity();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        existingComment.setAuthor(otherUser);

        given(commentRepository.findById(1L)).willReturn(Optional.of(existingComment));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            commentService.updateComment(1L, 1L, new CreateOrUpdateComment(), authentication);
        });
    }

    @Test
    void deleteComment_shouldDeleteComment() {
        // Given
        CommentEntity comment = createCommentEntity(1, "Удаляемый комментарий");
        comment.setAuthor(user);

        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        // When
        commentService.deleteComment(1L, 1L, authentication);

        // Then
        verify(commentRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteComment_shouldThrowExceptionWhenCommentNotFound() {
        // Given
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            commentService.deleteComment(1L, 999L, authentication);
        });
    }

    @Test
    void deleteComment_shouldAllowAdminToDeleteAnyComment() {
        // Given
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        CommentEntity comment = createCommentEntity(1, "Удаляемый комментарий");
        UserEntity otherUser = new UserEntity();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        comment.setAuthor(otherUser);

        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        // When
        commentService.deleteComment(1L, 1L, adminAuth);

        // Then
        verify(commentRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteComment_shouldPreventUserToDeleteOtherUsersComment() {
        // Given
        CommentEntity comment = createCommentEntity(1, "Чужой комментарий");
        UserEntity otherUser = new UserEntity();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        comment.setAuthor(otherUser);

        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            commentService.deleteComment(1L, 1L, authentication);
        });
    }

    // Helper methods
    private CommentEntity createCommentEntity(int id, String text) {
        CommentEntity entity = new CommentEntity();
        entity.setId((long) id);
        entity.setText(text);
        
        UserEntity author = new UserEntity();
        author.setId(1L);
        author.setUsername("testuser");
        entity.setAuthor(author);
        
        AdEntity ad = new AdEntity();
        ad.setId(1L);
        entity.setAd(ad);
        
        return entity;
    }
}
