package com.blogplatform.backend.Service.Impl;

import com.blogplatform.backend.Mapper.CommentLikeMapper;
import com.blogplatform.backend.Mapper.CommentMapper;
import com.blogplatform.backend.Mapper.UserMapper;
import com.blogplatform.backend.Service.CommentService;
import com.blogplatform.backend.entity.Comment;
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

    @Override
    public Result<Map<String, Object>> list(Integer articleId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<Comment> roots = commentMapper.selectRootList(articleId, offset, pageSize);
        int totalRoots = commentMapper.countRoots(articleId);
        int totalAll = commentMapper.countAll(articleId);

        if (roots.isEmpty()) {
            return Result.success(Map.of(
                    "total", totalRoots,
                    "totalAll", totalAll,
                    "list", List.of()
            ));
        }

        List<Integer> rootIds = roots.stream().map(Comment::getCommentId).toList();
        List<Comment> replies = commentMapper.selectRepliesByRootIds(articleId, rootIds);

        List<Comment> all = new ArrayList<>(roots.size() + replies.size());
        all.addAll(roots);
        all.addAll(replies);

        List<CommentVO> voList = enrichAndBuildTree(all);
        Map<String, Object> result = new HashMap<>();
        result.put("total", totalRoots);
        result.put("totalAll", totalAll);
        result.put("list", voList);
        return Result.success(result);
    }

    @Override
    public Result add(Integer articleId, String content, Integer parentId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(content);

        if (parentId == null) {
            comment.setParentId(null);
            comment.setRootId(null);
            commentMapper.insert(comment);
            commentMapper.setRootIdToSelf(comment.getCommentId());
        } else {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null) {
                return Result.error("父评论不存在");
            }
            if (!articleId.equals(parent.getArticleId())) {
                return Result.error("父评论不属于该文章");
            }
            comment.setParentId(parentId);
            Integer rootId = parent.getRootId() != null ? parent.getRootId() : parent.getCommentId();
            comment.setRootId(rootId);
            commentMapper.insert(comment);
        }
        return Result.success();
    }

    @Override
    public Result delete(Integer commentId) {
        commentMapper.delete(commentId);
        return Result.success();
    }

    private List<CommentVO> enrichAndBuildTree(List<Comment> comments) {
        List<Integer> commentIds = comments.stream().map(Comment::getCommentId).toList();
        Set<Integer> userIds = comments.stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, User> userMap = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectByIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        Integer currentUserId = resolveCurrentUserId();

        Map<Integer, Integer> likeCountMap = buildLikeCountMap(commentIds);
        Set<Integer> likedSet = buildLikedSet(commentIds, currentUserId);

        Map<Integer, CommentVO> voMap = new LinkedHashMap<>();
        for (Comment c : comments) {
            voMap.put(c.getCommentId(), toVO(c, userMap, likeCountMap, likedSet));
        }
        fillReplyToUser(voMap);

        Map<Integer, List<CommentVO>> byParent = new HashMap<>();
        for (CommentVO vo : voMap.values()) {
            int key = vo.getParentId() == null ? 0 : vo.getParentId();
            byParent.computeIfAbsent(key, k -> new ArrayList<>()).add(vo);
        }

        List<CommentVO> roots = byParent.getOrDefault(0, List.of());
        for (CommentVO root : roots) {
            attachChildren(root, byParent);
        }
        roots.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        return roots;
    }

    private void fillReplyToUser(Map<Integer, CommentVO> voMap) {
        for (CommentVO vo : voMap.values()) {
            if (vo.getParentId() == null) {
                continue;
            }
            CommentVO parent = voMap.get(vo.getParentId());
            if (parent == null) {
                continue;
            }
            vo.setReplyToUsername(parent.getUsername());
            vo.setReplyToNickname(parent.getNickname());
        }
    }

    private void attachChildren(CommentVO node, Map<Integer, List<CommentVO>> byParent) {
        List<CommentVO> children = byParent.getOrDefault(node.getCommentId(), List.of());
        node.setChildren(new ArrayList<>(children));
        for (CommentVO child : children) {
            attachChildren(child, byParent);
        }
    }

    private CommentVO toVO(Comment c, Map<Integer, User> userMap,
                         Map<Integer, Integer> likeCountMap, Set<Integer> likedSet) {
        CommentVO vo = new CommentVO();
        vo.setCommentId(c.getCommentId());
        vo.setArticleId(c.getArticleId());
        vo.setUserId(c.getUserId());
        vo.setParentId(c.getParentId());
        vo.setRootId(c.getRootId());
        vo.setContent(c.getContent());
        vo.setCreateTime(c.getCreateTime());

        User user = c.getUserId() != null ? userMap.get(c.getUserId()) : null;
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatarImage());
        } else {
            vo.setUsername("已注销用户");
        }
        vo.setLikeCount(likeCountMap.getOrDefault(c.getCommentId(), 0));
        vo.setIsLiked(likedSet.contains(c.getCommentId()));
        return vo;
    }

    private Map<Integer, Integer> buildLikeCountMap(List<Integer> commentIds) {
        if (commentIds.isEmpty()) {
            return Map.of();
        }
        List<com.blogplatform.backend.entity.CommentLikeCount> rows =
                commentLikeMapper.countByCommentIds(commentIds);
        Map<Integer, Integer> map = new HashMap<>();
        for (var row : rows) {
            if (row.getCommentId() != null && row.getLikeCount() != null) {
                map.put(row.getCommentId(), row.getLikeCount());
            }
        }
        return map;
    }

    private Integer resolveCurrentUserId() {
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            if (claims != null && claims.get("id") != null) {
                return (Integer) claims.get("id");
            }
        } catch (Exception ignored) {
            // 未登录浏览评论列表
        }
        return null;
    }

    private Set<Integer> buildLikedSet(List<Integer> commentIds, Integer userId) {
        if (userId == null || commentIds.isEmpty()) {
            return Set.of();
        }
        List<Integer> liked = commentLikeMapper.selectLikedCommentIds(userId, commentIds);
        return new HashSet<>(liked);
    }
}
