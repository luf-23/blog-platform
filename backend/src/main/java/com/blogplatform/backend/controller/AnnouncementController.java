package com.blogplatform.backend.controller;

import com.blogplatform.backend.entity.Announcement;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.mapper.AnnouncementMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/announcement")
public class AnnouncementController {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @GetMapping
    public Result<List<Announcement>> list(@RequestParam(required = false) Integer afterId) {
        if (afterId == null || afterId < 1) {
            return Result.success(announcementMapper.selectAll());
        }
        return Result.success(announcementMapper.selectAfterId(afterId));
    }
}
