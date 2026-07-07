package com.example.HonBam.snsapi.service;

import com.example.HonBam.exception.CommentNotFoundException;
import com.example.HonBam.exception.InvalidCommentException;
import com.example.HonBam.exception.SnsAccessDeniedException;
import com.example.HonBam.snsapi.dto.request.CommentCreateRequestDTO;
import com.example.HonBam.snsapi.dto.request.CommentUpdateRequestDTO;
import com.example.HonBam.snsapi.dto.response.CommentResponseDTO;
import com.example.HonBam.snsapi.entity.Comment;
import com.example.HonBam.snsapi.repository.CommentRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import com.example.HonBam.snsapi.service.support.AuthorProfileResolver;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock AuthorProfileResolver authorProfileResolver;

    @InjectMocks CommentService commentService;

    private Comment comment(Long id, Long postId, String authorId, Long parentId, boolean deleted) {
        return Comment.builder()
                .id(id)
                .postId(postId)
                .authorId(authorId)
                .parentId(parentId)
                .content("원본 내용")
                .deleted(deleted)
                .build();
    }

    @Test
    @DisplayName("대댓글이 있는 부모 댓글 삭제 → soft delete (행 유지, 카운트 1 감소)")
    void deleteCommentWithRepliesSoftDeletes() {
        Comment target = comment(10L, 1L, "user-1", null, false);
        given(commentRepository.findByIdAndPostId(10L, 1L)).willReturn(Optional.of(target));
        given(commentRepository.existsByParentId(10L)).willReturn(true);

        commentService.deleteComment("user-1", 1L, 10L);

        assertThat(target.isDeleted()).isTrue();
        then(commentRepository).should(never()).delete(any(Comment.class));
        then(postRepository).should().decreaseCommentCount(1L);
    }

    @Test
    @DisplayName("대댓글이 없는 댓글 삭제 → hard delete")
    void deleteCommentWithoutRepliesHardDeletes() {
        Comment target = comment(10L, 1L, "user-1", null, false);
        given(commentRepository.findByIdAndPostId(10L, 1L)).willReturn(Optional.of(target));
        given(commentRepository.existsByParentId(10L)).willReturn(false);

        commentService.deleteComment("user-1", 1L, 10L);

        then(commentRepository).should().delete(target);
        then(postRepository).should().decreaseCommentCount(1L);
    }

    @Test
    @DisplayName("이미 삭제된 댓글 재삭제 → CommentNotFoundException, 카운트 감소 없음")
    void deleteAlreadyDeletedCommentThrows() {
        Comment target = comment(10L, 1L, "user-1", null, true);
        given(commentRepository.findByIdAndPostId(10L, 1L)).willReturn(Optional.of(target));

        assertThatThrownBy(() -> commentService.deleteComment("user-1", 1L, 10L))
                .isInstanceOf(CommentNotFoundException.class);

        then(postRepository).should(never()).decreaseCommentCount(any());
    }

    @Test
    @DisplayName("타인 댓글 삭제 → SnsAccessDeniedException")
    void deleteOthersCommentThrows() {
        Comment target = comment(10L, 1L, "user-1", null, false);
        given(commentRepository.findByIdAndPostId(10L, 1L)).willReturn(Optional.of(target));

        assertThatThrownBy(() -> commentService.deleteComment("user-2", 1L, 10L))
                .isInstanceOf(SnsAccessDeniedException.class);

        then(commentRepository).should(never()).delete(any(Comment.class));
    }

    @Test
    @DisplayName("URL의 postId와 다른 게시글의 댓글 삭제 → CommentNotFoundException")
    void deleteCommentWithMismatchedPostIdThrows() {
        given(commentRepository.findByIdAndPostId(10L, 999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment("user-1", 999L, 10L))
                .isInstanceOf(CommentNotFoundException.class);
    }

    @Test
    @DisplayName("삭제된 댓글에 답글 작성 → InvalidCommentException")
    void replyToDeletedCommentThrows() {
        Comment parent = comment(10L, 1L, "user-1", null, true);
        given(commentRepository.findByIdAndPostId(10L, 1L)).willReturn(Optional.of(parent));
        CommentCreateRequestDTO request = CommentCreateRequestDTO.builder()
                .content("답글").parentId(10L).build();

        assertThatThrownBy(() -> commentService.createComment("user-2", 1L, request))
                .isInstanceOf(InvalidCommentException.class);

        then(commentRepository).should(never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("대댓글에 답글 작성 → InvalidCommentException")
    void replyToReplyThrows() {
        Comment parent = comment(11L, 1L, "user-1", 10L, false);
        given(commentRepository.findByIdAndPostId(11L, 1L)).willReturn(Optional.of(parent));
        CommentCreateRequestDTO request = CommentCreateRequestDTO.builder()
                .content("답글").parentId(11L).build();

        assertThatThrownBy(() -> commentService.createComment("user-2", 1L, request))
                .isInstanceOf(InvalidCommentException.class);
    }

    @Test
    @DisplayName("삭제된 댓글 수정 → InvalidCommentException")
    void updateDeletedCommentThrows() {
        Comment target = comment(10L, 1L, "user-1", null, true);
        given(commentRepository.findByIdAndPostId(10L, 1L)).willReturn(Optional.of(target));
        CommentUpdateRequestDTO request = CommentUpdateRequestDTO.builder().content("수정").build();

        assertThatThrownBy(() -> commentService.updateComment("user-1", 1L, 10L, request))
                .isInstanceOf(InvalidCommentException.class);
    }

    @Test
    @DisplayName("타인 댓글 수정 → SnsAccessDeniedException")
    void updateOthersCommentThrows() {
        Comment target = comment(10L, 1L, "user-1", null, false);
        given(commentRepository.findByIdAndPostId(10L, 1L)).willReturn(Optional.of(target));
        CommentUpdateRequestDTO request = CommentUpdateRequestDTO.builder().content("수정").build();

        assertThatThrownBy(() -> commentService.updateComment("user-2", 1L, 10L, request))
                .isInstanceOf(SnsAccessDeniedException.class);
    }

    @Test
    @DisplayName("목록 조회 시 soft delete된 부모 댓글은 대체 문구로 표시되고 대댓글은 유지")
    void getCommentsMasksDeletedParent() {
        Comment deletedParent = comment(10L, 1L, "user-1", null, true);
        Comment child = comment(11L, 1L, "user-2", 10L, false);
        given(commentRepository.findByPostIdOrderByCreatedAtAsc(1L))
                .willReturn(List.of(deletedParent, child));
        given(userRepository.findAllById(any()))
                .willReturn(List.of(
                        User.builder().id("user-1").nickname("작성자1").build(),
                        User.builder().id("user-2").nickname("작성자2").build()
                ));
        given(authorProfileResolver.resolveUrlMap(anyCollection())).willReturn(Map.of());

        List<CommentResponseDTO> roots = commentService.getComments(1L);

        assertThat(roots).hasSize(1);
        CommentResponseDTO root = roots.get(0);
        assertThat(root.isDeleted()).isTrue();
        assertThat(root.getContent()).isEqualTo("삭제된 댓글입니다.");
        assertThat(root.getChildren()).hasSize(1);
        assertThat(root.getChildren().get(0).getContent()).isEqualTo("원본 내용");
    }
}
