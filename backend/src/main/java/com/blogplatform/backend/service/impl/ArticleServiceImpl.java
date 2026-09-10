package com.blogplatform.backend.service.impl;

import com.blogplatform.backend.mapper.ArticleLikeMapper;
import com.blogplatform.backend.mapper.ArticleMapper;
import com.blogplatform.backend.mapper.CategoryMapper;
import com.blogplatform.backend.mapper.TagMapper;
import com.blogplatform.backend.service.ArticleService;
import com.blogplatform.backend.entity.Article;
import com.blogplatform.backend.entity.ArticleVO;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.entity.Tag;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private TagMapper tagMapper;
    @Autowired
    private ArticleLikeMapper articleLikeMapper;
    @Autowired
    private ArticleViewCountService articleViewCountService;

    // ── Public discovery ──────────────────────────────────────────────────────

    @Override
    public Result search(String keyword, Integer categoryId, Integer tagId,
                         Integer authorId, String sort, Integer page, Integer pageSize) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        if (pageSize > 50) pageSize = 50;

        int offset = (page - 1) * pageSize;
        List<ArticleVO> list = articleMapper.searchPublished(keyword, categoryId, tagId, authorId, sort, offset, pageSize);
        int total = articleMapper.countPublished(keyword, categoryId, tagId, authorId);

        if (!list.isEmpty()) {
            enrichWithTags(list);
            enrichWithLikedStatus(list);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return Result.success(result);
    }

    @Override
    public Result getPublicDetail(Integer articleId) {
        ArticleVO vo = articleMapper.selectVOById(articleId);
        if (vo == null) return Result.error("文章不存在");
        if (!"published".equals(vo.getStatus())) {
            Integer currentUserId = resolveCurrentUserId();
            if (currentUserId == null || !currentUserId.equals(vo.getUserId())) {
                return Result.error("文章不存在或无权访问");
            }
        }
        vo.setTags(tagMapper.selectByArticleId(articleId));
        vo.setIsLiked(isArticleLiked(articleId));
        articleViewCountService.recordView(articleId);
        vo.setViewCount((vo.getViewCount() == null ? 0 : vo.getViewCount()) + 1);
        return Result.success(vo);
    }

    // ── My blog ───────────────────────────────────────────────────────────────

    @Override
    public Result getMyArticles(String title, String status, Integer categoryId) {
        Integer userId = requireCurrentUserId();
        if (userId == null) return Result.error("请先登录");
        List<ArticleVO> list = articleMapper.selectMyByCondition(userId, title, status, categoryId);
        if (!list.isEmpty()) enrichWithTags(list);
        return Result.success(list);
    }

    @Override
    @Transactional
    public Result add(Article article, List<String> tagNames) {
        Integer userId = requireCurrentUserId();
        if (userId == null) return Result.error("请先登录");
        article.setUserId(userId);
        if (article.getTitle() == null || article.getTitle().isBlank()) return Result.error("标题不能为空");
        if (article.getContent() == null || article.getContent().isBlank()) return Result.error("内容不能为空");
        ensureSummary(article);
        Result categoryValidation = validateCategoryOwnership(article.getCategoryId(), userId);
        if (categoryValidation != null) return categoryValidation;
        Result tagValidation = validateManagedTags(tagNames);
        if (tagValidation != null) return tagValidation;

        if ("published".equals(article.getStatus())) {
            article.setStatus("pending");
        }
        articleMapper.insert(article);

        if (tagNames != null && !tagNames.isEmpty()) {
            saveArticleTags(article.getArticleId(), tagNames);
        }
        tagMapper.refreshAllCounts();
        return Result.success(article.getArticleId());
    }

    @Override
    @Transactional
    public Result update(Article article, List<String> tagNames) {
        Integer userId = requireCurrentUserId();
        if (userId == null) return Result.error("请先登录");
        Article existing = articleMapper.selectById(article.getArticleId());
        if (existing == null) return Result.error("文章不存在");
        if (!existing.getUserId().equals(userId)) return Result.error("权限不足");
        if (article.getTitle() == null || article.getTitle().isBlank()) return Result.error("标题不能为空");
        if (article.getContent() == null || article.getContent().isBlank()) return Result.error("内容不能为空");
        ensureSummary(article);
        Result categoryValidation = validateCategoryOwnership(article.getCategoryId(), userId);
        if (categoryValidation != null) return categoryValidation;
        Result tagValidation = validateManagedTags(tagNames);
        if (tagValidation != null) return tagValidation;

        if ("published".equals(article.getStatus())) {
            article.setStatus("pending");
        }
        articleMapper.update(article);

        tagMapper.deleteArticleTags(article.getArticleId());
        if (tagNames != null && !tagNames.isEmpty()) {
            saveArticleTags(article.getArticleId(), tagNames);
        }
        tagMapper.refreshAllCounts();
        return Result.success();
    }

    @Override
    public Result delete(Integer articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) return Result.error("文章不存在");
        Integer userId = requireCurrentUserId();
        if (userId == null) return Result.error("请先登录");
        if (!article.getUserId().equals(userId)) return Result.error("权限不足");
        articleMapper.deleteById(articleId);
        tagMapper.refreshAllCounts();
        return Result.success();
    }

    @Override
    public Result submitForReview(Integer articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) return Result.error("文章不存在");
        Integer userId = requireCurrentUserId();
        if (userId == null) return Result.error("请先登录");
        if (!article.getUserId().equals(userId)) return Result.error("权限不足");
        articleMapper.submitForReview(articleId);
        return Result.success();
    }

    @Override
    public Result updateCoverImage(Integer articleId, String coverImage) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) return Result.error("文章不存在");
        Integer userId = requireCurrentUserId();
        if (userId == null) return Result.error("请先登录");
        if (!article.getUserId().equals(userId)) return Result.error("权限不足");
        articleMapper.updateCoverImage(articleId, coverImage);
        return Result.success();
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Override
    public Result adminSearch(String status, String keyword, Integer page, Integer pageSize) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        int offset = (page - 1) * pageSize;
        List<ArticleVO> list = articleMapper.adminSearch(status, keyword, offset, pageSize);
        int total = articleMapper.adminCount(status, keyword);
        if (!list.isEmpty()) enrichWithTags(list);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return Result.success(result);
    }

    @Override
    public Result adminApprove(Integer articleId) {
        articleMapper.approveArticle(articleId);
        return Result.success();
    }

    @Override
    public Result adminReject(Integer articleId) {
        articleMapper.rejectArticle(articleId);
        return Result.success();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Result validateManagedTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return null;
        Set<String> normalizedNames = new LinkedHashSet<>();
        for (String value : tagNames) {
            if (value == null || value.isBlank()) return Result.error("标签不能为空");
            String name = value.trim();
            if (name.length() > 30) return Result.error("标签名称不能超过 30 个字符");
            normalizedNames.add(name);
        }
        if (normalizedNames.size() > 5) return Result.error("一篇文章最多选择 5 个标签");
        for (String name : normalizedNames) {
            Tag tag = tagMapper.selectByName(name);
            if (tag == null) {
                return Result.error("标签「" + name + "」不可用，请从管理员维护的标签中选择");
            }
            if (tag.getParentId() == null) return Result.error("一级标签仅用于分组，请选择具体的二级标签");
        }
        return null;
    }

    private void ensureSummary(Article article) {
        if (article.getSummary() != null && !article.getSummary().isBlank()) {
            article.setSummary(article.getSummary().trim());
            return;
        }
        String plainText = article.getContent()
                .replaceAll("(?s)```.*?```", " ")
                .replaceAll("`([^`]*)`", "$1")
                .replaceAll("!\\[[^]]*]\\([^)]*\\)", " ")
                .replaceAll("\\[([^]]+)]\\([^)]*\\)", "$1")
                .replaceAll("(?m)^\\s{0,3}[#>*+\\-]+\\s*", "")
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        article.setSummary(plainText.length() > 180 ? plainText.substring(0, 180) + "…" : plainText);
    }

    private Result validateCategoryOwnership(Integer categoryId, Integer userId) {
        if (categoryId == null) return null;
        Integer ownerId = categoryMapper.selectUserIdByCategoryId(categoryId);
        if (ownerId == null) return Result.error("所选文章分类不存在");
        if (!ownerId.equals(userId)) return Result.error("不能使用其他用户的文章分类");
        return null;
    }

    private void saveArticleTags(Integer articleId, List<String> tagNames) {
        Set<String> normalizedNames = new LinkedHashSet<>();
        for (String value : tagNames) normalizedNames.add(value.trim());
        for (String name : normalizedNames) {
            Tag tag = tagMapper.selectByName(name);
            if (tag == null) throw new IllegalStateException("标签已被管理员移除：" + name);
            tagMapper.addArticleTag(articleId, tag.getTagId());
        }
    }

    private void enrichWithTags(List<ArticleVO> list) {
        List<Integer> ids = list.stream().map(ArticleVO::getArticleId).toList();
        List<Tag> allTags = tagMapper.selectByArticleIds(ids);

        // We need article_id association — query individually for simplicity
        Map<Integer, List<Tag>> tagMap = new HashMap<>();
        for (ArticleVO vo : list) {
            tagMap.put(vo.getArticleId(), tagMapper.selectByArticleId(vo.getArticleId()));
        }
        list.forEach(vo -> vo.setTags(tagMap.getOrDefault(vo.getArticleId(), List.of())));
    }

    private void enrichWithLikedStatus(List<ArticleVO> list) {
        Integer userId = resolveCurrentUserId();
        if (userId == null) {
            list.forEach(vo -> vo.setIsLiked(false));
            return;
        }
        List<Integer> ids = list.stream().map(ArticleVO::getArticleId).toList();
        Set<Integer> likedSet = new HashSet<>(articleLikeMapper.selectLikedArticleIds(userId, ids));
        list.forEach(vo -> vo.setIsLiked(likedSet.contains(vo.getArticleId())));
    }

    private boolean isArticleLiked(Integer articleId) {
        Integer userId = resolveCurrentUserId();
        if (userId == null) return false;
        return articleLikeMapper.exists(articleId, userId) > 0;
    }

    private Integer resolveCurrentUserId() {
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            if (claims != null && claims.get("id") != null) {
                return (Integer) claims.get("id");
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Integer requireCurrentUserId() {
        return resolveCurrentUserId();
    }
}
