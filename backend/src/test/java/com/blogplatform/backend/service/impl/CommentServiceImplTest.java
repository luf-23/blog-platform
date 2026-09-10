package com.blogplatform.backend.service.impl;

import com.blogplatform.backend.entity.Comment;
import com.blogplatform.backend.entity.CommentVO;
import com.blogplatform.backend.entity.User;
import com.blogplatform.backend.mapper.ArticleMapper;
import com.blogplatform.backend.mapper.CommentLikeMapper;
import com.blogplatform.backend.mapper.CommentMapper;
import com.blogplatform.backend.mapper.UserMapper;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {
    @Mock private CommentMapper comments;
    @Mock private ArticleMapper articles;
    @Mock private UserMapper users;
    @Mock private CommentLikeMapper likes;
    @InjectMocks private CommentServiceImpl service;

    @BeforeEach
    void login() {
        ThreadLocalUtil.set(Map.of("id", 9));
    }

    @AfterEach
    void clearLogin() {
        ThreadLocalUtil.remove();
    }

    @Test
    void rootHasNeitherParentNorRootReference() {
        assertEquals(0, service.add(1, "root", null, 99).getCode());
        Comment inserted = insertedComment();
        assertNull(inserted.getParentId());
        assertNull(inserted.getRootCommentId());
        assertNull(inserted.getReplyToUserId());
        verify(articles).incrementCommentCount(1);
    }

    @Test
    void replyToRootStoresBothReferencesAndUsesActualTargetUser() {
        when(comments.selectById(10)).thenReturn(comment(10, null, null));

        assertEquals(0, service.add(1, "reply", 10, 999).getCode());

        Comment inserted = insertedComment();
        assertEquals(10, inserted.getParentId());
        assertEquals(10, inserted.getRootCommentId());
        assertEquals(7, inserted.getReplyToUserId());
    }

    @Test
    void deepReplyKeepsItsImmediateParentAndInheritsRoot() {
        when(comments.selectById(30)).thenReturn(comment(30, 20, 10));
        when(comments.selectById(10)).thenReturn(comment(10, null, null));

        assertEquals(0, service.add(1, "deep reply", 30, null).getCode());

        Comment inserted = insertedComment();
        assertEquals(30, inserted.getParentId());
        assertEquals(10, inserted.getRootCommentId());
    }

    @Test
    void rejectsReplyToAnotherArticle() {
        Comment parent = comment(20, 10, 10);
        parent.setArticleId(2);
        when(comments.selectById(20)).thenReturn(parent);

        assertEquals(1, service.add(1, "reply", 20, null).getCode());
        verify(comments, never()).insert(any());
        verifyNoInteractions(articles);
    }

    @Test
    void rejectsDeletedParent() {
        Comment parent = comment(20, 10, 10);
        parent.setStatus(0);
        when(comments.selectById(20)).thenReturn(parent);

        assertEquals(1, service.add(1, "reply", 20, null).getCode());
        verify(comments, never()).insert(any());
    }

    @Test
    void rejectsReplyUnderDeletedRootEvenWhenParentIsVisible() {
        Comment root = comment(10, null, null);
        root.setStatus(0);
        when(comments.selectById(20)).thenReturn(comment(20, 10, 10));
        when(comments.selectById(10)).thenReturn(root);

        assertEquals(1, service.add(1, "reply", 20, null).getCode());
        verify(comments, never()).insert(any());
        verifyNoInteractions(articles);
    }

    @Test
    void rejectsUnmigratedReplyInsteadOfCreatingAnOrphan() {
        when(comments.selectById(20)).thenReturn(comment(20, 10, null));

        assertEquals(1, service.add(1, "reply", 20, null).getCode());
        verify(comments, never()).insert(any());
    }

    @Test
    void rejectsNonRootAndCrossArticleReplyPagination() {
        when(comments.selectById(20)).thenReturn(comment(20, 10, 10));
        when(comments.selectById(10)).thenReturn(comment(10, null, null));

        assertEquals(1, service.replies(1, 20, 1, 10).getCode());
        assertEquals(1, service.replies(2, 10, 1, 10).getCode());
        verify(comments, never()).selectRepliesByRoot(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void replyPageRetainsParentAndTargetEvenIfParentIsOutsidePage() {
        when(comments.selectById(10)).thenReturn(comment(10, null, null));
        Comment reply = comment(30, 20, 10);
        reply.setReplyToUserId(8);
        when(comments.selectRepliesByRoot(1, 10, 1, 1)).thenReturn(List.of(reply));
        when(comments.countRepliesByRoot(1, 10)).thenReturn(3);
        User author = new User();
        author.setUserId(7);
        author.setNickname("author");
        User target = new User();
        target.setUserId(8);
        target.setNickname("reply target");
        when(users.selectByIds(anyList())).thenReturn(List.of(author, target));
        when(likes.countByCommentIds(List.of(30))).thenReturn(List.of());
        when(likes.selectLikedCommentIds(9, List.of(30))).thenReturn(List.of());

        var result = service.replies(1, 10, 2, 1);
        assertEquals(0, result.getCode());
        assertEquals(3, result.getData().get("total"));
        assertEquals(true, result.getData().get("hasMore"));
        CommentVO vo = (CommentVO) ((List<?>) result.getData().get("list")).getFirst();
        assertEquals(30, vo.getCommentId());
        assertEquals(20, vo.getParentId());
        assertEquals(10, vo.getRootId());
        assertEquals("reply target", vo.getReplyToNickname());
        verify(comments, never()).selectById(20);
    }

    private Comment insertedComment() {
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(comments).insert(captor.capture());
        return captor.getValue();
    }

    private Comment comment(int id, Integer parentId, Integer rootId) {
        Comment comment = new Comment();
        comment.setCommentId(id);
        comment.setArticleId(1);
        comment.setUserId(7);
        comment.setParentId(parentId);
        comment.setRootCommentId(rootId);
        comment.setStatus(1);
        return comment;
    }
}
