package com.example.HonBam.freeboardapi.service;

import com.example.HonBam.auth.CustomUserDetails;
import com.example.HonBam.freeboardapi.dto.request.CommentModifyRequestDTO;
import com.example.HonBam.freeboardapi.dto.request.FreeboardCommentRequestDTO;
import com.example.HonBam.freeboardapi.dto.request.FreeboardRequestDTO;
import com.example.HonBam.freeboardapi.dto.response.FreeboardCommentResponseDTO;
import com.example.HonBam.freeboardapi.dto.response.FreeboardDetailResponseDTO;
import com.example.HonBam.freeboardapi.dto.response.FreeboardResponseDTO;
import com.example.HonBam.freeboardapi.entity.Freeboard;
import com.example.HonBam.freeboardapi.entity.FreeboardComment;
import com.example.HonBam.freeboardapi.repository.FreeboardCommentRepository;
import com.example.HonBam.freeboardapi.repository.FreeboardRepository;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FreeboardService {

    private final FreeboardRepository freeboardRepository;
    private final UserRepository userRepository;
    private final FreeboardCommentRepository freeboardCommentRepository;

    // 공통 헬퍼
    @Transactional(readOnly = true)
    private User currentUser(CustomUserDetails userDetails) {
        return userDetails.getUser();
    }

    @Transactional(readOnly = true)
    private String currentUserId(CustomUserDetails userDetails) {
        return userDetails.getUser().getUserName();
    }

    @Transactional(readOnly = true)
    private Freeboard getPostOrThrow(Long postId) {
        return freeboardRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException(postId + "번 게시물이 존재하지 않습니다."));
    }

    @Transactional(readOnly = true)
    private FreeboardComment getCommentOrThrow(Long commentId) {
        return freeboardCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException(commentId + "번 댓글이 존재하지 않습니다."));
    }

    private List<FreeboardCommentResponseDTO> commentDtoByPost(Long postId) {
        return freeboardCommentRepository.findCommentsWithWriterByPostId(postId);
    }

    private void ensurePostOwner(Long postId, String userId) {
        if (!freeboardRepository.isPostOwner(postId, userId)) {
            throw new SecurityException("게시글의 소유자가 아닙니다.");
        }
    }

    private void ensureCommentOwner(Long commentId, String userId) {
        if (!freeboardCommentRepository.isCommentOwner(commentId, userId)) {
            throw new SecurityException("댓글의 소유자가 아닙니다.");
        }
    }

    // ----------- 비즈니스 -----------

    // 게시물 작성
    public FreeboardResponseDTO createContent(
            final FreeboardRequestDTO requestDto,
            final CustomUserDetails userDetails) {

        User user = currentUser(userDetails);
        freeboardRepository.save(requestDto.toEntity(user));
        return retrieve();
    }

    // 게시글 상세보기
    @Transactional(readOnly = true)
    public FreeboardDetailResponseDTO getContent(Long id) {
        return new FreeboardDetailResponseDTO(getPostOrThrow(id));
    }

    // 게시글 리스트
    @Transactional(readOnly = true)
    public FreeboardResponseDTO retrieve() {
        List<FreeboardDetailResponseDTO> dtoList = freeboardRepository.findAll()
                .stream().map(FreeboardDetailResponseDTO::new)
                .collect(Collectors.toList());

        return FreeboardResponseDTO.builder()
                .count(dtoList.size())
                .posts(dtoList)
                .build();
    }


    // 게시글 삭제
    public FreeboardResponseDTO delete(final String userId, final Long postId) {
        int affected = freeboardRepository.deleteByPostIdAndOwner(postId, userId);
        if (affected == 0) throw new SecurityException("삭제 권한이 없음 또는 대상 없음");
        return retrieve();
    }


    // 게시글 수정하기
    public FreeboardDetailResponseDTO modify(CustomUserDetails userDetails,
                                             Long postId,
                                             FreeboardRequestDTO requestDTO) {
        User user = currentUser(userDetails);
        ensureCommentOwner(postId, user.getId());

        Freeboard found = getPostOrThrow(postId);
        Freeboard entity = requestDTO.toEntity(found, user);
        entity.setUpdateDate(LocalDateTime.now());
        Freeboard saved = freeboardRepository.save(entity);
        return new FreeboardDetailResponseDTO(saved);
    }

    // 댓글 서비스 시작

    // 댓글 등록
    public List<FreeboardCommentResponseDTO> commentRegist(
            final FreeboardCommentRequestDTO dto,
            final CustomUserDetails userDetails
    ) {

        User user = currentUser(userDetails);
        Freeboard post = getPostOrThrow(dto.getId());
        freeboardCommentRepository.save(dto.toEntity(user, post));
        return commentDtoByPost(post.getId());
    }

    // 댓글 목록 요청
    @Transactional(readOnly = true)
    public List<FreeboardCommentResponseDTO> commentList(Long postId) {
        return commentDtoByPost(postId);
    }


    // 댓글 유효성 검사
    // 삭제요청
    public List<FreeboardCommentResponseDTO> commentDelete(CustomUserDetails userDetails, Long commentId) {
        String userId = currentUserId(userDetails);
        Long postId = getCommentOrThrow(commentId).getFreeboard().getId();

        int affected = freeboardRepository.deleteByPostIdAndOwner(commentId, userId);
        if (affected == 0) throw new SecurityException("삭제 권한 없음 또는 대상 없음");

        return commentDtoByPost(postId);
    }


    // 댓글 수정
    public List<FreeboardCommentResponseDTO> modify(CommentModifyRequestDTO requestDTO,
                                                    CustomUserDetails userDetails) {
        String userId = currentUserId(userDetails);
        ensureCommentOwner(requestDTO.getId(), userId);

        FreeboardComment comment = getCommentOrThrow(requestDTO.getId());
        comment.setComment(requestDTO.getComment());
        comment.setUpdateTime(LocalDateTime.now());
        freeboardCommentRepository.save(comment);

        return commentDtoByPost(comment.getFreeboard().getId());
    }

    // 작성자 검증(분리버전)
    @Transactional(readOnly = true)
    public boolean validatePostWriter(CustomUserDetails userDetails, Long postId) {
        return freeboardRepository.isPostOwner(postId, currentUserId(userDetails));
    }

    @Transactional(readOnly = true)
    public boolean validateCommentWriter(CustomUserDetails userDetails, Long commentId) {
        return freeboardCommentRepository.isCommentOwner(commentId, currentUserId(userDetails));
    }
}
