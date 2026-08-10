package com.blogplatform.backend.controller;

import com.blogplatform.backend.entity.CommunityPost;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.service.CommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/community")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    @GetMapping("/feed")
    public Result feed(@RequestParam(defaultValue = "latest") String sort,
                       @RequestParam(required = false) String topic,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return communityService.feed(sort, topic, keyword, page, pageSize);
    }

    @GetMapping("/feed/following")
    public Result followingFeed(@RequestParam(required = false) String topic,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return communityService.followingFeed(topic, keyword, page, pageSize);
    }

    @GetMapping("/meta")
    public Result meta() {
        return communityService.meta();
    }

    @PostMapping("/posts")
    public Result create(@RequestBody Map<String, Object> body) {
        CommunityPost post = new CommunityPost();
        post.setType((String) body.get("type"));
        post.setTitle((String) body.get("title"));
        post.setContent((String) body.get("content"));
        post.setTopic((String) body.get("topic"));
        @SuppressWarnings("unchecked")
        List<String> options = (List<String>) body.get("options");
        return communityService.create(post, options);
    }

    @PostMapping("/posts/{postId}/like")
    public Result toggleLike(@PathVariable Integer postId) {
        return communityService.toggleLike(postId);
    }

    @PostMapping("/posts/{postId}/vote")
    public Result vote(@PathVariable Integer postId, @RequestParam Integer optionId) {
        return communityService.vote(postId, optionId);
    }

    @PostMapping("/follow/{userId}")
    public Result toggleFollow(@PathVariable Integer userId) {
        return communityService.toggleFollow(userId);
    }
}
