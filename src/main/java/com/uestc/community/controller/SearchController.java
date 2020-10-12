package com.uestc.community.controller;

import com.uestc.community.entity.DiscussPost;
import com.uestc.community.entity.Page;
import com.uestc.community.service.*;
import com.uestc.community.util.CommunityConstant;
import com.uestc.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {

    @Autowired
    HostHolder hostHolder;

    @Autowired
    SignService signService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private DiscussPostService discussPostService;

    // search?keyword=xxx

    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model) {
        // 搜索帖子
        // 分页信息

        int rows = discussPostService.findDiscussPostRowsByKeyword("%"+keyword+"%");
        List<DiscussPost> searchResult = discussPostService.findDiscussPostsByKeyword("%"+keyword+"%",page.getOffset(), page.getLimit());
        page.setRows(rows);
        // 聚合数据
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (searchResult != null) {
            for (DiscussPost post : searchResult) {

                String titile = post.getTitle();
                int index = titile.indexOf(keyword);
                if(index!=-1){
                    StringBuilder newTitle = new StringBuilder(titile);
                    newTitle.insert(index,"<em>");
                    newTitle.insert(index+keyword.length()+4,"</em>");
                    post.setTitle(newTitle.toString());
                }
                Map<String, Object> map = new HashMap<>();
                // 帖子
                map.put("post", post);
                // 作者
                map.put("user", userService.findUserById(post.getUserId()));
                // 点赞数量
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));

                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword",keyword);


        return "/site/search";
    }

    @RequestMapping(path = "/search/elastic", method = RequestMethod.GET)
    public String elasticSearch(String keyword, Page page, Model model, HttpSession session) {
        // 搜索帖子
        org.springframework.data.domain.Page<DiscussPost> searchResult =
                elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());
        // 聚合数据
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (searchResult != null) {
            for (DiscussPost post : searchResult) {
                Map<String, Object> map = new HashMap<>();
                // 帖子
                map.put("post", post);
                // 作者
                map.put("user", userService.findUserById(post.getUserId()));
                // 点赞数量
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));

                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword",keyword);

        // 分页信息
        page.setRows(searchResult == null ? 0 : (int) searchResult.getTotalElements());

        return "/site/search";
    }

}

