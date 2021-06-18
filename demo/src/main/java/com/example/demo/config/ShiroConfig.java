package com.example.demo.config;

import com.example.demo.realm.AccountRealm;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {
    @Bean
    public AccountRealm accountRealm(){
        return new AccountRealm();
    }

    @Bean
    public DefaultWebSecurityManager securityManager(@Qualifier("accountRealm") AccountRealm accountRealm){
        return new DefaultWebSecurityManager(accountRealm);
    }

    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(@Qualifier("securityManager") DefaultWebSecurityManager securityManager){
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        //权限设置
        Map<String,String> map=new HashMap<>();
        map.put("/main","authc");
        map.put("/manager","perms[manager]");
        map.put("/admin","roles[admin]");

        shiroFilterFactoryBean.setFilterChainDefinitionMap(map);
        //设置登录页面
        shiroFilterFactoryBean.setLoginUrl("/login");
        //设置未授权页面
        shiroFilterFactoryBean.setLoginUrl("/unauth");
        return shiroFilterFactoryBean;
    }
}
