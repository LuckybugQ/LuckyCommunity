package com.uestc.community.util;

import com.uestc.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户信息，用于代替session对象
 */
@Component
public class HostHolder {

    private ThreadLocal<User> users = new ThreadLocal<>();

    public void setUser(User user){
        this.users.set(user);
    }
    public User getUser(){
        return this.users.get();
    }
    public void clear(){
        users.remove();
    }

}
