package com.uestc.community.util;

import com.uestc.community.dao.elasticsearch.DiscussPostRepository;
import com.uestc.community.entity.DiscussPost;
import com.uestc.community.service.DiscussPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchUtil {

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private DiscussPostService discussPostService;

    public void InsertList() {
        discussRepository.deleteAll();
        List<DiscussPost> list = discussPostService.findDiscussPostsByType(0,100,0,-1,0);
        discussRepository.saveAll(list);
    }
}
