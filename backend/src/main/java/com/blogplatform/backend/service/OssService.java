package com.blogplatform.backend.service;

import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.blogplatform.backend.entity.Result;

public interface OssService {
    Result<AssumeRoleResponse.Credentials> generateCredentials(Integer userId);

    Result<String> getBucket();

    Result<String> getRegion();

    Result<String> getEndPoint();
}
