package com.uestc.community.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 阿里云存储
 * https://blog.52itstyle.vip
 */
@Component
public class AliyunOssUtil {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.header.bucketName}")
    private String headerBucketName;

    @Value("${aliyun.oss.share.bucketName}")
    private String shareBucketName;

    @Value("${aliyun.oss.upload.bucketName}")
    private String uploadBucketName;

    private OSS ossClient;

    private OSS getInstance() {
        if(ossClient==null){
            synchronized(AliyunOssUtil.class){
                if(ossClient==null){
                    ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
                }
            }
        }
        return ossClient;
    }

    /**
     * 上传
     */
    public void uploadHeader(InputStream file, String fileName){
        getInstance().putObject(headerBucketName,fileName,file);
    }

    public void uploadShare(InputStream file, String fileName){
        getInstance().putObject(shareBucketName,fileName,file);
    }

    public void upload(InputStream file, String fileName){
        getInstance().putObject(uploadBucketName,fileName,file);
    }
}