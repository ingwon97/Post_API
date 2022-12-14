package com.example.postapi.repository;

import com.example.postapi.domain.Comment;
import com.example.postapi.domain.Member;
import com.example.postapi.domain.Reply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReplyRepository extends JpaRepository<Reply, Long> {
    List<Reply> findAllByComment(Comment comment);
    List<Reply> findAllByMember(Member member);
}
