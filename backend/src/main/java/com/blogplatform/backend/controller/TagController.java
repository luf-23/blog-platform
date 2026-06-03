package com.blogplatform.backend.controller;

import com.blogplatform.backend.service.TagService;
import com.blogplatform.backend.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tag")
public class TagController {

    @Autowired
    private TagService tagService;

    @GetMapping("/all")
    public Result getAllTags() {
        return tagService.getAllTags();
    }

    @GetMapping("/popular")
    public Result getPopularTags(@RequestParam(defaultValue = "20") int limit) {
        return tagService.getPopularTags(limit);
    }
}
