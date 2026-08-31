package com.blogplatform.backend.service.impl;

import com.blogplatform.backend.mapper.ArticleMapper;
import com.blogplatform.backend.mapper.CommentLikeMapper;
import com.blogplatform.backend.mapper.CommentMapper;
import com.blogplatform.backend.mapper.UserMapper;
import com.blogplatform.backend.service.CommentService;
import com.blogplatform.backend.entity.Comment;
import com.blogplatform.backend.entity.CommentLikeCount;
import com.blogplatform.backend.entity.CommentVO;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.entity.User;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CommentLikeMapper commentLikeMapper;
    @Autowired
    private ArticleMapper articleMapper;

    private static final int REPLY_PREVIEW_SIZE = 2;

    @Override
    public Result<Map<String, Object>> list(Integer articleId, Integer page, Integer pageSize) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        int offset = (page - 1) * pageSize;
        List<Comment> roots = commentMapper.selectRootList(articleId, offset, pageSize);
        int totalRoots = commentMapper.countRoots(articleId);
        int totalAll = commentMapper.countAll(articleId);

        if (roots.isEmpty()) {
            return Result.success(Map.of(
                    "total", totalRoots,
                    "totalAll", totalAll,
                    "hasMore", false,
                    "list", List.of()
            ));
        }

        List<Integer> rootIds = roots.stream().map(Comment::getCommentId).toList();
        List<Comment> replies = commentMapper.selectRepliesByRootIds(articleId, rootIds);

        List<Comment> all = new ArrayList<>(roots.size() + replies.size());
        all.addAll(roots);
        all.addAll(replies);
        Map<Integer, CommentVO> voMap = enrichToVoMap(all);

        Map<Integer, List<CommentVO>> repliesByRoot = new HashMap<>();
        for (Comment reply : replies) {
            CommentVO vo = voMap.get(reply.getCommentId());
            if (vo != null) {
                repliesByRoot.computeIfAbsent(reply.getRootId(), k -> new ArrayList<>()).add(vo);
            }
        }

        List<CommentVO> voList = new ArrayList<>(roots.size());
        for (Comment root : roots) {
            CommentVO rootVo = voMap.get(root.getCommentId());
            List<CommentVO> rootReplies = repliesByRoot.getOrDefault(root.getCommentId(), List.of());
            rootVo.setReplyCount(rootReplies.size());
            rootVo.setChildren(new ArrayList<>(
                    rootReplies.subList(0, Math.min(REPLY_PREVIEW_SIZE, rootReplies.size()))));
            voList.add(rootVo);
        }

        boolean hasMore = (page * pageSize) < totalRoots;

        Map<String, Object> result = new HashMap<>();
        result.put("total", totalRoots);
        result.put("totalAll", totalAll);
        result.put("hasMore", hasMore);
        result.put("list", voList);
        return Result.success(result);
    }

    @Override
    public Result<Map<String, Object>> replies(Integer articleId, Integer rootId, Integer page, Integer pageSize) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        int offset = (page - 1) * pageSize;
        List<Comment> replies = commentMapper.selectRepliesByRoot(articleId, rootId, offset, pageSize);
        int total = commentMapper.countRepliesByRoot(articleId, rootId);

        Map<Integer, CommentVO> voMap = enrichToVoMap(replies);
        List<CommentVO> list = new ArrayList<>(replies.size());
        for (Comment reply : replies) {
            CommentVO vo = voMap.get(reply.getCommentId());
            if (vo != null) list.add(vo);
        }

        boolean hasMore = (page * pageSize) < total;

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("hasMore", hasMore);
        result.put("list", list);
        return Result.success(result);
    }

    @Override
    public Result add(Integer articleId, String content, Integer parentId, Integer replyToUserId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setReplyToUserId(replyToUserId);

        if (parentId == null) {
            comment.setParentId(null);
            commentMapper.insert(comment);
        } else {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null) return Result.error("父评论不存在");
            if (!articleId.equals(parent.getArticleId())) return Result.error("评论不属于该文章");
            comment.setParentId(parentId);
            commentMapper.insert(comment);
        }
        articleMapper.incrementCommentCount(articleId);
        return Result.success();
    }

    @Override
    public Result delete(Integer commentId) {
        commentMapper.softDelete(commentId);
        return Result.success();
    }

    private Map<Integer, CommentVO> enrichToVoMap(List<Comment> comments) {
        if (comments.isEmpty()) return Map.of();

        Set<Integer> userIds = new HashSet<>();
        for (Comment c : comments) {
            if (c.getUserId() != null) userIds.add(c.getUserId());
            if (c.getReplyToUserId() != null) userIds.add(c.getReplyToUserId());
        }

        Map<Integer, User> userMap = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectByIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        List<Integer> commentIds = comments.stream().map(Comment::getCommentId).toList();
        Integer currentUserId = resolveCurrentUserId();

        Map<Integer, Integer> likeCountMap = buildLikeCountMap(commentIds);
        Set<Integer> likedSet = buildLikedSet(commentIds, currentUserId);

        Map<Integer, CommentVO> voMap = new LinkedHashMap<>();
        for (Comment c : comments) {
            voMap.put(c.getCommentId(), toVO(c, userMap, likeCountMap, likedSet));
        }
        return voMap;
    }

    private CommentVO toVO(Comment c, Map<Integer, User> userMap,
                            Map<Integer, Integer> likeCountMap, Set<Integer> likedSet) {
        CommentVO vo = new CommentVO();
        vo.setCommentId(c.getCommentId());
        vo.setArticleId(c.getArticleId());
        vo.setUserId(c.getUserId());
        vo.setParentId(c.getParentId());
        vo.setRootId(c.getRootId() != null ? c.getRootId() : c.getCommentId());
        vo.setReplyToUserId(c.getReplyToUserId());
        vo.setContent(c.getContent());
        vo.setCreateTime(c.getCreateTime());

        User user = c.getUserId() != null ? userMap.get(c.getUserId()) : null;
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatarImage());
        } else {
            vo.setNickname("已注销用户");
            vo.setUsername("deleted");
        }

        if (c.getReplyToUserId() != null) {
            User replyToUser = userMap.get(c.getReplyToUserId());
            if (replyToUser != null) {
                vo.setReplyToUsername(replyToUser.getUsername());
                vo.setReplyToNickname(replyToUser.getNickname());
            }
        }

        vo.setLikeCount(likeCountMap.getOrDefault(c.getCommentId(), 0));
        vo.setIsLiked(likedSet.contains(c.getCommentId()));
        vo.setReplyCount(0);
        return vo;
    }

    private Map<Integer, Integer> buildLikeCountMap(List<Integer> commentIds) {
        if (commentIds.isEmpty()) return Map.of();
        List<CommentLikeCount> rows = commentLikeMapper.countByCommentIds(commentIds);
        Map<Integer, Integer> map = new HashMap<>();
        for (CommentLikeCount row : rows) {
            if (row.getCommentId() != null && row.getLikeCount() != null) {
                map.put(row.getCommentId(), row.getLikeCount());
            }
        }
        return map;
    }

    private Integer resolveCurrentUserId() {
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            if (claims != null && claims.get("id") != null) return (Integer) claims.get("id");
        } catch (Exception ignored) {}
        return null;
    }

    private Set<Integer> buildLikedSet(List<Integer> commentIds, Integer userId) {
        if (userId == null || commentIds.isEmpty()) return Set.of();
        return new HashSet<>(commentLikeMapper.selectLikedCommentIds(userId, commentIds));
    }
}
