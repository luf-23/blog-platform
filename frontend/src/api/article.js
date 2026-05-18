import { ca } from "element-plus/es/locales.mjs";
import request from "../utils/request";
export function getPublishedArticleListService(params) {
  return request({
    url: "/article/publishedList",
    method: "get",
    params,
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}

export function getDraftArticleListService(params) {
  return request({
    url: "/article/draftList",
    method: "get",
    params,
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}

export function getArticleListService(params) {
  return request({
    url: "/article/list",
    method: "get",
    params,
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}

export function getSelectedArticleListService(params) {
  return request({
    url: "/article/selectedList",
    method: "get",
    params,
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}

export function deleteArticleService(params) {
  return request({
    url: "/article/delete",
    method: "post",
    params,
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}

export function addArticleService(data) {
  return request({
    url: "/article/add",
    method: "post",
    data,
    headers: {
      ContentType: "application/json"
    }
  });
}

export function getArticleDetailService(params) {
  return request({
    url: "/article/detail",
    method: "get",
    params,
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}

export function publishArticleService(params) {
  return request({
    url: "/article/publish",
    method: "post",
    params,
    headers: {
      ContentType: "application/x-www-form-urlencoded"
    }
  });
}

export function updateArticleService(data) {
  return request({
    url: "/article/update",
    method: "post",
    data,
    headers: {
      ContentType: "application/json"
    }
  });
}

export function checkService(params) {
  return request({
    url: "/article/check",
    method: "post",
    params,
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}

export function updateCoverImageService(params) {
  return request({
    url: "/article/updateCoverImage",
    method: "post",
    params,
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}
