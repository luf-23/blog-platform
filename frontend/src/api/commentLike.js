import request from "../utils/request";

export const getCommentLikesService = (params) => {
  return request({
    url: "/commentLike/count",
    method: "GET",
    params
  });
};

export const likeCommentService = (params) => {
  return request({
    url: "/commentLike/like",
    method: "POST",
    params
  });
};

export const unlikeCommentService = (params) => {
  return request({
    url: "/commentLike/unlike",
    method: "POST",
    params
  });
};

export const checkUserLikeService = (params) => {
  return request({
    url: "/commentLike/check",
    method: "GET",
    params
  });
};
