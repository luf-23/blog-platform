import request from "../utils/request";

export const likeCommentService = (commentId) =>
  request({ url: "/commentLike/like", method: "post", params: { commentId } });

export const unlikeCommentService = (commentId) =>
  request({ url: "/commentLike/unlike", method: "post", params: { commentId } });
