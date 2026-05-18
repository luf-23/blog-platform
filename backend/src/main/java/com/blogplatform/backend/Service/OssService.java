package com.blogplatform.backend.Service;

import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.blogplatform.backend.entity.Result;

import java.util.Map;

public interface OssService {
    Result<AssumeRoleResponse.Credentials> generateCredentials(Integer userId);

    Result<String> getBucket();

    Result<String> getRegion();

    Result<String> getEndPoint();
}
