import request from "../utils/request";

export const likeArticleService = (articleId) =>
  request({ url: "/articleLike/like", method: "post", params: { articleId } });

export const unlikeArticleService = (articleId) =>
  request({ url: "/articleLike/unlike", method: "post", params: { articleId } });

export const checkArticleLikeService = (articleId) =>
  request({ url: "/articleLike/check", method: "get", params: { articleId } });
