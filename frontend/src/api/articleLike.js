import request from "../utils/request";

export const getArticleLikeCountService = (params) =>
  request({ url: "/articleLike/count", method: "get", params });

export const likeArticleService = (params) =>
  request({ url: "/articleLike/like", method: "post", params });

export const unlikeArticleService = (params) =>
  request({ url: "/articleLike/unlike", method: "post", params });

export const checkArticleLikeService = (params) =>
  request({ url: "/articleLike/check", method: "get", params });
