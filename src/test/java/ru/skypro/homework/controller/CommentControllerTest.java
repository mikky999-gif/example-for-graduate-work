package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.skypro.homework.comment.Comment;
import ru.skypro.homework.comment.Comments;
import ru.skypro.homework.comment.CreateOrUpdateComment;
import ru.skypro.homework.service.CommentService;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

    //  Unit-тесты для CommentController

class CommentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    private ObjectMapper objectMapper;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(commentController).build();
        objectMapper = new ObjectMapper();

        authentication = new UsernamePasswordAuthenticationToken(
                "testUser",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void getComments_shouldReturnCommentsList() throws Exception {
        // Given
        Comment comment1 = createTestComment(1, "Отличный товар!", 1704067200000L, 1, null, "Иван");
        Comment comment2 = createTestComment(2, "Хорошее качество", 1704067300000L, 2, null, "Петр");

        Comments comments = new Comments();
        comments.setCount(2);
        comments.setResults(List.of(comment1, comment2));

        given(commentService.getCommentsForAd(1)).willReturn(comments);

        // When & Then
        mockMvc.perform(get("/ads/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.results", hasSize(2)))
                .andExpect(jsonPath("$.results[0].pk").value(1))
                .andExpect(jsonPath("$.results[0].text").value("Отличный товар!"));
    }

    @Test
    void getComments_shouldReturnEmptyListWhenNoComments() throws Exception {
        // Given
        Comments comments = new Comments();
        comments.setCount(0);
        comments.setResults(Collections.emptyList());

        given(commentService.getCommentsForAd(999)).willReturn(comments);

        // When & Then
        mockMvc.perform(get("/ads/999/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void createComment_shouldReturnCreatedCommentId() throws Exception {
        // Given
        CreateOrUpdateComment createComment = new CreateOrUpdateComment();
        createComment.setText("Отличный товар!");

        Comment createdComment = createTestComment(1, "Отличный товар!", 1704067200000L, 1, null, "Иван");

        given(commentService.createComment(eq(1L), eq(createComment), eq(authentication)))
                .willReturn(createdComment);

        // When & Then
        mockMvc.perform(post("/ads/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createComment))
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateComment_shouldReturnUpdatedComment() throws Exception {
        // Given
        CreateOrUpdateComment updateComment = new CreateOrUpdateComment();
        updateComment.setText("Обновленный комментарий");

        Comment updatedComment = createTestComment(1, "Обновленный комментарий", 1704067200000L, 1, null, "Иван");

        given(commentService.updateComment(eq(1L), eq(1L), eq(updateComment), eq(authentication)))
                .willReturn(updatedComment);

        // When & Then
        mockMvc.perform(patch("/ads/1/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateComment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pk").value(1))
                .andExpect(jsonPath("$.text").value("Обновленный комментарий"));
    }

    @Test
    void deleteComment_shouldReturnNoContent() throws Exception {
        // Given
        doNothing().when(commentService).deleteComment(1L, 1L, authentication);

        // When & Then
        mockMvc.perform(delete("/ads/1/comments/1"))
                .andExpect(status().isNoContent());
    }

    // Helper methods
    private Comment createTestComment(int pk, String text, Long createdAt, int author, String authorImage, String authorFirstName) {
        Comment comment = new Comment();
        comment.setPk(pk);
        comment.setText(text);
        comment.setCreatedAt(createdAt);
        comment.setAuthor(author);
        comment.setAuthorImage(authorImage);
        comment.setAuthorFirstName(authorFirstName);
        return comment;
    }
}
