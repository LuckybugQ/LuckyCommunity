package com.uestc.community.controller;

import com.uestc.community.annotation.LoginRequired;
import com.uestc.community.entity.Event;
import com.uestc.community.event.EventProducer;
import com.uestc.community.util.AliyunOssUtil;
import com.uestc.community.util.CommunityConstant;
import com.uestc.community.util.CommunityUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Controller
public class FileController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private EventProducer eventProducer;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${community.path.share}")
    private String shareBucketUrl;

    @Value("${community.path.upload}")
    private String uploadPath;

    @Autowired
    private AliyunOssUtil aliyunOssUtil;

    @LoginRequired
    @RequestMapping(path="/upload",method = RequestMethod.POST)
    @ResponseBody
    public String uploadHeader(HttpServletRequest request){
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        // 获取上传的文件
        MultipartFile multiFile = multipartRequest.getFile("file");

        String fileName = multiFile.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)){
            return CommunityUtil.getJSONString(1,"文件格式不正确！");
        }

        fileName = CommunityUtil.generateUUID() + suffix;
        Map<String,Object> map = new HashMap<>();
        try {
            map.put("status",0);
            String url = uploadPath + "/" + fileName;
            map.put("url",url);
            //headerImage.transferTo(dest);
            aliyunOssUtil.upload(multiFile.getInputStream(),fileName);
        } catch (Exception e) {
            logger.error("上传文件失败"+e.getMessage());
            throw new RuntimeException("上传文件失败，服务器异常",e);
        }

        return CommunityUtil.getJSONString(0,"上传成功！",map);
    }

    @RequestMapping(path = "/share", method = RequestMethod.GET)
    @ResponseBody
    public String share(String htmlUrl) {
        // 文件名
        String fileName = CommunityUtil.generateUUID();

        // 异步生成长图
        Event event = new Event()
                .setTopic(TOPIC_SHARE)
                .setData("htmlUrl", htmlUrl)
                .setData("fileName", fileName)
                .setData("suffix", ".png");
        eventProducer.fireEvent(event);

        // 返回访问路径
        Map<String, Object> map = new HashMap<>();
        map.put("shareUrl", shareBucketUrl + "/" + fileName + ".png");

        return CommunityUtil.getJSONString(0, null, map);
    }

    // 废弃
    // 获取长图
    @RequestMapping(path = "/share/image/{fileName}", method = RequestMethod.GET)
    public void getShareImage(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("文件名不能为空!");
        }

        response.setContentType("image/png");
        File file = new File(wkImageStorage + "/" + fileName + ".png");
        try {
            OutputStream os = response.getOutputStream();
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("获取长图失败: " + e.getMessage());
        }
    }


    @LoginRequired
    @RequestMapping(path="/write",method = RequestMethod.GET)
    public String getWritePage(){
        return "/site/write";
    }



}
