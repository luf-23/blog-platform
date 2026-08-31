package com.blogplatform.backend.controller;

import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.service.CommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/community")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    @GetMapping("/feed")
    public Result feed(@RequestParam(defaultValue = "latest") String sort,
                       @RequestParam(required = false) Integer tagId,
                       @RequestParam(required = false) String tagIds,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "12") Integer pageSize) {
        return communityService.feed(sort, parseTagIds(tagId, tagIds), keyword, page, pageSize);
    }

    @GetMapping("/feed/following")
    public Result followingFeed(@RequestParam(required = false) Integer tagId,
                                @RequestParam(required = false) String tagIds,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "12") Integer pageSize) {
        return communityService.followingFeed(parseTagIds(tagId, tagIds), keyword, page, pageSize);
    }

    @GetMapping("/meta")
    public Result meta() {
        return communityService.meta();
    }

    @GetMapping("/meta/personalized")
    public Result personalizedMeta() {
        return communityService.meta();
    }

    @GetMapping("/profile/{userId}")
    public Result profileMetrics(@PathVariable Integer userId) {
        return communityService.profileMetrics(userId);
    }

    @GetMapping("/profile/{userId}/followers")
    public Result profileFollowers(@PathVariable Integer userId,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        return communityService.profileFollowers(userId, page, pageSize);
    }

    @GetMapping("/profile/{userId}/following")
    public Result profileFollowing(@PathVariable Integer userId,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        return communityService.profileFollowing(userId, page, pageSize);
    }

    @GetMapping("/follow/{userId}")
    public Result followState(@PathVariable Integer userId) {
        return communityService.followState(userId);
    }

    @PostMapping("/follow/{userId}")
    public Result toggleFollow(@PathVariable Integer userId) {
        return communityService.toggleFollow(userId);
    }

    private List<Integer> parseTagIds(Integer tagId, String value) {
        List<Integer> ids = new ArrayList<>();
        if (tagId != null) ids.add(tagId);
        if (value == null || value.isBlank()) return ids;
        for (String part : value.split(",")) {
            try {
                Integer id = Integer.valueOf(part.trim());
                if (id > 0 && !ids.contains(id)) ids.add(id);
            } catch (NumberFormatException ignored) {}
        }
        return ids;
    }
}
