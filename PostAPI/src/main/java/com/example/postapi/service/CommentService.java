package com.example.postapi.service;

import com.example.postapi.controller.request.CommentHeartRequestDto;
import com.example.postapi.controller.request.CommentRequestDto;
import com.example.postapi.controller.response.CommentResponseDto;
import com.example.postapi.controller.response.ReplyResponseDto;
import com.example.postapi.controller.response.ResponseDto;
import com.example.postapi.domain.*;
import com.example.postapi.jwt.TokenProvider;
import com.example.postapi.repository.CommentHeartRepository;
import com.example.postapi.repository.CommentRepository;
import com.example.postapi.repository.ReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final TokenProvider tokenProvider;
  private final PostService postService;
  private final ReplyRepository replyRepository;
  private final CommentHeartRepository commentHeartRepository;

  @Transactional
  public ResponseDto<?> createComment(CommentRequestDto requestDto, HttpServletRequest request) {
    if (null == request.getHeader("Refresh-Token")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
          "로그인이 필요합니다.");
    }

    if (null == request.getHeader("Authorization")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
          "로그인이 필요합니다.");
    }

    Member member = validateMember(request);
    if (null == member) {
      return ResponseDto.fail("INVALID_TOKEN", "Token이 유효하지 않습니다.");
    }

    Post post = postService.isPresentPost(requestDto.getPostId());
    if (null == post) {
      return ResponseDto.fail("NOT_FOUND", "존재하지 않는 게시글 id 입니다.");
    }

    Comment comment = Comment.builder()
            .member(member)
            .post(post)
            .content(requestDto.getContent())
            .herartCount(0L)
            .build();
    commentRepository.save(comment);

    System.out.println(post.getComments());

    return ResponseDto.success(
        CommentResponseDto.builder()
                .id(comment.getId())
                .author(comment.getMember().getNickname())
                .content(comment.getContent())
                .heartCount(comment.getHerartCount())
                .createdAt(comment.getCreatedAt())
                .modifiedAt(comment.getModifiedAt())
                .build()
    );
  }

  @Transactional(readOnly = true)
  public ResponseDto<?> getAllCommentsByPost(Long postId) {
    Post post = postService.isPresentPost(postId);
    if (null == post) {
      return ResponseDto.fail("NOT_FOUND", "존재하지 않는 게시글 id 입니다.");
    }

    List<Comment> commentList = commentRepository.findAllByPost(post);
    List<CommentResponseDto> commentResponseDtoList = new ArrayList<>();

    for (Comment comment : commentList) {
      List<Reply> replyList = replyRepository.findAllByComment(comment);
      List<ReplyResponseDto> replyResponseDtoList = new ArrayList<>();

      for (Reply reply : replyList){
        replyResponseDtoList.add(
                ReplyResponseDto.builder()
                        .id(reply.getId())
                        .author(reply.getMember().getNickname())
                        .content(reply.getContent())
                        .heartCount(reply.getHerartCount())
                        .createdAt(reply.getCreatedAt())
                        .modifiedAt(reply.getModifiedAt())
                        .build()
        );
      }
      commentResponseDtoList.add(
          CommentResponseDto.builder()
                  .id(comment.getId())
                  .author(comment.getMember().getNickname())
                  .content(comment.getContent())
                  .heartCount(comment.getHerartCount())
                  .replyResponseDtoList(replyResponseDtoList)
                  .createdAt(comment.getCreatedAt())
                  .modifiedAt(comment.getModifiedAt())
                  .build()
      );
    }
    return ResponseDto.success(commentResponseDtoList);
  }

  @Transactional
  public ResponseDto<?> updateComment(Long id, CommentRequestDto requestDto, HttpServletRequest request) {
    if (null == request.getHeader("Refresh-Token")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
          "로그인이 필요합니다.");
    }

    if (null == request.getHeader("Authorization")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
          "로그인이 필요합니다.");
    }

    Member member = validateMember(request);
    if (null == member) {
      return ResponseDto.fail("INVALID_TOKEN", "Token이 유효하지 않습니다.");
    }

    Post post = postService.isPresentPost(requestDto.getPostId());
    if (null == post) {
      return ResponseDto.fail("NOT_FOUND", "존재하지 않는 게시글 id 입니다.");
    }

    Comment comment = isPresentComment(id);
    if (null == comment) {
      return ResponseDto.fail("NOT_FOUND", "존재하지 않는 댓글 id 입니다.");
    }

    if (comment.validateMember(member)) {
      return ResponseDto.fail("BAD_REQUEST", "작성자만 수정할 수 있습니다.");
    }

    comment.update(requestDto);
    return ResponseDto.success(
        CommentResponseDto.builder()
                .id(comment.getId())
                .author(comment.getMember().getNickname())
                .content(comment.getContent())
                .heartCount(comment.getHerartCount())
                .createdAt(comment.getCreatedAt())
                .modifiedAt(comment.getModifiedAt())
                .build()
    );
  }

  @Transactional
  public ResponseDto<?> deleteComment(Long id, HttpServletRequest request) {
    if (null == request.getHeader("Refresh-Token")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
          "로그인이 필요합니다.");
    }

    if (null == request.getHeader("Authorization")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
          "로그인이 필요합니다.");
    }

    Member member = validateMember(request);
    if (null == member) {
      return ResponseDto.fail("INVALID_TOKEN", "Token이 유효하지 않습니다.");
    }

    Comment comment = isPresentComment(id);
    if (null == comment) {
      return ResponseDto.fail("NOT_FOUND", "존재하지 않는 댓글 id 입니다.");
    }

    if (comment.validateMember(member)) {
      return ResponseDto.fail("BAD_REQUEST", "작성자만 수정할 수 있습니다.");
    }


    commentRepository.delete(comment);
    return ResponseDto.success("success");
  }

  @Transactional
  public ResponseDto<?> addCommnetHeart(Long id, HttpServletRequest request){
    if (null == request.getHeader("Refresh-Token")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
              "로그인이 필요합니다.");
    }

    if (null == request.getHeader("Authorization")) {
      return ResponseDto.fail("MEMBER_NOT_FOUND",
              "로그인이 필요합니다.");
    }

    Member member = validateMember(request);
    if (null == member) {
      return ResponseDto.fail("INVALID_TOKEN", "Token이 유효하지 않습니다.");
    }

    Comment comment = isPresentComment(id);
    if (null == comment) {
      return ResponseDto.fail("NOT_FOUND", "존재하지 않는 댓글 id 입니다.");
    }

    CommentHeartRequestDto commentHeartRequestDto = new CommentHeartRequestDto(member, comment);

    //좋아요 상태에서 한번 더 요청을 보내면 취소
    if (commentHeartRepository.findCommentHeartByMemberAndComment(member, comment).isPresent()){
      commentHeartRepository.deleteByMemberAndComment(member, comment);
      comment.cancleHeart(commentHeartRequestDto);

      return ResponseDto.success("좋아요 취소!");
    }

    //좋아요를 한 적이 없으면 추가
    comment.addHeart(commentHeartRequestDto);
    CommentHeart commentHeart = new CommentHeart(commentHeartRequestDto);
    commentHeartRepository.save(commentHeart);

    return ResponseDto.success("좋아요 완료!");
  }

  @Transactional(readOnly = true)
  public Comment isPresentComment(Long id) {
    Optional<Comment> optionalComment = commentRepository.findById(id);
    return optionalComment.orElse(null);
  }

  @Transactional
  public Member validateMember(HttpServletRequest request) {
    if (!tokenProvider.validateToken(request.getHeader("Refresh-Token"))) {
      return null;
    }
    return tokenProvider.getMemberFromAuthentication();
  }
}
