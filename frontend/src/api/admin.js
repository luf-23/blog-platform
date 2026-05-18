import request from "../utils/request";

export function getPendingArticleListService() {
  return request({
    url: "/admin/pendingList",
    method: "get",
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}

export function getPublishedArticleListService() {
  return request({
    url: "/admin/publishedList",
    method: "get",
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}

export function acceptArticleService(params) {
  return request({
    url: "/admin/accept",
    method: "post",
    params,
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    }
  });
}

export function rejectArticleService(params) {
  return request({
    url: "/admin/reject",
    method: "post",
    params,
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    }
  });
}

export function dropArticleService(params) {
  return request({
    url: "/admin/drop",
    method: "post",
    params,
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    }
  });
}

export function getArticleDetailService(params) {
  return request({
    url: "/admin/articleDetail",
    method: "get",
    params,
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}

export function getUsersService() {
  return request({
    url: "/admin/users",
    method: "get"
  });
}

export function getAnnouncementListService() {
  return request({
    url: "/admin/announcement",
    method: "get"
  });
}

export function deleteAnnouncementService(params) {
  return request({
    url: "/admin/deleteAnnouncement",
    method: "post",
    params,
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    }
  });
}
export function addAnnouncementService(data) {
  return request({
    url: "/admin/addAnnouncement",
    method: "post",
    data,
    headers: {
      ContentType: "application/json"
    }
  });
}
