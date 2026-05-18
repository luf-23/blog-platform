import request from "../utils/request";

// 获取文章评论列表
export const getCommentListService = (params) => {
  return request({
    url: "/comment/list",
    method: "get",
    params
  });
};

// 发布评论
export const publishCommentService = (data) => {
  return request({
    url: "/comment/publish",
    method: "post",
    data
  });
};

// 删除评论
export const deleteCommentService = (params) => {
  return request({
    url: "/comment/delete",
    method: "post",
    params
  });
};
