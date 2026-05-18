import request from "../utils/request.js";
export function loginService(data) {
  return request({
    url: "/user/login",
    method: "post",
    data,
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    }
  });
}

export function registerService(data) {
  return request({
    url: "/user/register",
    method: "post",
    data,
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    }
  });
}

export function getUserInfoService() {
  return request({
    url: "/user/getInfo",
    method: "get",
    headers: {
      //Authorization: token
    }
  });
}

export function updateUserInfoService(data) {
  console.log(data);
  return request({
    url: "/user/update",
    method: "post",
    data,
    headers: {
      "Content-Type": "application/json"
    }
  });
}

export function getUserInfoByNameService(params) {
  return request({
    url: "/user/getInfoByName",
    method: "get",
    params,
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    }
  });
}

export function getUserInfoByIdService(params) {
  return request({
    url: "/user/getInfoById",
    method: "get",
    params,
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    }
  });
}

export function sendEmailCaptchaService(params) {
  return request({
    url: "/user/captcha",
    method: "post",
    params,
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    }
  });
}

export function checkEmailCaptchaService(params) {
  return request({
    url: "/user/verify",
    method: "post",
    params,
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    }
  });
}

// 重置密码
export function resetPasswordService(params) {
  return request({
    url: "/user/reset-password",
    method: "post",
    params,
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    }
  });
}
